package com.example.reloader.web;

import com.example.reloader.entity.SenderQueueEntry;
import com.example.reloader.repository.SenderQueueRepository;
import com.example.reloader.service.SenderService;
import com.example.reloader.web.dto.EnqueueRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/senders")
public class SenderController {
    private final SenderService senderService;
    private final SenderQueueRepository repo;
    private final com.example.reloader.service.MetadataImporterService metadataImporterService;

    public SenderController(SenderService senderService, SenderQueueRepository repo, com.example.reloader.service.MetadataImporterService metadataImporterService) {
        this.senderService = senderService;
        this.repo = repo;
        this.metadataImporterService = metadataImporterService;
    }

    @GetMapping("/{id}/queue")
    public List<SenderQueueEntry> getQueue(@PathVariable("id") Integer id,
                                           @RequestParam(defaultValue = "NEW") String status,
                                           @RequestParam(defaultValue = "100") int limit) {
        return senderService.getQueue(id, status, limit);
    }

    // Run now: triggers scheduled logic immediately for testing / on-demand
    @PostMapping("/{id}/run")
    public ResponseEntity<String> runNow(@PathVariable("id") Integer id,
                                         @RequestParam(defaultValue = "false") boolean preview,
                                         @RequestParam(defaultValue = "100") int limit) {
        if (preview) {
            return ResponseEntity.ok("Preview - no items processed");
        }
        senderService.processBatch(limit);
        return ResponseEntity.accepted().body("Run started");
    }

    // New endpoint: enqueue payloads from UI form (Reload / Submit)
    @PostMapping("/{id}/enqueue")
    public ResponseEntity<com.example.reloader.web.dto.EnqueueResult> enqueue(@PathVariable("id") Integer id, @RequestBody EnqueueRequest req) {
        Integer senderId = id;
        if (req.getSenderId() != null) senderId = req.getSenderId();
        SenderService.EnqueueResultHolder holder = senderService.enqueuePayloadsWithResult(senderId, req.getPayloadIds(), req.getSource() != null ? req.getSource() : "ui_submit");
        com.example.reloader.web.dto.EnqueueResult result = new com.example.reloader.web.dto.EnqueueResult(holder.enqueuedCount, holder.skippedPayloads);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/discover")
    public ResponseEntity<String> discover(@PathVariable("id") Integer id,
                                           @RequestParam(defaultValue = "default") String site,
                                           @RequestParam(defaultValue = "qa") String environment,
                                           @RequestParam(required = false) String startDate,
                                           @RequestParam(required = false) String endDate,
                                           @RequestParam(required = false) String testerType,
                                           @RequestParam(required = false) String dataType,
                                           @RequestParam(required = false) String testPhase,
                                           @RequestParam(required = false) String location,
                                           @RequestParam(defaultValue = "false") boolean writeListFile,
                                           @RequestParam(defaultValue = "300") int numberOfDataToSend,
                                           @RequestParam(defaultValue = "600") int countLimitTrigger) {
        int added = metadataImporterService.discoverAndEnqueue(site, environment, id, startDate, endDate, testerType, dataType, testPhase, location, writeListFile, numberOfDataToSend, countLimitTrigger);
        return ResponseEntity.ok("Discovered and enqueued " + added + " payloads");
    }
}

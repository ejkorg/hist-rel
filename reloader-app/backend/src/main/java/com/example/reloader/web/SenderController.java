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

    public SenderController(SenderService senderService, SenderQueueRepository repo) {
        this.senderService = senderService;
        this.repo = repo;
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
    public ResponseEntity<String> enqueue(@PathVariable("id") Integer id, @RequestBody EnqueueRequest req) {
        Integer senderId = id;
        if (req.getSenderId() != null) senderId = req.getSenderId();
        int added = senderService.enqueuePayloads(senderId, req.getPayloadIds(), req.getSource() != null ? req.getSource() : "ui_submit");
        return ResponseEntity.ok("Enqueued " + added + " payloads");
    }
}

package com.onsemi.com.reloader.service;

import com.onsemi.com.reloader.config.SenderProperties;
import com.onsemi.com.reloader.entity.SenderQueueEntry;
import com.onsemi.com.reloader.repository.SenderQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.List;

@Service
public class SenderService {
    private final Logger log = LoggerFactory.getLogger(SenderService.class);
    private final SenderQueueRepository repository;
    private final SenderProperties props;

    public SenderService(SenderQueueRepository repository, SenderProperties props) {
        this.repository = repository;
        this.props = props;
    }

    @Scheduled(cron = "${app.sender.cron:0 */5 * * * *}")
    public void scheduledRun() {
        runIfBelowThreshold();
    }

    @Transactional
    public void runIfBelowThreshold() {
        long pending = repository.countByStatus("NEW");
        log.info("Pending items in queue (NEW): {}", pending);
        if (pending > props.getCountLimitTrigger()) {
            log.info("Queue above threshold ({} > {}). Sender will not run.", pending, props.getCountLimitTrigger());
            return;
        }
        int limit = props.getNumberOfDataToSend();
        processBatch(limit);
    }

    @Transactional
    public void processBatch(int limit) {
        List<SenderQueueEntry> batch = repository.findByStatusOrderByCreatedAt("NEW", PageRequest.of(0, limit));
        if (batch == null || batch.isEmpty()) {
            log.debug("No NEW items to process.");
            return;
        }

        for (SenderQueueEntry e : batch) {
            try {
                // Mark PROCESSING to avoid double processing
                e.setStatus("PROCESSING");
                repository.save(e);

                // Current behavior: no external send -> mark SENT
                // If an external send is reintroduced, implement it here and mark SENT only on success.
                e.setStatus("SENT");
                e.setProcessedAt(Instant.now());
                repository.save(e);
                log.info("Processed payload {} as SENT", e.getPayloadId());
            } catch (Exception ex) {
                log.error("Failed processing payload {}: {}", e.getPayloadId(), ex.getMessage());
                e.setStatus("FAILED");
                repository.save(e);
            }
        }
    }

    @Transactional
    public int enqueuePayloads(Integer senderId, java.util.List<String> payloadIds, String source) {
        if (payloadIds == null || payloadIds.isEmpty()) return 0;
        int count = 0;
        for (String p : payloadIds) {
            if (p == null || p.trim().isEmpty()) continue;
            SenderQueueEntry entry = new SenderQueueEntry(senderId, p.trim(), source);
            entry.setStatus("NEW");
            repository.save(entry);
            count++;
        }
        log.info("Enqueued {} payloads (senderId={}, source={})", count, senderId, source);
        return count;
    }

    public java.util.List<SenderQueueEntry> getQueue(Integer senderId, String status, int limit) {
        return repository.findBySenderIdAndStatusOrderByCreatedAt(senderId, status, PageRequest.of(0, limit));
    }
}
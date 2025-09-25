package com.example.reloader.service;

import com.example.reloader.config.ExternalDbConfig;
import com.example.reloader.repository.ExternalMetadataRepository;
import com.example.reloader.repository.MetadataRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetadataImporterService {
    private final Logger log = LoggerFactory.getLogger(MetadataImporterService.class);
    private final ExternalDbConfig externalDbConfig;
    private final SenderService senderService;
    private final MailService mailService;
    private final com.example.reloader.config.DiscoveryProperties discoveryProps;
    private final ExternalMetadataRepository externalMetadataRepository;

    public MetadataImporterService(ExternalDbConfig externalDbConfig, SenderService senderService, MailService mailService, com.example.reloader.config.DiscoveryProperties discoveryProps, ExternalMetadataRepository externalMetadataRepository) {
        this.externalDbConfig = externalDbConfig;
        this.senderService = senderService;
        this.mailService = mailService;
        this.discoveryProps = discoveryProps;
        this.externalMetadataRepository = externalMetadataRepository;
    }

    /**
     * Discover metadata rows from external site and enqueue into local sender queue.
     * Returns number enqueued.
     */
    public int discoverAndEnqueue(String site, String environment, Integer senderId, String startDate, String endDate,
                                  String testerType, String dataType, String testPhase, String location, boolean writeListFile,
                                  int numberOfDataToSend, int countLimitTrigger) {
        // We'll stream results from repository, write list-file progressively (if requested), and enqueue in batches.
        final int batchSize = 200;
        final List<String> batch = new ArrayList<>(batchSize);
    Path listFilePath = null;
    final BufferedWriter[] bwRef = new BufferedWriter[1];
    final int[] discoveredCount = {0};
    final int[] addedCount = {0};
    final java.util.List<String> skippedOverall = new java.util.ArrayList<>();

        try {
            if (writeListFile) {
                listFilePath = Path.of(String.format("sender_list_%s.txt", senderId == null ? "0" : senderId.toString()));
                bwRef[0] = Files.newBufferedWriter(listFilePath, StandardCharsets.UTF_8);
            }

            // parse timestamps into LocalDateTime for repository
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
            LocalDateTime lstart = null;
            LocalDateTime lend = null;
            try { lstart = (startDate == null || startDate.isBlank()) ? LocalDateTime.parse("1970-01-01 00:00:00.000000", dtf) : LocalDateTime.parse(startDate, dtf); } catch (Exception e) { try { lstart = LocalDateTime.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); } catch (Exception ex) { lstart = LocalDateTime.of(1970,1,1,0,0); } }
            try { lend = (endDate == null || endDate.isBlank()) ? LocalDateTime.parse("2099-12-31 23:59:59.999999", dtf) : LocalDateTime.parse(endDate, dtf); } catch (Exception e) { try { lend = LocalDateTime.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); } catch (Exception ex) { lend = LocalDateTime.of(2099,12,31,23,59,59); } }

            // Pre-check external queue size via ExternalDbConfig like before
            try (Connection c = externalDbConfig.getConnection(site, environment)) {
                String countSql = "select count(id) as count from DTP_SENDER_QUEUE_ITEM where id_sender=?";
                try (PreparedStatement cps = c.prepareStatement(countSql)) {
                    cps.setString(1, senderId == null ? "" : senderId.toString());
                    try (ResultSet crs = cps.executeQuery()) {
                        if (crs.next()) {
                            int existing = crs.getInt(1);
                            log.info("External queue size for sender {} is {}", senderId, existing);
                            if (existing >= countLimitTrigger) {
                                log.info("Queue above threshold ({} >= {}), skipping discovery", existing, countLimitTrigger);
                                return 0;
                            }
                        }
                    }
                }
            }

            final int maxToEnqueue = numberOfDataToSend > 0 ? numberOfDataToSend : Integer.MAX_VALUE;

            externalMetadataRepository.streamMetadata(site, environment, lstart, lend, dataType, testPhase, testerType, location, maxToEnqueue, mr -> {
                discoveredCount[0]++;
                String payload = (mr.getId() == null ? "" : mr.getId()) + "," + (mr.getIdData() == null ? "" : mr.getIdData());
                // write to file if enabled
                if (bwRef[0] != null) {
                    try { bwRef[0].write(payload); bwRef[0].newLine(); } catch (Exception e) { log.warn("Failed writing to list file: {}", e.getMessage()); }
                }
                // buffer for batch enqueue
                batch.add(payload);
                if (batch.size() >= batchSize) {
                    SenderService.EnqueueResultHolder r = senderService.enqueuePayloadsWithResult(senderId, new ArrayList<>(batch), "metadata_discover");
                    addedCount[0] += (r == null ? 0 : r.enqueuedCount);
                    if (r != null && r.skippedPayloads != null && !r.skippedPayloads.isEmpty()) skippedOverall.addAll(r.skippedPayloads);
                    batch.clear();
                }
            });

            // enqueue remaining
            if (!batch.isEmpty()) {
                SenderService.EnqueueResultHolder r = senderService.enqueuePayloadsWithResult(senderId, new ArrayList<>(batch), "metadata_discover");
                addedCount[0] += (r == null ? 0 : r.enqueuedCount);
                if (r != null && r.skippedPayloads != null && !r.skippedPayloads.isEmpty()) skippedOverall.addAll(r.skippedPayloads);
                batch.clear();
            }

        } catch (Exception ex) {
            log.error("Failed to discover metadata from site {}: {}", site, ex.getMessage(), ex);
            return 0;
        } finally {
            if (bwRef[0] != null) try { bwRef[0].close(); } catch (Exception ignore) {}
        }

        if (discoveredCount[0] == 0) {
            log.info("No metadata rows discovered for given criteria");
            return 0;
        }

    log.info("Discovered {} rows and enqueued {} payloads for sender {}. Skipped {} already-present.", discoveredCount[0], addedCount[0], senderId, skippedOverall.size());

        // Notification: prefer discovery properties, then fallback to env var
        String recipient = discoveryProps.getNotifyRecipient();
        if ((recipient == null || recipient.isBlank())) recipient = System.getenv("RELOADER_NOTIFY_RECIPIENT");
        if (recipient != null && !recipient.isBlank()) {
            String subj = String.format("Reloader: discovery complete for sender %s", senderId);
            StringBuilder body = new StringBuilder();
            body.append(String.format("Discovered %d rows and enqueued %d payloads for sender %s", discoveredCount[0], addedCount[0], senderId));
            if (!skippedOverall.isEmpty()) {
                body.append(". Skipped ").append(skippedOverall.size()).append(" already-present items:\n");
                int c = 0;
                for (String s : skippedOverall) {
                    if (c++ >= 50) { body.append("... (truncated)\n"); break; }
                    body.append(s).append("\n");
                }
            }
            boolean attach = discoveryProps.isNotifyAttachList();
            if (attach && listFilePath != null) {
                mailService.sendWithAttachment(recipient, subj, body.toString(), listFilePath);
            } else {
                mailService.send(recipient, subj, body.toString());
            }
        }

        return addedCount[0];
    }
}

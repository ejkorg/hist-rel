package com.example.reloader.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExternalMetadataRepository {
    List<MetadataRow> findMetadata(String site, String environment, LocalDateTime start, LocalDateTime end,
                                   String dataType, String testPhase, String testerType, String location, int limit);

    /**
     * Stream rows; consumer should be fast. This will use JDBC ResultSet iteration.
     */
    void streamMetadata(String site, String environment, LocalDateTime start, LocalDateTime end,
                        String dataType, String testPhase, String testerType, String location, int limit,
                        java.util.function.Consumer<MetadataRow> consumer);
}

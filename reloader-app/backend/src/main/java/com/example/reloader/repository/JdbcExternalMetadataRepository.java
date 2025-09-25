package com.example.reloader.repository;

import com.example.reloader.config.ExternalDbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcExternalMetadataRepository implements ExternalMetadataRepository {
    private final Logger log = LoggerFactory.getLogger(JdbcExternalMetadataRepository.class);
    private final ExternalDbConfig externalDbConfig;

    public JdbcExternalMetadataRepository(ExternalDbConfig externalDbConfig) {
        this.externalDbConfig = externalDbConfig;
    }

    @Override
    public List<MetadataRow> findMetadata(String site, String environment, LocalDateTime start, LocalDateTime end, String dataType, String testPhase, String testerType, String location, int limit) {
        List<MetadataRow> rows = new ArrayList<>();
        streamMetadata(site, environment, start, end, dataType, testPhase, testerType, location, limit, rows::add);
        return rows;
    }

    @Override
    public void streamMetadata(String site, String environment, LocalDateTime start, LocalDateTime end, String dataType, String testPhase, String testerType, String location, int limit, java.util.function.Consumer<MetadataRow> consumer) {
        StringBuilder sb = new StringBuilder("select lot, id, id_data, end_time from all_metadata_view where end_time BETWEEN ? AND ?");
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.valueOf(start));
        params.add(Timestamp.valueOf(end));
        if (dataType != null && !dataType.isBlank()) { sb.append(" and data_type = ?"); params.add(dataType); }
        if (testPhase != null && !testPhase.isBlank()) { sb.append(" and test_phase = ?"); params.add(testPhase); }
        if (testerType != null && !testerType.isBlank()) { sb.append(" and tester_type = ?"); params.add(testerType); }
        if (location != null && !location.isBlank()) { sb.append(" and location = ?"); params.add(location); }
        if (limit > 0) sb.append(" fetch first ").append(limit).append(" rows only");

        String sql = sb.toString();
        try (Connection c = externalDbConfig.getConnection(site, environment);
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            for (Object p : params) {
                if (p instanceof Timestamp) ps.setTimestamp(idx++, (Timestamp) p);
                else ps.setString(idx++, p == null ? null : p.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String lot = rs.getString("lot");
                    String id = rs.getString("id");
                    String idData = rs.getString("id_data");
                    Timestamp ts = rs.getTimestamp("end_time");
                    LocalDateTime endTime = ts == null ? null : ts.toLocalDateTime();
                    consumer.accept(new MetadataRow(lot, id, idData, endTime));
                }
            }
        } catch (Exception ex) {
            log.error("Failed streaming metadata for site {} env {}: {}", site, environment, ex.getMessage(), ex);
            throw new RuntimeException("External metadata read failed", ex);
        }
    }
}

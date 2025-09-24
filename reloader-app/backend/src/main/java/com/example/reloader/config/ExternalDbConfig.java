package com.example.reloader.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

@Component
public class ExternalDbConfig {

    private final Map<String, Map<String, String>> dbConnections;

    public ExternalDbConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource r = new ClassPathResource("dbconnections.json");
        dbConnections = mapper.readValue(r.getInputStream(), new TypeReference<>() {});
    }

    public Map<String, String> getConfigForSite(String site) {
        return dbConnections.get(site);
    }

    /**
     * Returns a JDBC Connection for the configured site. This method attempts to be
     * DB-agnostic. For the common Oracle "host/sid" style host we build a thin URL.
     */
    public Connection getConnection(String site) throws SQLException {
        Map<String, String> cfg = getConfigForSite(site);
        if (cfg == null) throw new SQLException("No DB configuration for site " + site);

        String host = cfg.get("host");
        String user = cfg.get("user");
        String pw = cfg.get("password");
        String port = cfg.getOrDefault("port", "1521");

        // If host contains a slash (host/SERVICE) assume Oracle thin format
        String jdbcUrl;
        if (host != null && host.contains("/")) {
            String[] parts = host.split("/");
            String hostname = parts[0];
            String service = parts[1];
            // If service contains dots (full domain), keep as-is for connection string
            String sid = service.split("\\.")[0];
            jdbcUrl = String.format("jdbc:oracle:thin:@%s:%s:%s", hostname, port, sid);
        } else {
            // Fallback - try as JDBC URL or generic mysql-style host
            if (host != null && host.startsWith("jdbc:")) jdbcUrl = host;
            else jdbcUrl = String.format("jdbc:oracle:thin:@%s:%s", host, port);
        }

        return DriverManager.getConnection(jdbcUrl, user, pw);
    }
}

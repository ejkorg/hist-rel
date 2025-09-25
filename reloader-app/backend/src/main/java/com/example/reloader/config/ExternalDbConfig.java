package com.example.reloader.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

@Component
public class ExternalDbConfig {

    private final Map<String, Map<String, String>> dbConnections;

    public ExternalDbConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Prefer an external file path via env var RELOADER_DBCONN_PATH for secrets in deployments.
        String externalPath = System.getenv("RELOADER_DBCONN_PATH");
        if (externalPath != null && !externalPath.isBlank()) {
            try (InputStream is = Files.newInputStream(Paths.get(externalPath))) {
                dbConnections = mapper.readValue(is, new TypeReference<>() {});
                return;
            }
        }

        // Fallback to classpath resource (for local dev only). Avoid packaging real secrets into JAR.
        ClassPathResource r = new ClassPathResource("dbconnections.json");
        dbConnections = mapper.readValue(r.getInputStream(), new TypeReference<>() {});
    }

    public Map<String, String> getConfigForSite(String site) {
        return dbConnections.get(site);
    }

    /**
     * Try resolving db config with environment qualifiers. Order: site-environment, site_environment, site.environment, site
     */
    public Map<String, String> getConfigForSite(String site, String environment) {
        if (environment != null && !environment.isBlank()) {
            String[] candidates = new String[]{String.format("%s-%s", site, environment), String.format("%s_%s", site, environment), String.format("%s.%s", site, environment), site};
            for (String k : candidates) {
                Map<String, String> cfg = dbConnections.get(k);
                if (cfg != null) return cfg;
            }
        }
        return dbConnections.get(site);
    }

    /**
     * Returns a JDBC Connection for the configured site. This method attempts to be
     * DB-agnostic. For the common Oracle "host/sid" style host we build a thin URL.
     */
    public Connection getConnection(String site) throws SQLException {
        return getConnection(site, null);
    }

    public Connection getConnection(String site, String environment) throws SQLException {
        Map<String, String> cfg = getConfigForSite(site, environment);
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

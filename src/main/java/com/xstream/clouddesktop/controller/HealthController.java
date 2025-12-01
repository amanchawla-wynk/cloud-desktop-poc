package com.xstream.clouddesktop.controller;

import com.xstream.clouddesktop.client.guacamole.GuacamoleClient;
import com.xstream.clouddesktop.client.proxmox.ProxmoxClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import java.util.Optional;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    @Value("${spring.application.name:cloud-desktop-poc}")
    private String applicationName;

    private final Optional<BuildProperties> buildProperties;
    private final JdbcTemplate jdbcTemplate;
    private final ProxmoxClient proxmoxClient;
    private final GuacamoleClient guacamoleClient;


    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", OffsetDateTime.now());
        response.put("application", applicationName);
        response.put("version", buildProperties.map(BuildProperties::getVersion).orElse("N/A"));
        return response;
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> checks = new HashMap<>();
        boolean isReady = true;

        // 1. Database Check
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            checks.put("database", "UP");
        } catch (Exception e) {
            checks.put("database", "DOWN");
            isReady = false;
        }

        // 2. Proxmox Check
        try {
            proxmoxClient.checkProxmoxHealth();
            checks.put("proxmox", "UP");
        } catch (Exception e) {
            checks.put("proxmox", "DOWN");
            isReady = false;
        }

        // 3. Guacamole Check
        try {
            guacamoleClient.checkGuacamoleHealth();
            checks.put("guacamole", "UP");
        } catch (Exception e) {
            checks.put("guacamole", "DOWN");
            isReady = false;
        }


        response.put("ready", isReady);
        response.put("checks", checks);

        return isReady
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
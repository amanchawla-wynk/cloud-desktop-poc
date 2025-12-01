package com.xstream.clouddesktop.controller;

import com.xstream.clouddesktop.client.guacamole.GuacamoleClient;
import com.xstream.clouddesktop.client.proxmox.ProxmoxClient;
import com.xstream.clouddesktop.config.GuacamoleProperties;
import com.xstream.clouddesktop.config.ProxmoxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic controller to test external service connectivity
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnostics")
@RequiredArgsConstructor
public class DiagnosticsController {

    private final ProxmoxClient proxmoxClient;
    private final GuacamoleClient guacamoleClient;
    private final ProxmoxProperties proxmoxProperties;
    private final GuacamoleProperties guacamoleProperties;

    @GetMapping("/proxmox")
    public ResponseEntity<Map<String, Object>> testProxmoxConnection() {
        Map<String, Object> result = new HashMap<>();
        result.put("configured", true);
        result.put("url", proxmoxProperties.getUrl());
        result.put("node", proxmoxProperties.getNode());
        result.put("username", proxmoxProperties.getUsername());
        result.put("tokenIdConfigured",
                proxmoxProperties.getTokenId() != null && !proxmoxProperties.getTokenId().isEmpty());
        result.put("tokenSecretConfigured",
                proxmoxProperties.getTokenSecret() != null && !proxmoxProperties.getTokenSecret().isEmpty());

        try {
            log.info("Testing Proxmox connectivity to: {}", proxmoxProperties.getUrl());
            Integer nextVmId = proxmoxClient.getNextAvailableVmId();
            result.put("status", "SUCCESS");
            result.put("nextAvailableVmId", nextVmId);
            result.put("message", "Successfully connected to Proxmox");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Proxmox connection test failed", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());

            // Get root cause
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            result.put("rootCause", rootCause.getMessage());

            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/guacamole")
    public ResponseEntity<Map<String, Object>> testGuacamoleConnection() {
        Map<String, Object> result = new HashMap<>();
        result.put("configured", true);
        result.put("url", guacamoleProperties.getUrl());
        result.put("username", guacamoleProperties.getUsername());
        result.put("dataSource", guacamoleProperties.getDataSource());

        try {
            log.info("Testing Guacamole connectivity to: {}", guacamoleProperties.getUrl());
            guacamoleClient.authenticate();
            result.put("status", "SUCCESS");
            result.put("message", "Successfully authenticated with Guacamole");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Guacamole connection test failed", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());

            // Get root cause
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            result.put("rootCause", rootCause.getMessage());

            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> testAllConnections() {
        Map<String, Object> result = new HashMap<>();

        // Test Proxmox
        try {
            proxmoxClient.getNextAvailableVmId();
            result.put("proxmox", "OK");
        } catch (Exception e) {
            result.put("proxmox", "FAILED: " + e.getMessage());
        }

        // Test Guacamole
        try {
            guacamoleClient.authenticate();
            result.put("guacamole", "OK");
        } catch (Exception e) {
            result.put("guacamole", "FAILED: " + e.getMessage());
        }

        boolean allOk = result.values().stream().allMatch(v -> v.equals("OK"));
        result.put("overallStatus", allOk ? "ALL_OK" : "SOME_FAILED");

        return ResponseEntity.ok(result);
    }
}

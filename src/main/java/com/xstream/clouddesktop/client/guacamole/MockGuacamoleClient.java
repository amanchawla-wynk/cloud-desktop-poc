package com.xstream.clouddesktop.client.guacamole;

import com.xstream.clouddesktop.client.guacamole.dto.*;
import com.xstream.clouddesktop.client.guacamole.exception.*;
import com.xstream.clouddesktop.config.GuacamoleProperties;
import com.xstream.clouddesktop.config.MockProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock implementation of GuacamoleClient for POC demonstration without actual
 * infrastructure
 */
@Slf4j
@Component
@Profile("mock")
public class MockGuacamoleClient extends GuacamoleClient {

    private final Map<String, ConnectionResponse> connections = new ConcurrentHashMap<>();
    private final AtomicInteger connectionIdCounter = new AtomicInteger(1);
    private final String mockAuthToken = "mock-auth-token-" + UUID.randomUUID();
    private final MockProperties mockProperties;
    private final GuacamoleProperties guacamoleProperties;

    public MockGuacamoleClient(GuacamoleProperties properties, MockProperties mockProperties) {
        super(null, properties); // No RestTemplate needed for mock
        this.guacamoleProperties = properties;
        this.mockProperties = mockProperties;
        log.info("MockGuacamoleClient initialized - Running in DEMO MODE");
    }

    @Override
    public AuthResponse authenticate() {
        log.debug("Mock: Authenticating with Guacamole");
        AuthResponse response = new AuthResponse();
        response.setAuthToken(mockAuthToken);
        response.setUsername(guacamoleProperties.getUsername());
        response.setDataSource(guacamoleProperties.getDataSource());
        response.setAvailableDataSources(Collections.singletonList(guacamoleProperties.getDataSource()));
        return response;
    }

    @Override
    public Map<String, ConnectionResponse> listConnections() {
        log.debug("Mock: Listing connections - {} connections in registry", connections.size());
        return new HashMap<>(connections);
    }

    @Override
    public ConnectionResponse getConnection(String connectionId) {
        log.debug("Mock: Getting connection {}", connectionId);
        ConnectionResponse connection = connections.get(connectionId);
        if (connection == null) {
            throw new ConnectionNotFoundException(connectionId, guacamoleProperties.getDataSource());
        }
        return connection;
    }

    @Override
    public ConnectionResponse createConnection(String name, String protocol, String hostname, Integer port,
            Map<String, String> extraParams) {
        log.info("Mock: Creating {} connection '{}' to {}:{}", protocol, name, hostname, port);

        // Simulate delay
        simulateDelay(mockProperties.getConnection().getCreateDelay());

        String connectionId = String.valueOf(connectionIdCounter.getAndIncrement());

        Map<String, String> parameters = new HashMap<>();
        parameters.put("hostname", hostname);
        parameters.put("port", String.valueOf(port));
        if (extraParams != null) {
            parameters.putAll(extraParams);
        }

        ConnectionResponse response = new ConnectionResponse();
        response.setIdentifier(connectionId);
        response.setName(name);
        response.setProtocol(protocol);
        response.setParentIdentifier("ROOT");
        response.setParameters(parameters);
        response.setAttributes(Collections.singletonMap("max-connections", "1"));

        connections.put(connectionId, response);
        log.info("Mock: Connection {} created successfully", connectionId);

        return response;
    }

    @Override
    public ConnectionResponse createSpiceConnection(String name, String hostname, Integer port, String password) {
        log.info("Mock: Creating SPICE connection '{}'", name);
        Map<String, String> params = new HashMap<>();
        if (password != null && !password.isEmpty()) {
            params.put("password", password);
        }
        params.put("enable-audio", "true");
        params.put("resize-method", "reconnect");

        return createConnection(name, "spice", hostname, port, params);
    }

    @Override
    public ConnectionResponse createVncConnection(String name, String hostname, Integer port, String password) {
        log.info("Mock: Creating VNC connection '{}'", name);
        Map<String, String> params = new HashMap<>();
        if (password != null && !password.isEmpty()) {
            params.put("password", password);
        }
        params.put("color-depth", "24");
        params.put("cursor", "remote");
        params.put("read-only", "false");

        return createConnection(name, "vnc", hostname, port, params);
    }

    @Override
    public ConnectionResponse createRdpConnection(String name, String hostname, Integer port, String username,
            String password) {
        log.info("Mock: Creating RDP connection '{}'", name);
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("security", "any");
        params.put("ignore-cert", "true");
        params.put("enable-wallpaper", "true");
        params.put("enable-font-smoothing", "true");

        return createConnection(name, "rdp", hostname, port, params);
    }

    @Override
    public void deleteConnection(String connectionId) {
        log.info("Mock: Deleting connection {}", connectionId);
        ConnectionResponse removed = connections.remove(connectionId);
        if (removed != null) {
            log.info("Mock: Connection {} deleted successfully", connectionId);
        } else {
            log.warn("Mock: Connection {} not found during deletion", connectionId);
        }
    }

    @Override
    public String generateClientUrl(String connectionId) {
        log.debug("Mock: Generating client URL for connection {}", connectionId);

        // Generate a mock URL that looks realistic
        // In mock mode, this will point to a demo page
        String baseUrl = guacamoleProperties.getUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // Create a mock encoded ID similar to real Guacamole
        String rawString = connectionId + "\0" + "c" + "\0" + guacamoleProperties.getDataSource();
        String encodedId = Base64.getEncoder().encodeToString(rawString.getBytes());

        return String.format("%s/#/client/%s?mock=true", baseUrl, encodedId);
    }

    @Override
    public void checkGuacamoleHealth() {
        log.debug("Mock: Guacamole health check - OK");
        // Always healthy in mock mode
    }

    // Helper methods

    private void simulateDelay(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Mock delay interrupted", e);
        }
    }
}

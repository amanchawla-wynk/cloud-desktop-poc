package com.xstream.clouddesktop.client.guacamole;

import com.xstream.clouddesktop.client.guacamole.dto.*;
import com.xstream.clouddesktop.client.guacamole.exception.*;
import com.xstream.clouddesktop.config.GuacamoleProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@Profile("!mock") // Only active when NOT using mock profile
public class GuacamoleClient {

    private final RestTemplate restTemplate;
    private final GuacamoleProperties properties;

    private String cachedAuthToken;
    private Instant tokenExpiry;

    // Token validity buffer (refresh if within 5 minutes of expiry) - assuming 1
    // hour default validity
    private static final long TOKEN_VALIDITY_SECONDS = 3600;
    private static final long TOKEN_BUFFER_SECONDS = 300;

    public GuacamoleClient(@Qualifier("guacamoleRestTemplate") RestTemplate restTemplate,
            GuacamoleProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public AuthResponse authenticate() {
        String url = String.format("%s/api/tokens", properties.getUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", properties.getUsername());
        map.add("password", properties.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    AuthResponse.class);

            AuthResponse authResponse = response.getBody();
            if (authResponse != null) {
                this.cachedAuthToken = authResponse.getAuthToken();
                this.tokenExpiry = Instant.now().plusSeconds(TOKEN_VALIDITY_SECONDS);
                return authResponse;
            } else {
                throw new GuacamoleAuthException("Authentication response was empty");
            }
        } catch (Exception e) {
            log.error("Failed to authenticate with Guacamole", e);
            throw new GuacamoleAuthException("Authentication failed", e);
        }
    }

    private String getAuthToken() {
        if (cachedAuthToken == null || tokenExpiry == null
                || Instant.now().plusSeconds(TOKEN_BUFFER_SECONDS).isAfter(tokenExpiry)) {
            authenticate();
        }
        return cachedAuthToken;
    }

    public Map<String, ConnectionResponse> listConnections() {
        String token = getAuthToken();
        String url = String.format("%s/api/session/data/%s/connections?token=%s",
                properties.getUrl(), properties.getDataSource(), token);

        try {
            ResponseEntity<Map<String, ConnectionResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody()).orElse(Collections.emptyMap());
        } catch (Exception e) {
            log.error("Error listing connections", e);
            throw new GuacamoleException("Failed to list connections", e);
        }
    }

    public ConnectionResponse getConnection(String connectionId) {
        String token = getAuthToken();
        String url = String.format("%s/api/session/data/%s/connections/%s?token=%s",
                properties.getUrl(), properties.getDataSource(), connectionId, token);

        try {
            ResponseEntity<ConnectionResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    ConnectionResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ConnectionNotFoundException(connectionId, properties.getDataSource());
        } catch (Exception e) {
            log.error("Error getting connection {}", connectionId, e);
            throw new GuacamoleException("Failed to get connection", e);
        }
    }

    public ConnectionResponse createConnection(String name, String protocol, String hostname, Integer port,
            Map<String, String> extraParams) {
        String token = getAuthToken();
        String url = String.format("%s/api/session/data/%s/connections?token=%s",
                properties.getUrl(), properties.getDataSource(), token);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("hostname", hostname);
        parameters.put("port", String.valueOf(port));
        if (extraParams != null) {
            parameters.putAll(extraParams);
        }

        CreateConnectionRequest request = CreateConnectionRequest.builder()
                .parentIdentifier("ROOT")
                .name(name)
                .protocol(protocol)
                .parameters(parameters)
                .attributes(Collections.singletonMap("max-connections", "1"))
                .build();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<ConnectionResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    ConnectionResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error creating connection {}", name, e);
            throw new GuacamoleException("Failed to create connection", e);
        }
    }

    public ConnectionResponse createSpiceConnection(String name, String hostname, Integer port, String password) {
        Map<String, String> params = new HashMap<>();
        if (password != null && !password.isEmpty()) {
            params.put("password", password);
        }
        params.put("enable-audio", "true");
        params.put("resize-method", "reconnect");

        return createConnection(name, "spice", hostname, port, params);
    }

    public ConnectionResponse createVncConnection(String name, String hostname, Integer port, String password) {
        Map<String, String> params = new HashMap<>();
        if (password != null && !password.isEmpty()) {
            params.put("password", password);
        }
        params.put("color-depth", "24");
        params.put("cursor", "remote");
        params.put("read-only", "false");

        return createConnection(name, "vnc", hostname, port, params);
    }

    public ConnectionResponse createRdpConnection(String name, String hostname, Integer port, String username,
            String password) {
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("security", "any");
        params.put("ignore-cert", "true");
        params.put("enable-wallpaper", "true");
        params.put("enable-font-smoothing", "true");

        return createConnection(name, "rdp", hostname, port, params);
    }

    public void deleteConnection(String connectionId) {
        String token = getAuthToken();
        String url = String.format("%s/api/session/data/%s/connections/%s?token=%s",
                properties.getUrl(), properties.getDataSource(), connectionId, token);

        try {
            restTemplate.delete(url);
        } catch (HttpClientErrorException.NotFound e) {
            // Ignore if already deleted
            log.warn("Connection {} not found during deletion", connectionId);
        } catch (Exception e) {
            log.error("Error deleting connection {}", connectionId, e);
            throw new GuacamoleException("Failed to delete connection", e);
        }
    }

    public String generateClientUrl(String connectionId) {
        // Format: connectionId + "\0" + "c" + "\0" + dataSource
        String rawString = connectionId + "\0" + "c" + "\0" + properties.getDataSource();
        String encodedId = Base64.getEncoder().encodeToString(rawString.getBytes(StandardCharsets.UTF_8));

        // Ensure URL ends with / if not present before appending fragment
        String baseUrl = properties.getUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return String.format("%s/#/client/%s", baseUrl, encodedId);
    }

    public void checkGuacamoleHealth() {
        try {
            authenticate();
        } catch (GuacamoleAuthException e) {
            throw new GuacamoleException("Guacamole health check failed", e);
        }
    }
}

package com.xstream.clouddesktop.client.guacamole;

import com.xstream.clouddesktop.client.guacamole.dto.*;
import com.xstream.clouddesktop.client.guacamole.exception.GuacamoleAuthException;
import com.xstream.clouddesktop.config.GuacamoleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuacamoleClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GuacamoleProperties properties;

    private GuacamoleClient guacamoleClient;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getUrl()).thenReturn("http://guacamole:8080/guacamole");
        lenient().when(properties.getUsername()).thenReturn("guacadmin");
        lenient().when(properties.getPassword()).thenReturn("guacadmin");
        lenient().when(properties.getDataSource()).thenReturn("mysql");

        guacamoleClient = new GuacamoleClient(restTemplate, properties);
    }

    @Test
    void authenticate_shouldReturnToken() {
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAuthToken("test-token");
        authResponse.setUsername("guacadmin");
        authResponse.setDataSource("mysql");

        when(restTemplate.exchange(
                eq("http://guacamole:8080/guacamole/api/tokens"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AuthResponse.class))).thenReturn(new ResponseEntity<>(authResponse, HttpStatus.OK));

        AuthResponse result = guacamoleClient.authenticate();

        assertNotNull(result);
        assertEquals("test-token", result.getAuthToken());
    }

    @Test
    void authenticate_shouldThrowException_whenFailed() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AuthResponse.class))).thenThrow(new RuntimeException("Auth failed"));

        assertThrows(GuacamoleAuthException.class, () -> guacamoleClient.authenticate());
    }

    @Test
    void createConnection_shouldReturnConnection() {
        // Mock authentication first
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAuthToken("test-token");
        when(restTemplate.exchange(
                eq("http://guacamole:8080/guacamole/api/tokens"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AuthResponse.class))).thenReturn(new ResponseEntity<>(authResponse, HttpStatus.OK));

        // Mock create connection
        ConnectionResponse connectionResponse = new ConnectionResponse();
        connectionResponse.setIdentifier("123");
        connectionResponse.setName("test-connection");

        when(restTemplate.exchange(
                contains("/connections?token=test-token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(ConnectionResponse.class))).thenReturn(new ResponseEntity<>(connectionResponse, HttpStatus.OK));

        ConnectionResponse result = guacamoleClient.createConnection("test-connection", "spice", "localhost", 5900,
                null);

        assertNotNull(result);
        assertEquals("123", result.getIdentifier());
        assertEquals("test-connection", result.getName());
    }

    @Test
    void listConnections_shouldReturnMap() {
        // Mock authentication
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAuthToken("test-token");
        when(restTemplate.exchange(
                eq("http://guacamole:8080/guacamole/api/tokens"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AuthResponse.class))).thenReturn(new ResponseEntity<>(authResponse, HttpStatus.OK));

        // Mock list connections
        ConnectionResponse conn1 = new ConnectionResponse();
        conn1.setIdentifier("1");

        Map<String, ConnectionResponse> connections = Collections.singletonMap("1", conn1);

        when(restTemplate.exchange(
                contains("/connections?token=test-token"),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<Map<String, ConnectionResponse>>>any()))
                .thenReturn(new ResponseEntity<>(connections, HttpStatus.OK));

        Map<String, ConnectionResponse> result = guacamoleClient.listConnections();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("1"));
    }

    @Test
    void generateClientUrl_shouldEncodeCorrectly() {
        // Connection ID: "42"
        // Data Source: "mysql"
        // Expected Base64 of "42\0c\0mysql" -> "NDIAYwBteXNxbA=="

        String url = guacamoleClient.generateClientUrl("42");

        assertEquals("http://guacamole:8080/guacamole/#/client/NDIAYwBteXNxbA==", url);
    }
}

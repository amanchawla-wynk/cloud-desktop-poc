package com.xstream.clouddesktop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xstream.clouddesktop.dto.request.CreateDesktopRequest;
import com.xstream.clouddesktop.model.Desktop;
import com.xstream.clouddesktop.model.DesktopPlan;
import com.xstream.clouddesktop.model.DesktopStatus;
import com.xstream.clouddesktop.service.DesktopService;
import com.xstream.clouddesktop.service.exception.DesktopNotFoundException;
import com.xstream.clouddesktop.service.exception.InvalidDesktopStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import com.xstream.clouddesktop.controller.advice.GlobalExceptionHandler;
import com.xstream.clouddesktop.controller.advice.RequestLoggingFilter;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DesktopController.class)
@Import({GlobalExceptionHandler.class, RequestLoggingFilter.class})
class DesktopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DesktopService desktopService;

    private Desktop createTestDesktop(Long id, String userId, DesktopStatus status) {
        return Desktop.builder()
                .id(id)
                .userId(userId)
                .name("test-desktop")
                .plan(DesktopPlan.BASIC)
                .status(status)
                .statusMessage("Status message")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .vmId(100)
                .connectionId("test-connection")
                .build();
    }

    @Test
    void createDesktop_shouldReturn201_whenRequestIsValid() throws Exception {
        CreateDesktopRequest request = new CreateDesktopRequest("test-user", "My Desktop", "BASIC");
        Desktop createdDesktop = createTestDesktop(1L, "test-user", DesktopStatus.RUNNING);

        when(desktopService.createDesktop(any(), any(), any())).thenReturn(createdDesktop);

        mockMvc.perform(post("/api/v1/desktops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.userId").value("test-user"));
    }

    @Test
    void createDesktop_shouldReturn400_whenUserIdIsMissing() throws Exception {
        CreateDesktopRequest request = new CreateDesktopRequest(null, "My Desktop", "BASIC");

        mockMvc.perform(post("/api/v1/desktops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.userId").value("User ID is required"));
    }

    @Test
    void getDesktopById_shouldReturn200_whenDesktopExists() throws Exception {
        Desktop desktop = createTestDesktop(1L, "test-user", DesktopStatus.RUNNING);
        when(desktopService.getDesktop(1L)).thenReturn(desktop);

        mockMvc.perform(get("/api/v1/desktops/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    void getDesktopById_shouldReturn404_whenDesktopNotFound() throws Exception {
        when(desktopService.getDesktop(99L)).thenThrow(new DesktopNotFoundException(99L));

        mockMvc.perform(get("/api/v1/desktops/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("DESKTOP_NOT_FOUND"));
    }

    @Test
    void getDesktopConnection_shouldReturn200_whenDesktopIsRunning() throws Exception {
        Desktop desktop = createTestDesktop(1L, "test-user", DesktopStatus.RUNNING);
        desktop.setConnectionUrl("http://test-url");
        when(desktopService.getDesktopConnection(1L)).thenReturn(desktop);

        mockMvc.perform(get("/api/v1/desktops/1/connect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.connectionUrl").value("http://test-url"));
    }

    @Test
    void getDesktopConnection_shouldReturn409_whenDesktopIsNotRunning() throws Exception {
        when(desktopService.getDesktopConnection(1L)).thenThrow(new InvalidDesktopStateException(1L, DesktopStatus.STOPPED, "CONNECT"));

        mockMvc.perform(get("/api/v1/desktops/1/connect"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_STATE"));
    }

    @Test
    void startDesktop_shouldReturn200_whenSuccessful() throws Exception {
        Desktop desktop = createTestDesktop(1L, "test-user", DesktopStatus.RUNNING);
        when(desktopService.startDesktop(1L)).thenReturn(desktop);

        mockMvc.perform(post("/api/v1/desktops/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void startDesktop_shouldReturn409_whenInInvalidState() throws Exception {
        when(desktopService.startDesktop(anyLong())).thenThrow(new InvalidDesktopStateException(1L, DesktopStatus.RUNNING, "START"));

        mockMvc.perform(post("/api/v1/desktops/1/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_STATE"));
    }

    @Test
    void stopDesktop_shouldReturn200_whenSuccessful() throws Exception {
        Desktop desktop = createTestDesktop(1L, "test-user", DesktopStatus.STOPPED);
        when(desktopService.stopDesktop(1L, false)).thenReturn(desktop);

        mockMvc.perform(post("/api/v1/desktops/1/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("STOPPED"));
    }

    @Test
    void deleteDesktop_shouldReturn200_whenSuccessful() throws Exception {
        doNothing().when(desktopService).deleteDesktop(1L);

        mockMvc.perform(delete("/api/v1/desktops/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Desktop deleted successfully."));
    }
}

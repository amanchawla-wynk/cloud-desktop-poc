package com.xstream.clouddesktop.controller;

import com.xstream.clouddesktop.dto.request.CreateDesktopRequest;
import com.xstream.clouddesktop.dto.request.StartDesktopRequest;
import com.xstream.clouddesktop.dto.request.StopDesktopRequest;
import com.xstream.clouddesktop.dto.response.*;
import com.xstream.clouddesktop.model.Desktop;
import com.xstream.clouddesktop.model.DesktopPlan;
import com.xstream.clouddesktop.service.DesktopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/desktops")
@RequiredArgsConstructor
@Validated
@Slf4j
public class DesktopController {

    private final DesktopService desktopService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DesktopResponse> createDesktop(@Valid @RequestBody CreateDesktopRequest request) {
        log.info("Received request to create desktop for user: {}", request.getUserId());
        DesktopPlan plan = Optional.ofNullable(request.getPlan()).map(DesktopPlan::valueOf).orElse(DesktopPlan.BASIC);
        String name = Optional.ofNullable(request.getName()).orElse("Desktop-" + request.getUserId());

        Desktop desktop = desktopService.createDesktop(request.getUserId(), name, plan);

        return ApiResponse.success(DesktopResponse.fromEntity(desktop), "Desktop creation initiated successfully.");
    }

    @GetMapping
    public ApiResponse<DesktopListResponse> listDesktops(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status) {
        log.info("Received request to list desktops with filter userId: {} and status: {}", userId, status);
        List<Desktop> desktops = desktopService.findDesktops(userId, status);
        return ApiResponse.success(DesktopListResponse.fromEntities(desktops));
    }

    @GetMapping("/{id}")
    public ApiResponse<DesktopResponse> getDesktopById(@PathVariable Long id) {
        log.info("Received request to get desktop with ID: {}", id);
        Desktop desktop = desktopService.getDesktop(id);
        return ApiResponse.success(DesktopResponse.fromEntity(desktop));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<DesktopResponse> getDesktopByUserId(@PathVariable String userId) {
        log.info("Received request to get desktop for user: {}", userId);
        Desktop desktop = desktopService.getDesktopByUserId(userId);
        return ApiResponse.success(DesktopResponse.fromEntity(desktop));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<DesktopStatusResponse> getDesktopStatus(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean refresh) {
        log.info("Received request to get status for desktop ID: {}. Refresh: {}", id, refresh);
        Desktop desktop = desktopService.getDesktop(id, refresh);
        return ApiResponse.success(DesktopStatusResponse.fromEntity(desktop));
    }

    @GetMapping("/{id}/connect")
    public ApiResponse<ConnectionResponse> getDesktopConnection(@PathVariable Long id) {
        log.info("Received request to get connection for desktop ID: {}", id);
        Desktop desktop = desktopService.getDesktopConnection(id);
        ConnectionResponse connectionResponse = ConnectionResponse.builder()
                .desktopId(desktop.getId())
                .connectionUrl(desktop.getConnectionUrl())
                .protocol("spice") // Assuming SPICE, could be dynamic
                .build();
        return ApiResponse.success(connectionResponse);
    }

    @PostMapping("/{id}/start")
    public ApiResponse<DesktopResponse> startDesktop(
            @PathVariable Long id,
            @RequestBody(required = false) StartDesktopRequest request) {
        log.info("Received request to start desktop ID: {}", id);
        Desktop desktop = desktopService.startDesktop(id);
        return ApiResponse.success(DesktopResponse.fromEntity(desktop), "Desktop started successfully.");
    }

    @PostMapping("/{id}/stop")
    public ApiResponse<DesktopResponse> stopDesktop(
            @PathVariable Long id,
            @RequestBody(required = false) StopDesktopRequest request) {
        boolean force = (request != null) && request.isForce();
        log.info("Received request to stop desktop ID: {}. Force: {}", id, force);
        Desktop desktop = desktopService.stopDesktop(id, force);
        return ApiResponse.success(DesktopResponse.fromEntity(desktop), "Desktop stopped successfully.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDesktop(@PathVariable Long id) {
        log.info("Received request to delete desktop ID: {}", id);
        desktopService.deleteDesktop(id);
        return ApiResponse.success(null, "Desktop deleted successfully.");
    }
}

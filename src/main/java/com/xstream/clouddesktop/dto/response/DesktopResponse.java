package com.xstream.clouddesktop.dto.response;

import com.xstream.clouddesktop.model.Desktop;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
@Builder
public class DesktopResponse {
    private Long id;
    private String userId;
    private String name;
    private String status;
    private String statusMessage;
    private String plan;
    private Integer cpuCores;
    private Integer memoryMb;
    private String ipAddress;
    private String connectionUrl;
    private String createdAt;
    private String updatedAt;
    private String lastAccessedAt;

    public static DesktopResponse fromEntity(Desktop desktop) {
        return DesktopResponse.builder()
                .id(desktop.getId())
                .userId(desktop.getUserId())
                .name(desktop.getName())
                .status(desktop.getStatus().name())
                .statusMessage(desktop.getStatusMessage())
                .plan(desktop.getPlan().name())
                .cpuCores(desktop.getCpuCores())
                .memoryMb(desktop.getMemoryMb())
                .ipAddress(desktop.getVmIpAddress())
                .connectionUrl(desktop.getConnectionUrl())
                .createdAt(formatInstant(desktop.getCreatedAt()))
                .updatedAt(formatInstant(desktop.getUpdatedAt()))
                .lastAccessedAt(formatInstant(desktop.getLastAccessedAt()))
                .build();
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }
}

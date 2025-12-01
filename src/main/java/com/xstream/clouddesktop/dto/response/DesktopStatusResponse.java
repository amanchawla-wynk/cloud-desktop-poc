package com.xstream.clouddesktop.dto.response;

import com.xstream.clouddesktop.model.Desktop;
import com.xstream.clouddesktop.model.DesktopStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DesktopStatusResponse {
    private Long desktopId;
    private String status;
    private String statusMessage;
    private boolean ready;

    public static DesktopStatusResponse fromEntity(Desktop desktop) {
        return DesktopStatusResponse.builder()
                .desktopId(desktop.getId())
                .status(desktop.getStatus().name())
                .statusMessage(desktop.getStatusMessage())
                .ready(desktop.getStatus() == DesktopStatus.RUNNING)
                .build();
    }
}

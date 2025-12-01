package com.xstream.clouddesktop.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectionResponse {
    private Long desktopId;
    private String connectionUrl;
    private String protocol;
    private String expiresAt;
}

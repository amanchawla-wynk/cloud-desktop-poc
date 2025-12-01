package com.xstream.clouddesktop.client.guacamole.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ConnectionResponse {
    private String identifier;
    private String parentIdentifier;
    private String name;
    private String protocol;
    private Map<String, String> parameters;
    private Map<String, String> attributes;
    private Integer activeConnections;
}

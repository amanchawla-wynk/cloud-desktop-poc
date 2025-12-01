package com.xstream.clouddesktop.client.guacamole.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class CreateConnectionRequest {
    private String parentIdentifier;
    private String name;
    private String protocol;
    private Map<String, String> parameters;
    private Map<String, String> attributes;
}

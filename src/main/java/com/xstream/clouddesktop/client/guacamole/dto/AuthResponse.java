package com.xstream.clouddesktop.client.guacamole.dto;

import lombok.Data;
import java.util.List;

@Data
public class AuthResponse {
    private String authToken;
    private String username;
    private String dataSource;
    private List<String> availableDataSources;
}

package com.xstream.clouddesktop.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Apache Guacamole integration.
 * 
 * These properties are loaded from application.yml under the 'guacamole'
 * prefix.
 * All required fields are validated using Jakarta Bean Validation.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "guacamole")
public class GuacamoleProperties {

    /**
     * Guacamole server URL (e.g., http://localhost:8081/guacamole)
     */
    @NotBlank(message = "Guacamole URL is required")
    private String url;

    /**
     * Admin username for Guacamole authentication
     */
    @NotBlank(message = "Guacamole username is required")
    private String username;

    /**
     * Admin password for Guacamole authentication
     */
    @NotBlank(message = "Guacamole password is required")
    private String password;

    /**
     * Data source type (e.g., "mysql" or "postgresql")
     */
    private String dataSource;

    /**
     * Default protocol for remote desktop connections (e.g., "SPICE" or "VNC")
     */
    private String defaultProtocol = "SPICE";

}

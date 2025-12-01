package com.xstream.clouddesktop.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Proxmox VE integration.
 * 
 * These properties are loaded from application.yml under the 'proxmox' prefix.
 * All required fields are validated using Jakarta Bean Validation.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "proxmox")
public class ProxmoxProperties {

    /**
     * Proxmox server URL (e.g., https://proxmox.local:8006)
     */
    @NotBlank(message = "Proxmox URL is required")
    private String url;

    /**
     * Proxmox node name (e.g., pve)
     */
    @NotBlank(message = "Proxmox node name is required")
    private String node;

    /**
     * API user for authentication (e.g., root@pam)
     */
    @NotBlank(message = "Proxmox username is required")
    private String username;

    /**
     * API token ID for authentication
     */
    private String tokenId;

    /**
     * API token secret for authentication
     */
    private String tokenSecret;

    /**
     * VM template ID to clone from (e.g., 9000)
     */
    private Integer templateVmId;

    /**
     * Default number of CPU cores for new VMs (e.g., 2)
     */
    private Integer defaultCores;

    /**
     * Default RAM in MB for new VMs (e.g., 4096)
     */
    private Integer defaultMemoryMb;

}

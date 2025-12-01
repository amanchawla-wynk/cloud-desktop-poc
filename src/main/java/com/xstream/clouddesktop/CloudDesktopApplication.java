package com.xstream.clouddesktop;

import com.xstream.clouddesktop.config.GuacamoleProperties;
import com.xstream.clouddesktop.config.ProxmoxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main Spring Boot application class for Cloud Desktop POC.
 * 
 * This application provides a foundation for integrating with:
 * - Proxmox VE for virtual machine management
 * - Apache Guacamole for remote desktop access
 */
@SpringBootApplication
@EnableConfigurationProperties({ProxmoxProperties.class, GuacamoleProperties.class})
public class CloudDesktopApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudDesktopApplication.class, args);
    }

}

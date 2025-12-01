package com.xstream.clouddesktop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for mock infrastructure timing
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mock")
public class MockProperties {

    private VmTiming vm = new VmTiming();
    private ConnectionTiming connection = new ConnectionTiming();

    @Data
    public static class VmTiming {
        private long cloneDelayMin = 30000; // 30 seconds
        private long cloneDelayMax = 45000; // 45 seconds
        private long startDelayMin = 20000; // 20 seconds
        private long startDelayMax = 30000; // 30 seconds
        private long stopDelayMin = 10000; // 10 seconds
        private long stopDelayMax = 15000; // 15 seconds
        private long ipDelayMin = 10000; // 10 seconds
        private long ipDelayMax = 20000; // 20 seconds
    }

    @Data
    public static class ConnectionTiming {
        private long createDelay = 5000; // 5 seconds
    }
}

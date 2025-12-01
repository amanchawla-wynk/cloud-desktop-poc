package com.xstream.clouddesktop.client.proxmox.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Mock VM data structure for in-memory storage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockVm {
    private Integer vmId;
    private String name;
    private String status; // "running" or "stopped"
    private String ipAddress;
    private Integer cpuCores;
    private Integer memoryMb;
    private Instant createdAt;
    private Integer templateId;
}

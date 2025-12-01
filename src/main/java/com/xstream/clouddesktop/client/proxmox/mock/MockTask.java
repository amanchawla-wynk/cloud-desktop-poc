package com.xstream.clouddesktop.client.proxmox.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Mock task data structure for simulating Proxmox async operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockTask {
    private String upid;
    private String status; // "running" or "stopped"
    private String exitStatus; // "OK" or "ERROR"
    private Instant startTime;
    private Instant completionTime;
    private String taskType; // "clone", "start", "stop", "delete"
    private Integer vmId;
}

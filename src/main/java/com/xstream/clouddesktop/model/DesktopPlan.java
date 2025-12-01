package com.xstream.clouddesktop.model;

import lombok.Getter;

@Getter
public enum DesktopPlan {
    BASIC(2, 2048, "Basic", "2 vCPU, 2GB RAM"),
    STANDARD(4, 4096, "Standard", "4 vCPU, 4GB RAM"),
    PREMIUM(8, 8192, "Premium", "8 vCPU, 8GB RAM");

    private final int cpuCores;
    private final int memoryMb;
    private final String displayName;
    private final String description;

    DesktopPlan(int cpuCores, int memoryMb, String displayName, String description) {
        this.cpuCores = cpuCores;
        this.memoryMb = memoryMb;
        this.displayName = displayName;
        this.description = description;
    }
}

package com.xstream.clouddesktop.client.proxmox.dto;

import lombok.Data;

@Data
public class VmInfo {
    private Integer vmid;
    private String name;
    private String status;
    private Integer cpus;
    private Long maxmem;
    private Long maxdisk;
    private Long uptime;
    private Long netin;
    private Long netout;
}

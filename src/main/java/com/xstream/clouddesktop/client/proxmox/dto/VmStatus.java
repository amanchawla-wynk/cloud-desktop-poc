package com.xstream.clouddesktop.client.proxmox.dto;

import lombok.Data;

@Data
public class VmStatus {
    private Integer vmid;
    private String name;
    private String status;
    private Integer cpus;
    private Long mem;
    private Long maxmem;
    private Long disk;
    private Long maxdisk;
    private Long uptime;
    private Integer pid;
    private String qmpstatus;
}

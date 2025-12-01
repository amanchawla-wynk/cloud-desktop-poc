package com.xstream.clouddesktop.client.proxmox.dto;

import lombok.Data;

@Data
public class VmConfig {
    private String name;
    private Integer cores;
    private Long memory;
    private String net0;
    private String ide0;
    private String scsi0;
    private String boot;
}

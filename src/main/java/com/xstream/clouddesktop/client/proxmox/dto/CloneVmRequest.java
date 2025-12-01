package com.xstream.clouddesktop.client.proxmox.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloneVmRequest {
    private Integer newid;
    private String name;
    private Boolean full;
    private String target;
    private String storage;
}

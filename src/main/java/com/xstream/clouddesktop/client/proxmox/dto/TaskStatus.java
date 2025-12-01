package com.xstream.clouddesktop.client.proxmox.dto;

import lombok.Data;

@Data
public class TaskStatus {
    private String status;
    private String exitstatus;
    private String type;
    private String id;
    private String node;
    private Integer pid;
    private Long starttime;
    private Long endtime;
}

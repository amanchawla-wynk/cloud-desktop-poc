package com.xstream.clouddesktop.client.proxmox.dto;

import lombok.Data;

@Data
public class ProxmoxResponse<T> {
    private T data;
    private Object errors;
}

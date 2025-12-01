package com.xstream.clouddesktop.client.proxmox.exception;

import lombok.Getter;

@Getter
public class ProxmoxException extends RuntimeException {
    private final int statusCode;
    private final String endpoint;

    public ProxmoxException(String message) {
        super(message);
        this.statusCode = 0;
        this.endpoint = null;
    }

    public ProxmoxException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.endpoint = null;
    }

    public ProxmoxException(String message, int statusCode, String endpoint) {
        super(message);
        this.statusCode = statusCode;
        this.endpoint = endpoint;
    }
}

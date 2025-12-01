package com.xstream.clouddesktop.client.guacamole.exception;

import lombok.Getter;

@Getter
public class GuacamoleException extends RuntimeException {
    private final int statusCode;
    private final String endpoint;

    public GuacamoleException(String message) {
        super(message);
        this.statusCode = 0;
        this.endpoint = null;
    }

    public GuacamoleException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.endpoint = null;
    }

    public GuacamoleException(String message, int statusCode, String endpoint) {
        super(message);
        this.statusCode = statusCode;
        this.endpoint = endpoint;
    }
}

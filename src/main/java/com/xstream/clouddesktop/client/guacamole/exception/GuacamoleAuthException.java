package com.xstream.clouddesktop.client.guacamole.exception;

public class GuacamoleAuthException extends GuacamoleException {
    public GuacamoleAuthException(String message) {
        super(message);
    }

    public GuacamoleAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}

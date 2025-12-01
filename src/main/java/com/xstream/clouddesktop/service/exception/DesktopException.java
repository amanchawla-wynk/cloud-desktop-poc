package com.xstream.clouddesktop.service.exception;

import lombok.Getter;

@Getter
public class DesktopException extends RuntimeException {
    private final Long desktopId;
    private final String userId;

    public DesktopException(String message) {
        super(message);
        this.desktopId = null;
        this.userId = null;
    }

    public DesktopException(String message, Throwable cause) {
        super(message, cause);
        this.desktopId = null;
        this.userId = null;
    }

    public DesktopException(String message, Long desktopId, String userId) {
        super(message);
        this.desktopId = desktopId;
        this.userId = userId;
    }

    public DesktopException(String message, Throwable cause, Long desktopId, String userId) {
        super(message, cause);
        this.desktopId = desktopId;
        this.userId = userId;
    }
}

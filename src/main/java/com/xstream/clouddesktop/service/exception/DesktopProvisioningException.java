package com.xstream.clouddesktop.service.exception;

public class DesktopProvisioningException extends DesktopException {
    public DesktopProvisioningException(String message, Long desktopId, String userId) {
        super(message, desktopId, userId);
    }

    public DesktopProvisioningException(String message, Throwable cause, Long desktopId, String userId) {
        super(message, cause, desktopId, userId);
    }
}

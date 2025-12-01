package com.xstream.clouddesktop.service.exception;

public class DesktopNotFoundException extends DesktopException {
    public DesktopNotFoundException(Long desktopId) {
        super("Desktop not found with ID: " + desktopId, desktopId, null);
    }

    public DesktopNotFoundException(String userId) {
        super("Active desktop not found for user: " + userId, null, userId);
    }
}

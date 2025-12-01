package com.xstream.clouddesktop.service.exception;

public class DesktopAlreadyExistsException extends DesktopException {
    public DesktopAlreadyExistsException(String userId, Long desktopId) {
        super("User " + userId + " already has an active desktop (ID: " + desktopId + ")", desktopId, userId);
    }
}

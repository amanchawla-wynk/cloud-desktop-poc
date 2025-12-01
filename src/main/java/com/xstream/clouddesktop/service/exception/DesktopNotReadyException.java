package com.xstream.clouddesktop.service.exception;

import com.xstream.clouddesktop.model.DesktopStatus;

public class DesktopNotReadyException extends DesktopException {
    public DesktopNotReadyException(Long desktopId, DesktopStatus status) {
        super("Desktop " + desktopId + " is not ready for connection. Current status: " + status, desktopId, null);
    }
}

package com.xstream.clouddesktop.service.exception;

import com.xstream.clouddesktop.model.DesktopStatus;

public class InvalidDesktopStateException extends DesktopException {
    public InvalidDesktopStateException(Long desktopId, DesktopStatus currentStatus, String operation) {
        super("Cannot perform operation '" + operation + "' on desktop " + desktopId + " in state " + currentStatus,
                desktopId, null);
    }
}

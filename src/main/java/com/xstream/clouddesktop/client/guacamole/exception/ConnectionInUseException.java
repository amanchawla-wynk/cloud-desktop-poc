package com.xstream.clouddesktop.client.guacamole.exception;

public class ConnectionInUseException extends GuacamoleException {
    public ConnectionInUseException(String message) {
        super(message);
    }

    public ConnectionInUseException(String connectionId, int activeConnections) {
        super("Connection " + connectionId + " is currently in use by " + activeConnections + " active sessions");
    }
}

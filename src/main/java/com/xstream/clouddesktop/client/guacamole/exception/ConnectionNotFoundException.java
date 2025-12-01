package com.xstream.clouddesktop.client.guacamole.exception;

public class ConnectionNotFoundException extends GuacamoleException {
    public ConnectionNotFoundException(String message) {
        super(message);
    }

    public ConnectionNotFoundException(String connectionId, String dataSource) {
        super("Connection not found with ID: " + connectionId + " in data source: " + dataSource);
    }
}

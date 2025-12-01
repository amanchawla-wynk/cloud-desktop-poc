package com.xstream.clouddesktop.model;

public enum DesktopStatus {
    PENDING, // Desktop creation requested, not yet started
    PROVISIONING, // VM is being cloned from template
    STARTING, // VM is cloned, now starting up
    WAITING_FOR_IP, // VM started, waiting for network
    CONFIGURING, // Got IP, creating Guacamole connection
    RUNNING, // Desktop is ready and accessible
    STOPPING, // Desktop is being stopped
    STOPPED, // VM is stopped but not deleted
    DELETING, // Desktop is being removed
    DELETED, // Desktop has been removed (soft delete)
    FAILED // Something went wrong
}

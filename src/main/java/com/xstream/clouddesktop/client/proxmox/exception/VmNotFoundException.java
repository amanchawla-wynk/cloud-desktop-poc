package com.xstream.clouddesktop.client.proxmox.exception;

public class VmNotFoundException extends ProxmoxException {
    public VmNotFoundException(String message) {
        super(message);
    }

    public VmNotFoundException(Integer vmId) {
        super("VM not found with ID: " + vmId);
    }
}

package com.xstream.clouddesktop.client.proxmox.exception;

public class GuestAgentNotAvailableException extends ProxmoxException {
    public GuestAgentNotAvailableException(String message) {
        super(message);
    }

    public GuestAgentNotAvailableException(Integer vmId) {
        super("QEMU Guest Agent not available for VM ID: " + vmId);
    }
}

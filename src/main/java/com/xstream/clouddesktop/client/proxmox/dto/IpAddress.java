package com.xstream.clouddesktop.client.proxmox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IpAddress {
    @JsonProperty("ip-address")
    private String ipAddress;

    @JsonProperty("ip-address-type")
    private String ipAddressType;

    private Integer prefix;
}

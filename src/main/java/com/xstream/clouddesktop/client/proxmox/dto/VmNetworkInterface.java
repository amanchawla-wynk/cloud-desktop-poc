package com.xstream.clouddesktop.client.proxmox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class VmNetworkInterface {
    private String name;

    @JsonProperty("hardware-address")
    private String hwaddr;

    @JsonProperty("ip-addresses")
    private List<IpAddress> ipAddresses;
}

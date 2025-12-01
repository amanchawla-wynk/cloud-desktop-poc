package com.xstream.clouddesktop.client.proxmox;

import com.xstream.clouddesktop.client.proxmox.dto.*;
import com.xstream.clouddesktop.client.proxmox.exception.*;
import com.xstream.clouddesktop.config.ProxmoxProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("!mock") // Only active when NOT using mock profile
public class ProxmoxClient {

    private final RestTemplate restTemplate;
    private final ProxmoxProperties properties;

    public ProxmoxClient(@Qualifier("proxmoxRestTemplate") RestTemplate restTemplate, ProxmoxProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public List<VmInfo> listVMs() {
        String url = String.format("%s/api2/json/nodes/%s/qemu", properties.getUrl(), properties.getNode());
        try {
            ResponseEntity<ProxmoxResponse<List<VmInfo>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody())
                    .map(ProxmoxResponse::getData)
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Error listing VMs", e);
            throw new ProxmoxException("Failed to list VMs", e);
        }
    }

    public VmStatus getVmStatus(Integer vmId) {
        String url = String.format("%s/api2/json/nodes/%s/qemu/%d/status/current", properties.getUrl(),
                properties.getNode(), vmId);
        try {
            ResponseEntity<ProxmoxResponse<VmStatus>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody())
                    .map(ProxmoxResponse::getData)
                    .orElseThrow(() -> new VmNotFoundException(vmId));
        } catch (HttpClientErrorException.NotFound e) {
            throw new VmNotFoundException(vmId);
        } catch (Exception e) {
            log.error("Error getting status for VM {}", vmId, e);
            throw new ProxmoxException("Failed to get VM status", e);
        }
    }

    public VmConfig getVmConfig(Integer vmId) {
        String url = String.format("%s/api2/json/nodes/%s/qemu/%d/config", properties.getUrl(), properties.getNode(),
                vmId);
        try {
            ResponseEntity<ProxmoxResponse<VmConfig>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody())
                    .map(ProxmoxResponse::getData)
                    .orElseThrow(() -> new VmNotFoundException(vmId));
        } catch (HttpClientErrorException.NotFound e) {
            throw new VmNotFoundException(vmId);
        } catch (Exception e) {
            log.error("Error getting config for VM {}", vmId, e);
            throw new ProxmoxException("Failed to get VM config", e);
        }
    }

    public List<VmNetworkInterface> getVmNetworkInterfaces(Integer vmId) {
        String url = String.format("%s/api2/json/nodes/%s/qemu/%d/agent/network-get-interfaces", properties.getUrl(),
                properties.getNode(), vmId);
        try {
            ResponseEntity<ProxmoxResponse<Map<String, List<VmNetworkInterface>>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData().get("result");
            }
            return Collections.emptyList();

        } catch (org.springframework.web.client.HttpServerErrorException.InternalServerError e) {
            // Often means guest agent is not running
            log.warn("Guest agent not available for VM {}", vmId);
            throw new GuestAgentNotAvailableException(vmId);
        } catch (Exception e) {
            log.error("Error getting network interfaces for VM {}", vmId, e);
            throw new ProxmoxException("Failed to get VM network interfaces", e);
        }
    }

    public String cloneVm(Integer templateId, Integer newVmId, String vmName) {
        String url = String.format("%s/api2/json/nodes/%s/qemu/%d/clone", properties.getUrl(), properties.getNode(),
                templateId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("newid", String.valueOf(newVmId));
        map.add("name", vmName);
        map.add("full", "1"); // Full clone

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<ProxmoxResponse<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody())
                    .map(ProxmoxResponse::getData)
                    .orElseThrow(() -> new ProxmoxException("Failed to get task UPID from clone response"));
        } catch (Exception e) {
            log.error("Error cloning VM {} to new ID {}", templateId, newVmId, e);
            throw new ProxmoxException("Failed to clone VM", e);
        }
    }

    public String startVm(Integer vmId) {
        return performVmAction(vmId, "start");
    }

    public String stopVm(Integer vmId) {
        return performVmAction(vmId, "stop");
    }

    public String shutdownVm(Integer vmId) {
        return performVmAction(vmId, "shutdown");
    }

    public String deleteVm(Integer vmId) {
        String url = String.format("%s/api2/json/nodes/%s/qemu/%d", properties.getUrl(), properties.getNode(), vmId);
        try {
            ResponseEntity<ProxmoxResponse<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody())
                    .map(ProxmoxResponse::getData)
                    .orElse(null); // Delete might not return a task ID immediately if synchronous, but usually
                                   // does
        } catch (Exception e) {
            log.error("Error deleting VM {}", vmId, e);
            throw new ProxmoxException("Failed to delete VM", e);
        }
    }

    private String performVmAction(Integer vmId, String action) {
        String url = String.format("%s/api2/json/nodes/%s/qemu/%d/status/%s", properties.getUrl(), properties.getNode(),
                vmId, action);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>("", headers);

        try {
            ResponseEntity<ProxmoxResponse<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody())
                    .map(ProxmoxResponse::getData)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error performing action {} on VM {}", action, vmId, e);
            throw new ProxmoxException("Failed to perform action " + action, e);
        }
    }

    public TaskStatus getTaskStatus(String upid) {
        String url = String.format("%s/api2/json/nodes/%s/tasks/%s/status", properties.getUrl(), properties.getNode(),
                upid);
        try {
            ResponseEntity<ProxmoxResponse<TaskStatus>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody())
                    .map(ProxmoxResponse::getData)
                    .orElseThrow(() -> new ProxmoxException("Failed to get task status"));
        } catch (Exception e) {
            log.error("Error getting task status for UPID {}", upid, e);
            throw new ProxmoxException("Failed to get task status", e);
        }
    }

    public TaskStatus waitForTask(String upid, Duration timeout) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            TaskStatus status = getTaskStatus(upid);
            if ("stopped".equals(status.getStatus())) {
                if ("OK".equals(status.getExitstatus())) {
                    return status;
                } else {
                    throw new ProxmoxException("Task failed with exit status: " + status.getExitstatus());
                }
            }
            try {
                Thread.sleep(2000); // Poll every 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProxmoxException("Interrupted while waiting for task", e);
            }
        }
        throw new ProxmoxTaskTimeoutException("Timed out waiting for task " + upid);
    }

    public Integer getNextAvailableVmId() {
        String url = String.format("%s/api2/json/cluster/nextid", properties.getUrl());
        log.debug("Requesting next available VM ID from: {}", url);
        try {
            ResponseEntity<ProxmoxResponse<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody())
                    .map(ProxmoxResponse::getData)
                    .map(Integer::parseInt)
                    .orElseThrow(() -> new ProxmoxException("Failed to get next VM ID"));
        } catch (HttpClientErrorException e) {
            log.error("HTTP {} error getting next VM ID from {}: {}",
                    e.getStatusCode(), url, e.getResponseBodyAsString());
            throw new ProxmoxException(
                    String.format("Failed to get next available VM ID - HTTP %s: %s",
                            e.getStatusCode(), e.getResponseBodyAsString()),
                    e);
        } catch (Exception e) {
            log.error("Error getting next available VM ID from {}: {}", url, e.getMessage(), e);
            throw new ProxmoxException("Failed to get next available VM ID: " + e.getMessage(), e);
        }
    }

    public String waitForVmIp(Integer vmId, Duration timeout) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                List<VmNetworkInterface> interfaces = getVmNetworkInterfaces(vmId);
                for (VmNetworkInterface iface : interfaces) {
                    if (iface.getIpAddresses() != null) {
                        for (IpAddress ip : iface.getIpAddresses()) {
                            if ("ipv4".equals(ip.getIpAddressType())
                                    && !ip.getIpAddress().startsWith("127.")
                                    && !ip.getIpAddress().startsWith("169.254.")) {
                                return ip.getIpAddress();
                            }
                        }
                    }
                }
            } catch (GuestAgentNotAvailableException e) {
                // Ignore and retry, agent might not be ready yet
                log.debug("Guest agent not ready yet for VM {}", vmId);
            } catch (Exception e) {
                log.warn("Error checking for IP address: {}", e.getMessage());
            }

            try {
                Thread.sleep(5000); // Poll every 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProxmoxException("Interrupted while waiting for IP", e);
            }
        }
        throw new ProxmoxTaskTimeoutException("Timed out waiting for IP address for VM " + vmId);
    }

    public void checkProxmoxHealth() {
        try {
            // A simple call to check connectivity and authentication
            listVMs();
        } catch (ProxmoxException e) {
            throw new ProxmoxException("Proxmox health check failed", e);
        }
    }
}

package com.xstream.clouddesktop.client.proxmox;

import com.xstream.clouddesktop.client.proxmox.dto.*;
import com.xstream.clouddesktop.client.proxmox.exception.ProxmoxTaskTimeoutException;
import com.xstream.clouddesktop.config.ProxmoxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProxmoxClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ProxmoxProperties properties;

    private ProxmoxClient proxmoxClient;

    @BeforeEach
    void setUp() {
        when(properties.getUrl()).thenReturn("https://proxmox.example.com:8006");
        when(properties.getNode()).thenReturn("pve");
        proxmoxClient = new ProxmoxClient(restTemplate, properties);
    }

    @Test
    void listVMs_shouldReturnListOfVms() {
        VmInfo vm1 = new VmInfo();
        vm1.setVmid(100);
        vm1.setName("vm1");

        ProxmoxResponse<List<VmInfo>> response = new ProxmoxResponse<>();
        response.setData(Collections.singletonList(vm1));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<ProxmoxResponse<List<VmInfo>>>>any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        List<VmInfo> vms = proxmoxClient.listVMs();

        assertNotNull(vms);
        assertEquals(1, vms.size());
        assertEquals(100, vms.get(0).getVmid());
        assertEquals("vm1", vms.get(0).getName());
    }

    @Test
    void getVmStatus_shouldReturnStatus() {
        VmStatus status = new VmStatus();
        status.setVmid(100);
        status.setStatus("running");

        ProxmoxResponse<VmStatus> response = new ProxmoxResponse<>();
        response.setData(status);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<ProxmoxResponse<VmStatus>>>any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        VmStatus result = proxmoxClient.getVmStatus(100);

        assertNotNull(result);
        assertEquals(100, result.getVmid());
        assertEquals("running", result.getStatus());
    }

    @Test
    void cloneVm_shouldReturnUpid() {
        ProxmoxResponse<String> response = new ProxmoxResponse<>();
        response.setData("UPID:pve:00001234:12345678:12345678:qmclone:100:root@pam:");

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                ArgumentMatchers.<ParameterizedTypeReference<ProxmoxResponse<String>>>any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        String upid = proxmoxClient.cloneVm(9000, 100, "new-vm");

        assertNotNull(upid);
        assertTrue(upid.startsWith("UPID:"));
    }

    @Test
    void waitForTask_shouldReturnStatusWhenComplete() {
        TaskStatus runningStatus = new TaskStatus();
        runningStatus.setStatus("running");

        TaskStatus stoppedStatus = new TaskStatus();
        stoppedStatus.setStatus("stopped");
        stoppedStatus.setExitstatus("OK");

        ProxmoxResponse<TaskStatus> runningResponse = new ProxmoxResponse<>();
        runningResponse.setData(runningStatus);

        ProxmoxResponse<TaskStatus> stoppedResponse = new ProxmoxResponse<>();
        stoppedResponse.setData(stoppedStatus);

        // First call returns running, second call returns stopped
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<ProxmoxResponse<TaskStatus>>>any()))
                .thenReturn(new ResponseEntity<>(runningResponse, HttpStatus.OK))
                .thenReturn(new ResponseEntity<>(stoppedResponse, HttpStatus.OK));

        TaskStatus result = proxmoxClient.waitForTask("UPID:...", Duration.ofSeconds(5));

        assertNotNull(result);
        assertEquals("stopped", result.getStatus());
        assertEquals("OK", result.getExitstatus());
    }

    @Test
    void waitForVmIp_shouldReturnIpWhenFound() {
        IpAddress ip = new IpAddress();
        ip.setIpAddress("192.168.1.100");
        ip.setIpAddressType("ipv4");

        VmNetworkInterface iface = new VmNetworkInterface();
        iface.setName("eth0");
        iface.setIpAddresses(Collections.singletonList(ip));

        ProxmoxResponse<Map<String, List<VmNetworkInterface>>> response = new ProxmoxResponse<>();
        Map<String, List<VmNetworkInterface>> data = Collections.singletonMap("result",
                Collections.singletonList(iface));
        response.setData(data);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers
                        .<ParameterizedTypeReference<ProxmoxResponse<Map<String, List<VmNetworkInterface>>>>>any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        String resultIp = proxmoxClient.waitForVmIp(100, Duration.ofSeconds(5));

        assertEquals("192.168.1.100", resultIp);
    }

    @Test
    void waitForVmIp_shouldIgnoreLoopback() {
        IpAddress loopback = new IpAddress();
        loopback.setIpAddress("127.0.0.1");
        loopback.setIpAddressType("ipv4");

        VmNetworkInterface iface = new VmNetworkInterface();
        iface.setName("lo");
        iface.setIpAddresses(Collections.singletonList(loopback));

        ProxmoxResponse<Map<String, List<VmNetworkInterface>>> response = new ProxmoxResponse<>();
        Map<String, List<VmNetworkInterface>> data = Collections.singletonMap("result",
                Collections.singletonList(iface));
        response.setData(data);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                ArgumentMatchers
                        .<ParameterizedTypeReference<ProxmoxResponse<Map<String, List<VmNetworkInterface>>>>>any()))
                .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        assertThrows(ProxmoxTaskTimeoutException.class, () -> {
            proxmoxClient.waitForVmIp(100, Duration.ofMillis(100));
        });
    }
}

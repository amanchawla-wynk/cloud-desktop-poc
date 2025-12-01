package com.xstream.clouddesktop.service;

import com.xstream.clouddesktop.client.guacamole.GuacamoleClient;
import com.xstream.clouddesktop.client.guacamole.dto.ConnectionResponse;
import com.xstream.clouddesktop.client.proxmox.ProxmoxClient;
import com.xstream.clouddesktop.client.proxmox.dto.VmStatus;
import com.xstream.clouddesktop.config.GuacamoleProperties;
import com.xstream.clouddesktop.config.ProxmoxProperties;
import com.xstream.clouddesktop.model.Desktop;
import com.xstream.clouddesktop.model.DesktopPlan;
import com.xstream.clouddesktop.model.DesktopStatus;
import com.xstream.clouddesktop.repository.DesktopRepository;
import com.xstream.clouddesktop.service.exception.DesktopAlreadyExistsException;
import com.xstream.clouddesktop.service.exception.DesktopProvisioningException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DesktopServiceTest {

    @Mock
    private DesktopRepository desktopRepository;
    @Mock
    private ProxmoxClient proxmoxClient;
    @Mock
    private GuacamoleClient guacamoleClient;
    @Mock
    private ProxmoxProperties proxmoxProperties;
    @Mock
    private GuacamoleProperties guacamoleProperties;

    private DesktopService desktopService;

    @BeforeEach
    void setUp() {
        lenient().when(proxmoxProperties.getTemplateVmId()).thenReturn(9000);
        lenient().when(guacamoleProperties.getDefaultProtocol()).thenReturn("spice");

        desktopService = new DesktopService(
                desktopRepository,
                proxmoxClient,
                guacamoleClient,
                proxmoxProperties,
                guacamoleProperties);
    }

    @Test
    void createDesktop_shouldSucceed() {
        // Arrange
        String userId = "user1";
        String desktopName = "My Desktop";
        DesktopPlan plan = DesktopPlan.BASIC;

        when(desktopRepository.findByUserIdAndStatusNot(eq(userId), eq(DesktopStatus.DELETED)))
                .thenReturn(Optional.empty());

        when(desktopRepository.save(any(Desktop.class))).thenAnswer(invocation -> {
            Desktop d = invocation.getArgument(0);
            if (d.getId() == null)
                d.setId(1L);
            return d;
        });

        when(proxmoxClient.getNextAvailableVmId()).thenReturn(100);
        when(proxmoxClient.cloneVm(anyInt(), anyInt(), anyString())).thenReturn("UPID:clone");
        when(proxmoxClient.startVm(anyInt())).thenReturn("UPID:start");
        when(proxmoxClient.waitForVmIp(anyInt(), any(Duration.class))).thenReturn("192.168.1.100");

        ConnectionResponse connectionResponse = new ConnectionResponse();
        connectionResponse.setIdentifier("conn1");
        when(guacamoleClient.createSpiceConnection(anyString(), anyString(), anyInt(), any()))
                .thenReturn(connectionResponse);
        when(guacamoleClient.generateClientUrl(anyString())).thenReturn("http://guac/#/client/conn1");

        // Act
        Desktop result = desktopService.createDesktop(userId, desktopName, plan);

        // Assert
        assertNotNull(result);
        assertEquals(DesktopStatus.RUNNING, result.getStatus());
        assertEquals(100, result.getVmId());
        assertEquals("192.168.1.100", result.getVmIpAddress());
        assertEquals("conn1", result.getConnectionId());

        verify(proxmoxClient).cloneVm(eq(9000), eq(100), anyString());
        verify(proxmoxClient).startVm(100);
        verify(guacamoleClient).createSpiceConnection(anyString(), eq("192.168.1.100"), anyInt(), any());
    }

    @Test
    void createDesktop_shouldFail_whenUserHasActiveDesktop() {
        when(desktopRepository.findByUserIdAndStatusNot(eq("user1"), eq(DesktopStatus.DELETED)))
                .thenReturn(Optional.of(new Desktop()));

        assertThrows(DesktopAlreadyExistsException.class,
                () -> desktopService.createDesktop("user1", "test", DesktopPlan.BASIC));
    }

    @Test
    void createDesktop_shouldCleanup_whenProvisioningFails() {
        // Arrange
        when(desktopRepository.findByUserIdAndStatusNot(anyString(), any())).thenReturn(Optional.empty());
        when(desktopRepository.save(any(Desktop.class))).thenAnswer(i -> {
            Desktop d = i.getArgument(0);
            d.setId(1L);
            return d;
        });
        when(proxmoxClient.getNextAvailableVmId()).thenReturn(100);
        when(proxmoxClient.cloneVm(anyInt(), anyInt(), anyString())).thenThrow(new RuntimeException("Clone failed"));

        // Act & Assert
        assertThrows(DesktopProvisioningException.class,
                () -> desktopService.createDesktop("user1", "test", DesktopPlan.BASIC));

        // Verify cleanup
        verify(proxmoxClient).deleteVm(100);

        ArgumentCaptor<Desktop> desktopCaptor = ArgumentCaptor.forClass(Desktop.class);
        verify(desktopRepository, atLeastOnce()).save(desktopCaptor.capture());
        assertEquals(DesktopStatus.FAILED, desktopCaptor.getValue().getStatus());
    }

    @Test
    void stopDesktop_shouldStopVm() {
        Desktop desktop = new Desktop();
        desktop.setId(1L);
        desktop.setVmId(100);
        desktop.setStatus(DesktopStatus.RUNNING);

        when(desktopRepository.findById(1L)).thenReturn(Optional.of(desktop));
        when(proxmoxClient.shutdownVm(100)).thenReturn("UPID:stop");

        desktopService.stopDesktop(1L, false);

        assertEquals(DesktopStatus.STOPPED, desktop.getStatus());
        verify(proxmoxClient).shutdownVm(100);
    }

    @Test
    void deleteDesktop_shouldCleanupResources() {
        Desktop desktop = new Desktop();
        desktop.setId(1L);
        desktop.setVmId(100);
        desktop.setConnectionId("conn1");
        desktop.setStatus(DesktopStatus.STOPPED);

        when(desktopRepository.findById(1L)).thenReturn(Optional.of(desktop));
        when(proxmoxClient.getVmStatus(100)).thenReturn(new VmStatus()); // default stopped

        desktopService.deleteDesktop(1L);

        assertEquals(DesktopStatus.DELETED, desktop.getStatus());
        verify(guacamoleClient).deleteConnection("conn1");
        verify(proxmoxClient).deleteVm(100);
    }
}

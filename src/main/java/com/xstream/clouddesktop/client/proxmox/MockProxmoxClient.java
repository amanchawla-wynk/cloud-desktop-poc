package com.xstream.clouddesktop.client.proxmox;

import com.xstream.clouddesktop.client.proxmox.dto.*;
import com.xstream.clouddesktop.client.proxmox.exception.*;
import com.xstream.clouddesktop.client.proxmox.mock.MockTask;
import com.xstream.clouddesktop.client.proxmox.mock.MockVm;
import com.xstream.clouddesktop.config.MockProperties;
import com.xstream.clouddesktop.config.ProxmoxProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Mock implementation of ProxmoxClient for POC demonstration without actual
 * infrastructure
 */
@Slf4j
@Component
@Profile("mock")
public class MockProxmoxClient extends ProxmoxClient {

    private final Map<Integer, MockVm> vms = new ConcurrentHashMap<>();
    private final Map<String, MockTask> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger vmIdCounter = new AtomicInteger(1000);
    private final AtomicInteger ipCounter = new AtomicInteger(100);
    private final MockProperties mockProperties;
    private final Random random = new Random();

    public MockProxmoxClient(ProxmoxProperties properties, MockProperties mockProperties) {
        super(null, properties); // No RestTemplate needed for mock
        this.mockProperties = mockProperties;
        log.info("MockProxmoxClient initialized - Running in DEMO MODE");
    }

    @Override
    public List<VmInfo> listVMs() {
        log.debug("Mock: Listing VMs - {} VMs in registry", vms.size());
        return vms.values().stream()
                .map(mockVm -> {
                    VmInfo vmInfo = new VmInfo();
                    vmInfo.setVmid(mockVm.getVmId());
                    vmInfo.setName(mockVm.getName());
                    vmInfo.setStatus(mockVm.getStatus());
                    vmInfo.setCpus(mockVm.getCpuCores());
                    vmInfo.setMaxmem((long) mockVm.getMemoryMb() * 1024 * 1024);
                    return vmInfo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public VmStatus getVmStatus(Integer vmId) {
        log.debug("Mock: Getting status for VM {}", vmId);
        MockVm mockVm = vms.get(vmId);
        if (mockVm == null) {
            throw new VmNotFoundException(vmId);
        }

        VmStatus status = new VmStatus();
        status.setVmid(vmId);
        status.setStatus(mockVm.getStatus());
        status.setCpus(mockVm.getCpuCores());
        status.setMaxmem((long) mockVm.getMemoryMb() * 1024 * 1024);
        status.setName(mockVm.getName());
        status.setQmpstatus(mockVm.getStatus());
        return status;
    }

    @Override
    public VmConfig getVmConfig(Integer vmId) {
        log.debug("Mock: Getting config for VM {}", vmId);
        MockVm mockVm = vms.get(vmId);
        if (mockVm == null) {
            throw new VmNotFoundException(vmId);
        }

        VmConfig config = new VmConfig();
        config.setCores(mockVm.getCpuCores());
        config.setMemory(mockVm.getMemoryMb().longValue());
        config.setName(mockVm.getName());
        return config;
    }

    @Override
    public List<VmNetworkInterface> getVmNetworkInterfaces(Integer vmId) {
        log.debug("Mock: Getting network interfaces for VM {}", vmId);
        MockVm mockVm = vms.get(vmId);
        if (mockVm == null) {
            throw new VmNotFoundException(vmId);
        }

        if (mockVm.getIpAddress() == null) {
            throw new GuestAgentNotAvailableException(vmId);
        }

        VmNetworkInterface iface = new VmNetworkInterface();
        iface.setName("eth0");
        iface.setHwaddr("00:11:22:33:44:55");

        IpAddress ipAddress = new IpAddress();
        ipAddress.setIpAddressType("ipv4");
        ipAddress.setIpAddress(mockVm.getIpAddress());
        ipAddress.setPrefix(24);

        iface.setIpAddresses(Collections.singletonList(ipAddress));

        return Collections.singletonList(iface);
    }

    @Override
    public String cloneVm(Integer templateId, Integer newVmId, String vmName) {
        log.info("Mock: Cloning VM from template {} to new VM {} with name '{}'", templateId, newVmId, vmName);

        // Create mock task
        String upid = "UPID:mock-node:clone:" + UUID.randomUUID().toString().substring(0, 8);
        long delay = getRandomDelay(mockProperties.getVm().getCloneDelayMin(),
                mockProperties.getVm().getCloneDelayMax());

        MockTask task = MockTask.builder()
                .upid(upid)
                .status("running")
                .exitStatus(null)
                .startTime(Instant.now())
                .completionTime(Instant.now().plusMillis(delay))
                .taskType("clone")
                .vmId(newVmId)
                .build();

        tasks.put(upid, task);

        // Schedule task completion
        scheduleTaskCompletion(upid, delay, () -> {
            // Create the VM when clone completes
            MockVm newVm = MockVm.builder()
                    .vmId(newVmId)
                    .name(vmName)
                    .status("stopped")
                    .ipAddress(null)
                    .cpuCores(2)
                    .memoryMb(2048)
                    .createdAt(Instant.now())
                    .templateId(templateId)
                    .build();
            vms.put(newVmId, newVm);
            log.info("Mock: VM {} cloned successfully", newVmId);
        });

        return upid;
    }

    @Override
    public String startVm(Integer vmId) {
        log.info("Mock: Starting VM {}", vmId);
        MockVm mockVm = vms.get(vmId);
        if (mockVm == null) {
            throw new VmNotFoundException(vmId);
        }

        String upid = "UPID:mock-node:start:" + UUID.randomUUID().toString().substring(0, 8);
        long delay = getRandomDelay(mockProperties.getVm().getStartDelayMin(),
                mockProperties.getVm().getStartDelayMax());

        MockTask task = MockTask.builder()
                .upid(upid)
                .status("running")
                .exitStatus(null)
                .startTime(Instant.now())
                .completionTime(Instant.now().plusMillis(delay))
                .taskType("start")
                .vmId(vmId)
                .build();

        tasks.put(upid, task);

        // Schedule task completion
        scheduleTaskCompletion(upid, delay, () -> {
            mockVm.setStatus("running");
            // Assign IP after a short delay
            long ipDelay = getRandomDelay(mockProperties.getVm().getIpDelayMin(),
                    mockProperties.getVm().getIpDelayMax());
            scheduleTaskCompletion("ip-" + vmId, ipDelay, () -> {
                String ip = "192.168.100." + ipCounter.getAndIncrement();
                mockVm.setIpAddress(ip);
                log.info("Mock: VM {} assigned IP {}", vmId, ip);
            });
            log.info("Mock: VM {} started successfully", vmId);
        });

        return upid;
    }

    @Override
    public String stopVm(Integer vmId) {
        return performStopAction(vmId, "stop", false);
    }

    @Override
    public String shutdownVm(Integer vmId) {
        return performStopAction(vmId, "shutdown", true);
    }

    private String performStopAction(Integer vmId, String action, boolean graceful) {
        log.info("Mock: {} VM {}", action, vmId);
        MockVm mockVm = vms.get(vmId);
        if (mockVm == null) {
            throw new VmNotFoundException(vmId);
        }

        String upid = "UPID:mock-node:" + action + ":" + UUID.randomUUID().toString().substring(0, 8);
        long delay = getRandomDelay(mockProperties.getVm().getStopDelayMin(),
                mockProperties.getVm().getStopDelayMax());

        MockTask task = MockTask.builder()
                .upid(upid)
                .status("running")
                .exitStatus(null)
                .startTime(Instant.now())
                .completionTime(Instant.now().plusMillis(delay))
                .taskType(action)
                .vmId(vmId)
                .build();

        tasks.put(upid, task);

        // Schedule task completion
        scheduleTaskCompletion(upid, delay, () -> {
            mockVm.setStatus("stopped");
            log.info("Mock: VM {} {} successfully", vmId, action);
        });

        return upid;
    }

    @Override
    public String deleteVm(Integer vmId) {
        log.info("Mock: Deleting VM {}", vmId);
        MockVm mockVm = vms.get(vmId);
        if (mockVm == null) {
            throw new VmNotFoundException(vmId);
        }

        vms.remove(vmId);
        log.info("Mock: VM {} deleted successfully", vmId);

        // Return null as delete is synchronous in mock
        return null;
    }

    @Override
    public TaskStatus getTaskStatus(String upid) {
        log.debug("Mock: Getting task status for {}", upid);
        MockTask mockTask = tasks.get(upid);
        if (mockTask == null) {
            throw new ProxmoxException("Task not found: " + upid);
        }

        // Check if task should be completed by now
        if (Instant.now().isAfter(mockTask.getCompletionTime()) && "running".equals(mockTask.getStatus())) {
            mockTask.setStatus("stopped");
            mockTask.setExitStatus("OK");
        }

        TaskStatus status = new TaskStatus();
        status.setStatus(mockTask.getStatus());
        status.setExitstatus(mockTask.getExitStatus());
        // Note: TaskStatus doesn't have upid field, it's tracked separately
        return status;
    }

    @Override
    public TaskStatus waitForTask(String upid, Duration timeout) {
        log.debug("Mock: Waiting for task {} with timeout {}", upid, timeout);
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
                Thread.sleep(1000); // Poll every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProxmoxException("Interrupted while waiting for task", e);
            }
        }
        throw new ProxmoxTaskTimeoutException("Timed out waiting for task " + upid);
    }

    @Override
    public Integer getNextAvailableVmId() {
        int nextId = vmIdCounter.getAndIncrement();
        log.debug("Mock: Generated next VM ID: {}", nextId);
        return nextId;
    }

    @Override
    public String waitForVmIp(Integer vmId, Duration timeout) {
        log.debug("Mock: Waiting for IP for VM {} with timeout {}", vmId, timeout);
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            MockVm mockVm = vms.get(vmId);
            if (mockVm != null && mockVm.getIpAddress() != null) {
                log.info("Mock: VM {} has IP {}", vmId, mockVm.getIpAddress());
                return mockVm.getIpAddress();
            }
            try {
                Thread.sleep(2000); // Poll every 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProxmoxException("Interrupted while waiting for IP", e);
            }
        }
        throw new ProxmoxTaskTimeoutException("Timed out waiting for IP address for VM " + vmId);
    }

    @Override
    public void checkProxmoxHealth() {
        log.debug("Mock: Proxmox health check - OK");
        // Always healthy in mock mode
    }

    // Helper methods

    private long getRandomDelay(long min, long max) {
        return min + (long) (random.nextDouble() * (max - min));
    }

    private void scheduleTaskCompletion(String taskId, long delayMs, Runnable action) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                action.run();

                // Update task status if it exists
                MockTask task = tasks.get(taskId);
                if (task != null) {
                    task.setStatus("stopped");
                    task.setExitStatus("OK");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Mock task {} interrupted", taskId, e);

                // Mark task as failed
                MockTask task = tasks.get(taskId);
                if (task != null) {
                    task.setStatus("stopped");
                    task.setExitStatus("ERROR");
                }
            }
        }, "mock-task-" + taskId).start();
    }
}

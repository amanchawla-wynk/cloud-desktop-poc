# Mock Infrastructure Setup Guide

## Overview

The Cloud Desktop POC includes a complete mock infrastructure that allows you to run and demonstrate the entire system **without requiring actual Proxmox or Guacamole servers**. This is perfect for:

- Local development and testing
- Demonstrations and POC presentations
- CI/CD pipeline testing
- Understanding the system without infrastructure setup

## Quick Start

### Running with Mock Infrastructure

```bash
cd /Users/b0279485/Desktop/cloud-desktop-poc

# Run with mock profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

The application will start on `http://localhost:8080` with **zero external dependencies**.

### Running with Real Infrastructure

```bash
# Run with default profile (requires Proxmox and Guacamole)
./mvnw spring-boot:run
```

## What Gets Mocked?

### 1. Proxmox VE Operations
- ✅ VM ID generation (sequential IDs starting from 1000)
- ✅ VM cloning from templates (30-45 second delay)
- ✅ VM start/stop operations (20-30 second delays)
- ✅ IP address assignment (192.168.100.x range)
- ✅ VM status checking
- ✅ Async task tracking with UPIDs
- ✅ Network interface queries

### 2. Guacamole Operations
- ✅ Authentication (mock tokens)
- ✅ Connection creation (SPICE/VNC/RDP)
- ✅ Connection URL generation
- ✅ Connection management (list/get/delete)

### 3. Realistic Timing

All operations simulate real-world delays:

| Operation | Delay Range | Purpose |
|-----------|-------------|---------|
| VM Clone | 30-45 seconds | Simulates template cloning |
| VM Start | 20-30 seconds | Simulates boot time |
| VM Stop | 10-15 seconds | Simulates shutdown |
| IP Assignment | 10-20 seconds | Simulates DHCP/network setup |
| Connection Setup | 5 seconds | Simulates Guacamole config |

## Configuration

### Mock Timing Configuration

Edit `src/main/resources/application-mock.yml` to adjust delays:

```yaml
mock:
  vm:
    clone-delay-min: 30000    # 30 seconds
    clone-delay-max: 45000    # 45 seconds
    start-delay-min: 20000    # 20 seconds
    start-delay-max: 30000    # 30 seconds
    stop-delay-min: 10000     # 10 seconds
    stop-delay-max: 15000     # 15 seconds
    ip-delay-min: 10000       # 10 seconds
    ip-delay-max: 20000       # 20 seconds
  connection:
    create-delay: 5000        # 5 seconds
```

### Faster Demo Mode

For faster demonstrations, reduce the delays:

```yaml
mock:
  vm:
    clone-delay-min: 5000     # 5 seconds
    clone-delay-max: 10000    # 10 seconds
    start-delay-min: 3000     # 3 seconds
    start-delay-max: 5000     # 5 seconds
```

## Testing the Mock Implementation

### 1. Start the Application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

Wait for the application to start (look for "Started CloudDesktopPocApplication").

### 2. Open Web Interface

Navigate to: `http://localhost:8080`

You should see a **DEMO MODE** banner indicating mock infrastructure is active.

### 3. Create a Desktop

1. Enter User ID: `demo-user-001`
2. Enter Desktop Name: `My Demo Desktop`
3. Select Plan: `STANDARD`
4. Click **Create Desktop**

**Expected Behavior**:
- Status changes: `PENDING` → `PROVISIONING` → `STARTING` → `WAITING_FOR_IP` → `CONFIGURING` → `RUNNING`
- Total time: ~60-90 seconds
- Desktop assigned IP: `192.168.100.100` (or next available)
- Connection URL generated

### 4. Test Lifecycle Operations

**Start Desktop** (if stopped):
- Click "Start Desktop"
- Wait 20-30 seconds
- Status changes to `RUNNING`

**Stop Desktop**:
- Click "Stop Desktop"
- Wait 10-15 seconds
- Status changes to `STOPPED`

**Delete Desktop**:
- Click "Delete Desktop"
- Confirm deletion
- Desktop removed immediately

### 5. Test Connection URL

1. With desktop in `RUNNING` state, click "Show Connection"
2. Connection URL will be displayed
3. Mock desktop viewer will load (shows demo desktop interface)

## API Testing with Mock Mode

### Using cURL

```bash
# Create desktop
curl -X POST http://localhost:8080/api/v1/desktops \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "api-test-user",
    "name": "API Test Desktop",
    "plan": "BASIC"
  }'

# Get desktop status (replace {id} with actual ID)
curl http://localhost:8080/api/v1/desktops/1/status?refresh=true

# Get connection URL
curl http://localhost:8080/api/v1/desktops/1/connect

# Stop desktop
curl -X POST http://localhost:8080/api/v1/desktops/1/stop \
  -H "Content-Type: application/json" \
  -d '{"force": false}'

# Delete desktop
curl -X DELETE http://localhost:8080/api/v1/desktops/1
```

### Using Postman

Import the API contracts from `API_CONTRACTS.md` and test all endpoints with `http://localhost:8080` as the base URL.

## How It Works

### Spring Profile Architecture

```
┌─────────────────────────────────────┐
│  Application Startup                │
└──────────────┬──────────────────────┘
               │
               ├─── Profile: default
               │    ├─ ProxmoxClient (@Profile("!mock"))
               │    ├─ GuacamoleClient (@Profile("!mock"))
               │    ├─ ProxmoxClientConfig
               │    └─ GuacamoleClientConfig
               │
               └─── Profile: mock
                    ├─ MockProxmoxClient (@Profile("mock"))
                    ├─ MockGuacamoleClient (@Profile("mock"))
                    └─ MockProperties (timing config)
```

### Mock Data Storage

All mock data is stored **in-memory only**:

- **VMs**: `ConcurrentHashMap<Integer, MockVm>`
- **Tasks**: `ConcurrentHashMap<String, MockTask>`
- **Connections**: `ConcurrentHashMap<String, ConnectionResponse>`

**Important**: Restarting the application clears all mock data.

### Thread Safety

Mock implementations use:
- `ConcurrentHashMap` for thread-safe storage
- `AtomicInteger` for ID generation
- Separate threads for async task simulation

## Troubleshooting

### Issue: Application won't start with mock profile

**Solution**: Check that `application-mock.yml` exists and is properly formatted.

```bash
# Verify file exists
ls -la src/main/resources/application-mock.yml

# Check for YAML syntax errors
cat src/main/resources/application-mock.yml
```

### Issue: Desktop creation hangs

**Solution**: Check application logs for errors. Mock operations run in background threads.

```bash
# Check logs for errors
tail -f logs/spring.log
```

### Issue: Status not updating

**Solution**: The UI auto-refreshes every 3 seconds during provisioning. Check browser console for errors.

### Issue: Connection URL doesn't work

**Solution**: In mock mode, connection URLs point to `/mock-desktop.html`. Ensure this file exists in `src/main/resources/static/`.

## Switching Between Mock and Real

### Environment Variable Method

```bash
# Set environment variable
export SPRING_PROFILES_ACTIVE=mock
./mvnw spring-boot:run

# Or inline
SPRING_PROFILES_ACTIVE=mock ./mvnw spring-boot:run
```

### Application Properties Method

Edit `src/main/resources/application.yml`:

```yaml
spring:
  profiles:
    active: mock  # Change to 'default' for real infrastructure
```

### Command Line Method (Recommended)

```bash
# Mock mode
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock

# Real mode
./mvnw spring-boot:run
```

## Demo Checklist

Use this checklist for demonstrations:

- [ ] Start application with mock profile
- [ ] Open `http://localhost:8080`
- [ ] Verify DEMO MODE banner is visible
- [ ] Create desktop with STANDARD plan
- [ ] Watch status progression (takes ~60-90 seconds)
- [ ] Show desktop details (IP, resources, timestamps)
- [ ] Click "Show Connection" to display mock desktop
- [ ] Stop desktop (takes ~10-15 seconds)
- [ ] Start desktop again (takes ~20-30 seconds)
- [ ] Delete desktop
- [ ] Create another desktop with different plan
- [ ] Demonstrate API using cURL or Postman

## Production Deployment

When ready to deploy with real infrastructure:

1. **Update Configuration**: Edit `application.yml` or `application-local.yml` with real Proxmox/Guacamole URLs
2. **Remove Mock Profile**: Run without `-Dspring-boot.run.profiles=mock`
3. **Verify Connectivity**: Check `/actuator/health` endpoint
4. **Test with Real Template**: Ensure Proxmox template VM exists

## Next Steps

- Review [API_CONTRACTS.md](file:///Users/b0279485/Desktop/cloud-desktop-poc/API_CONTRACTS.md) for complete API documentation
- Review [TECHNICAL_PRD.md](file:///Users/b0279485/Desktop/cloud-desktop-poc/TECHNICAL_PRD.md) for architecture details
- Set up real Proxmox and Guacamole infrastructure for production

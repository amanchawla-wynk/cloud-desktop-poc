# Cloud Desktop Service – API Contracts

## Introduction

**Desktop Creation API** – Enables on-demand provisioning of virtual desktop environments with configurable resource plans (Basic, Standard, Premium). Supports automated VM cloning, network configuration, and remote access setup.

**Desktop Lifecycle Management API** – Provides complete control over desktop instances including start, stop, and delete operations with real-time status tracking.

**Desktop Connection API** – Delivers browser-based access URLs for seamless desktop connectivity through Apache Guacamole without requiring client software installation.

**Desktop Status & Monitoring API** – Real-time status checking with optional refresh capability to sync with underlying infrastructure state.

---

## Use Cases

### Desktop Creation API
- **On-demand developer workspaces** with pre-configured development environments
- **Multi-tier resource allocation** based on user subscription levels (Basic/Standard/Premium)
- **Automated provisioning** for training sessions and educational labs
- **Isolated testing environments** for QA and security operations
- **Temporary workspaces** with configurable expiration policies

### Desktop Lifecycle Management API
- **Cost optimization** through automated stop/start of idle desktops
- **Resource management** by controlling desktop power states
- **Cleanup operations** to remove unused or expired desktop instances
- **Maintenance windows** with graceful shutdown capabilities

### Desktop Connection API
- **Zero-install access** via browser-based remote desktop connections
- **Embedded desktop experiences** within existing web applications
- **Protocol-agnostic connectivity** supporting SPICE and VNC
- **Session management** with connection URL generation

### Desktop Status & Monitoring API
- **Real-time provisioning tracking** during desktop creation workflow
- **Health monitoring** for running desktop instances
- **Infrastructure sync** with optional refresh from Proxmox VE
- **User-facing status updates** for desktop readiness

---

## Authentication Headers (Common for All APIs)

All API requests must include the following headers for authentication and identification:

| Header Name | Value Example | Description |
|-------------|---------------|-------------|
| `Authorization` | `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` | JWT token for user authentication |
| `Content-Type` | `application/json` | Request content type |
| `X-Request-ID` | `550e8400-e29b-41d4-a716-446655440000` | Optional unique request identifier for tracing |

> [!NOTE]
> In the current POC implementation, authentication is not enforced. Production deployment requires integration with an authentication provider (OAuth2, JWT, or SSO).

---

## 1 – Desktop Creation API

### Endpoint

```
POST /api/v1/desktops
```

### Request

**Content-Type:**
```
application/json
```

**Required Headers:**
```json
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "Content-Type": "application/json"
}
```

**Body Example:**
```json
{
  "userId": "user_12345",
  "name": "Development Workspace",
  "plan": "STANDARD"
}
```

### Request Fields

| Field | Type | Description | Required | Validation |
|-------|------|-------------|----------|------------|
| `userId` | String | Unique identifier for the user | Yes | Not blank |
| `name` | String | Display name for the desktop | No | Max 100 characters. Defaults to "Desktop-{userId}" |
| `plan` | String | Resource allocation tier | No | Must be `BASIC`, `STANDARD`, or `PREMIUM`. Defaults to `BASIC` |

### Desktop Plans

| Plan | vCPU | Memory | Description |
|------|------|--------|-------------|
| `BASIC` | 2 | 2 GB | Light productivity and browsing |
| `STANDARD` | 4 | 4 GB | Development and office applications |
| `PREMIUM` | 8 | 8 GB | Content creation and heavy workloads |

### Response

**Status:**
```
201 Created
```

**Body Example:**
```json
{
  "success": true,
  "message": "Desktop creation initiated successfully.",
  "data": {
    "id": 1,
    "userId": "user_12345",
    "name": "Development Workspace",
    "status": "PROVISIONING",
    "statusMessage": "Cloning VM from template",
    "plan": "STANDARD",
    "cpuCores": 4,
    "memoryMb": 4096,
    "ipAddress": null,
    "connectionUrl": null,
    "createdAt": "2024-11-28T10:30:00+05:30",
    "updatedAt": "2024-11-28T10:30:00+05:30",
    "lastAccessedAt": null
  },
  "timestamp": "2024-11-28T10:30:00+05:30"
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Indicates if the request was successful |
| `message` | String | Human-readable success message |
| `data` | Object | Desktop details (see Desktop Object below) |
| `timestamp` | ISO 8601 | Response generation timestamp |

### Desktop Object

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Unique desktop identifier |
| `userId` | String | Owner's user ID |
| `name` | String | Desktop display name |
| `status` | String | Current desktop status (see Status Definitions) |
| `statusMessage` | String | Detailed status description |
| `plan` | String | Resource plan tier |
| `cpuCores` | Integer | Allocated CPU cores |
| `memoryMb` | Integer | Allocated memory in MB |
| `ipAddress` | String | VM IP address (null until RUNNING) |
| `connectionUrl` | String | Guacamole connection URL (null until RUNNING) |
| `createdAt` | ISO 8601 | Desktop creation timestamp |
| `updatedAt` | ISO 8601 | Last update timestamp |
| `lastAccessedAt` | ISO 8601 | Last connection timestamp |

### Status Progression

During desktop creation, the status transitions through the following states:

```
PENDING → PROVISIONING → STARTING → WAITING_FOR_IP → CONFIGURING → RUNNING
```

> [!IMPORTANT]
> Desktop creation is an asynchronous operation. The initial response will have status `PENDING` or `PROVISIONING`. Use the Status API to poll for completion (status = `RUNNING`).

### Error Responses

**400 Bad Request** - Validation error:
```json
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "message": "Invalid request parameters",
  "details": {
    "userId": "User ID is required",
    "plan": "Plan must be one of BASIC, STANDARD, or PREMIUM"
  },
  "path": "/api/v1/desktops",
  "timestamp": "2024-11-28T10:30:00+05:30"
}
```

**409 Conflict** - User already has a desktop:
```json
{
  "success": false,
  "error": "DESKTOP_ALREADY_EXISTS",
  "message": "User already has an active desktop",
  "path": "/api/v1/desktops",
  "timestamp": "2024-11-28T10:30:00+05:30"
}
```

**500 Internal Server Error** - Infrastructure failure:
```json
{
  "success": false,
  "error": "PROVISIONING_FAILED",
  "message": "Failed to create desktop: Unable to clone VM from template",
  "path": "/api/v1/desktops",
  "timestamp": "2024-11-28T10:30:00+05:30"
}
```

---

## 2 – Desktop List API

### Endpoint

```
GET /api/v1/desktops
```

### Request

**Query Parameters:**

| Parameter | Type | Description | Required |
|-----------|------|-------------|----------|
| `userId` | String | Filter by user ID | No |
| `status` | String | Filter by desktop status | No |

**Example:**
```
GET /api/v1/desktops?userId=user_12345&status=RUNNING
```

### Response

**Status:**
```
200 OK
```

**Body Example:**
```json
{
  "success": true,
  "data": {
    "desktops": [
      {
        "id": 1,
        "userId": "user_12345",
        "name": "Development Workspace",
        "status": "RUNNING",
        "statusMessage": "Desktop is ready",
        "plan": "STANDARD",
        "cpuCores": 4,
        "memoryMb": 4096,
        "ipAddress": "192.168.1.100",
        "connectionUrl": "https://guacamole.example.com/#/client/c/connection-123",
        "createdAt": "2024-11-28T10:30:00+05:30",
        "updatedAt": "2024-11-28T10:35:00+05:30",
        "lastAccessedAt": "2024-11-28T11:00:00+05:30"
      }
    ],
    "total": 1
  },
  "timestamp": "2024-11-28T11:15:00+05:30"
}
```

---

## 3 – Desktop Details API

### Endpoint

```
GET /api/v1/desktops/{id}
```

### Request

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Desktop ID |

**Example:**
```
GET /api/v1/desktops/1
```

### Response

**Status:**
```
200 OK
```

**Body Example:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user_12345",
    "name": "Development Workspace",
    "status": "RUNNING",
    "statusMessage": "Desktop is ready",
    "plan": "STANDARD",
    "cpuCores": 4,
    "memoryMb": 4096,
    "ipAddress": "192.168.1.100",
    "connectionUrl": "https://guacamole.example.com/#/client/c/connection-123",
    "createdAt": "2024-11-28T10:30:00+05:30",
    "updatedAt": "2024-11-28T10:35:00+05:30",
    "lastAccessedAt": "2024-11-28T11:00:00+05:30"
  },
  "timestamp": "2024-11-28T11:15:00+05:30"
}
```

### Error Responses

**404 Not Found** - Desktop does not exist:
```json
{
  "success": false,
  "error": "DESKTOP_NOT_FOUND",
  "message": "Desktop with ID 1 not found",
  "path": "/api/v1/desktops/1",
  "timestamp": "2024-11-28T11:15:00+05:30"
}
```

---

## 4 – Get Desktop by User ID API

### Endpoint

```
GET /api/v1/desktops/user/{userId}
```

### Request

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `userId` | String | User ID |

**Example:**
```
GET /api/v1/desktops/user/user_12345
```

### Response

**Status:**
```
200 OK
```

**Body Example:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user_12345",
    "name": "Development Workspace",
    "status": "RUNNING",
    "statusMessage": "Desktop is ready",
    "plan": "STANDARD",
    "cpuCores": 4,
    "memoryMb": 4096,
    "ipAddress": "192.168.1.100",
    "connectionUrl": "https://guacamole.example.com/#/client/c/connection-123",
    "createdAt": "2024-11-28T10:30:00+05:30",
    "updatedAt": "2024-11-28T10:35:00+05:30",
    "lastAccessedAt": "2024-11-28T11:00:00+05:30"
  },
  "timestamp": "2024-11-28T11:15:00+05:30"
}
```

> [!NOTE]
> This endpoint returns the user's active desktop. Currently, the system supports one desktop per user.

---

## 5 – Desktop Status API

### Endpoint

```
GET /api/v1/desktops/{id}/status
```

### Request

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Desktop ID |

**Query Parameters:**

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `refresh` | Boolean | Force refresh from Proxmox VE | `false` |

**Example:**
```
GET /api/v1/desktops/1/status?refresh=true
```

### Response

**Status:**
```
200 OK
```

**Body Example:**
```json
{
  "success": true,
  "data": {
    "desktopId": 1,
    "status": "RUNNING",
    "statusMessage": "Desktop is ready and accessible",
    "ready": true
  },
  "timestamp": "2024-11-28T11:15:00+05:30"
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `desktopId` | Long | Desktop identifier |
| `status` | String | Current status (see Status Definitions) |
| `statusMessage` | String | Human-readable status description |
| `ready` | Boolean | `true` if status is `RUNNING`, `false` otherwise |

> [!TIP]
> Use `refresh=true` to get real-time status from Proxmox VE. This is useful for monitoring desktop provisioning progress but may increase response time.

---

## 6 – Desktop Connection API

### Endpoint

```
GET /api/v1/desktops/{id}/connect
```

### Request

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Desktop ID |

**Example:**
```
GET /api/v1/desktops/1/connect
```

### Response

**Status:**
```
200 OK
```

**Body Example:**
```json
{
  "success": true,
  "data": {
    "desktopId": 1,
    "connectionUrl": "https://guacamole.example.com/#/client/c/connection-123",
    "protocol": "spice",
    "expiresAt": null
  },
  "timestamp": "2024-11-28T11:15:00+05:30"
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `desktopId` | Long | Desktop identifier |
| `connectionUrl` | String | Full Guacamole connection URL |
| `protocol` | String | Remote desktop protocol (`spice` or `vnc`) |
| `expiresAt` | ISO 8601 | Connection expiration time (null if no expiry) |

### Usage

The `connectionUrl` can be used in multiple ways:

**Embedded in iframe:**
```html
<iframe 
  src="https://guacamole.example.com/#/client/c/connection-123" 
  width="100%" 
  height="800px"
  allow="clipboard-read; clipboard-write">
</iframe>
```

**New browser tab:**
```javascript
window.open(connectionUrl, '_blank');
```

### Error Responses

**400 Bad Request** - Desktop not ready:
```json
{
  "success": false,
  "error": "DESKTOP_NOT_READY",
  "message": "Desktop is not in RUNNING state. Current status: PROVISIONING",
  "path": "/api/v1/desktops/1/connect",
  "timestamp": "2024-11-28T11:15:00+05:30"
}
```

---

## 7 – Start Desktop API

### Endpoint

```
POST /api/v1/desktops/{id}/start
```

### Request

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Desktop ID |

**Body:** (Optional, can be empty)
```json
{}
```

**Example:**
```
POST /api/v1/desktops/1/start
```

### Response

**Status:**
```
200 OK
```

**Body Example:**
```json
{
  "success": true,
  "message": "Desktop started successfully.",
  "data": {
    "id": 1,
    "userId": "user_12345",
    "name": "Development Workspace",
    "status": "STARTING",
    "statusMessage": "VM is booting up",
    "plan": "STANDARD",
    "cpuCores": 4,
    "memoryMb": 4096,
    "ipAddress": "192.168.1.100",
    "connectionUrl": null,
    "createdAt": "2024-11-28T10:30:00+05:30",
    "updatedAt": "2024-11-28T12:00:00+05:30",
    "lastAccessedAt": "2024-11-28T11:00:00+05:30"
  },
  "timestamp": "2024-11-28T12:00:00+05:30"
}
```

> [!IMPORTANT]
> Starting a desktop is asynchronous. The status will transition from `STOPPED` → `STARTING` → `RUNNING`. Poll the Status API to check when the desktop is ready.

### Error Responses

**400 Bad Request** - Invalid state:
```json
{
  "success": false,
  "error": "INVALID_OPERATION",
  "message": "Cannot start desktop in RUNNING state",
  "path": "/api/v1/desktops/1/start",
  "timestamp": "2024-11-28T12:00:00+05:30"
}
```

---

## 8 – Stop Desktop API

### Endpoint

```
POST /api/v1/desktops/{id}/stop
```

### Request

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Desktop ID |

**Body:** (Optional)
```json
{
  "force": false
}
```

### Request Fields

| Field | Type | Description | Default |
|-------|------|-------------|---------|
| `force` | Boolean | Force immediate shutdown without graceful shutdown | `false` |

**Example:**
```
POST /api/v1/desktops/1/stop
Content-Type: application/json

{
  "force": true
}
```

### Response

**Status:**
```
200 OK
```

**Body Example:**
```json
{
  "success": true,
  "message": "Desktop stopped successfully.",
  "data": {
    "id": 1,
    "userId": "user_12345",
    "name": "Development Workspace",
    "status": "STOPPING",
    "statusMessage": "Shutting down VM",
    "plan": "STANDARD",
    "cpuCores": 4,
    "memoryMb": 4096,
    "ipAddress": "192.168.1.100",
    "connectionUrl": null,
    "createdAt": "2024-11-28T10:30:00+05:30",
    "updatedAt": "2024-11-28T13:00:00+05:30",
    "lastAccessedAt": "2024-11-28T11:00:00+05:30"
  },
  "timestamp": "2024-11-28T13:00:00+05:30"
}
```

> [!WARNING]
> Using `force: true` performs an immediate power-off without graceful shutdown. This may result in data loss or file system corruption. Use only when necessary.

---

## 9 – Delete Desktop API

### Endpoint

```
DELETE /api/v1/desktops/{id}
```

### Request

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | Long | Desktop ID |

**Example:**
```
DELETE /api/v1/desktops/1
```

### Response

**Status:**
```
200 OK
```

**Body Example:**
```json
{
  "success": true,
  "message": "Desktop deleted successfully.",
  "data": null,
  "timestamp": "2024-11-28T14:00:00+05:30"
}
```

> [!CAUTION]
> Deleting a desktop is irreversible. The VM and all associated data will be permanently removed from Proxmox VE. The Guacamole connection will also be deleted.

### Error Responses

**404 Not Found** - Desktop does not exist:
```json
{
  "success": false,
  "error": "DESKTOP_NOT_FOUND",
  "message": "Desktop with ID 1 not found",
  "path": "/api/v1/desktops/1",
  "timestamp": "2024-11-28T14:00:00+05:30"
}
```

---

## Status Definitions

| Status | Description | User Action | Typical Duration |
|--------|-------------|-------------|------------------|
| `PENDING` | Desktop creation initiated | Wait | < 5 seconds |
| `PROVISIONING` | VM being cloned from template | Wait | 30-60 seconds |
| `STARTING` | VM is booting up | Wait | 20-40 seconds |
| `WAITING_FOR_IP` | Waiting for network configuration | Wait | 10-30 seconds |
| `CONFIGURING` | Setting up Guacamole connection | Wait | 5-10 seconds |
| `RUNNING` | Desktop ready for use | Connect | N/A |
| `STOPPING` | Desktop shutting down | Wait | 10-20 seconds |
| `STOPPED` | Desktop powered off | Start or Delete | N/A |
| `DELETING` | Desktop being removed | Wait | 10-20 seconds |
| `DELETED` | Desktop removed (soft delete) | Create new | N/A |
| `FAILED` | Error occurred | Check logs, Delete | N/A |

---

## Error Handling

### Standard Error Response Format

All error responses follow this structure:

```json
{
  "success": false,
  "error": "ERROR_CODE",
  "message": "Human-readable error description",
  "details": {
    "field1": "Validation error message",
    "field2": "Another validation error"
  },
  "path": "/api/v1/desktops",
  "timestamp": "2024-11-28T10:30:00+05:30"
}
```

### Common Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Request validation failed |
| 400 | `INVALID_OPERATION` | Operation not allowed in current state |
| 400 | `DESKTOP_NOT_READY` | Desktop not in RUNNING state |
| 404 | `DESKTOP_NOT_FOUND` | Desktop does not exist |
| 409 | `DESKTOP_ALREADY_EXISTS` | User already has an active desktop |
| 500 | `PROVISIONING_FAILED` | VM creation or cloning failed |
| 500 | `INFRASTRUCTURE_ERROR` | Proxmox or Guacamole communication error |
| 500 | `INTERNAL_SERVER_ERROR` | Unexpected server error |

### Retry Guidelines

| Error Type | Retry Recommended | Strategy |
|------------|-------------------|----------|
| `VALIDATION_ERROR` | No | Fix request parameters |
| `DESKTOP_NOT_FOUND` | No | Verify desktop ID |
| `DESKTOP_ALREADY_EXISTS` | No | Use existing desktop or delete first |
| `DESKTOP_NOT_READY` | Yes | Poll status API until RUNNING |
| `PROVISIONING_FAILED` | Yes | Retry after 30 seconds (max 3 attempts) |
| `INFRASTRUCTURE_ERROR` | Yes | Exponential backoff (1s, 2s, 4s, 8s) |
| `INTERNAL_SERVER_ERROR` | Yes | Retry after 5 seconds (max 2 attempts) |

---

## Security

### Authentication & Authorization

**Current POC Implementation:**
- No authentication enforced
- All endpoints are publicly accessible

**Production Requirements:**

1. **JWT-based Authentication**
   - All requests must include valid JWT token in `Authorization` header
   - Token validation against authentication service
   - Token expiration and refresh mechanism

2. **User Authorization**
   - Users can only access their own desktops
   - Admin users can access all desktops
   - Role-based access control (RBAC) for operations

3. **API Rate Limiting**
   - Desktop creation: 5 requests per hour per user
   - Other operations: 100 requests per minute per user
   - Burst protection with token bucket algorithm

### Data Security

**Encryption:**
- All API communication over HTTPS/TLS 1.3
- Desktop connection URLs transmitted securely
- Sensitive data encrypted at rest in database

**Network Security:**
- Proxmox VE accessible only from application server
- Guacamole connections proxied through secure gateway
- VM network isolation with VLANs
- Firewall rules restricting VM-to-VM communication

**Audit Logging:**
- All API requests logged with user ID and timestamp
- Desktop lifecycle events tracked
- Failed authentication attempts monitored
- Suspicious activity alerts

### Compliance Considerations

- **Data Residency**: Deploy infrastructure in required geographic regions
- **Access Controls**: Implement RBAC for desktop management
- **Audit Trails**: Comprehensive logging of all operations
- **Data Retention**: Configurable desktop expiration policies
- **Encryption**: At-rest and in-transit encryption for all data

---

## Performance Characteristics

### Response Times (Target)

| Operation | Target (p95) | Notes |
|-----------|--------------|-------|
| Create Desktop | < 2 minutes | Asynchronous, includes full provisioning |
| List Desktops | < 200ms | Cached from database |
| Get Desktop Details | < 100ms | Database query |
| Get Status | < 150ms | Database query |
| Get Status (refresh) | < 500ms | Includes Proxmox API call |
| Get Connection | < 100ms | Database query |
| Start Desktop | < 45 seconds | Asynchronous VM boot |
| Stop Desktop | < 20 seconds | Asynchronous shutdown |
| Delete Desktop | < 30 seconds | Asynchronous cleanup |

### Scalability

**Concurrent Operations:**
- Desktop creation: 10 concurrent operations per Proxmox node
- API requests: 1000 requests per second
- Active connections: 100+ concurrent desktop sessions

**Resource Limits:**
- Max desktops per user: 1 (current), configurable
- Max concurrent desktops per plan tier: Based on infrastructure capacity
- Template storage: 50GB per template
- Desktop storage: 50GB per instance (thin provisioned)

---

## API Versioning

**Current Version:** `v1`

**Endpoint Pattern:**
```
/api/v1/{resource}
```

**Version Strategy:**
- Major version in URL path (`/api/v1/`, `/api/v2/`)
- Backward compatibility maintained within major versions
- Deprecation notices provided 6 months before removal
- Multiple versions supported simultaneously during transition

**Future Versions:**
- `v2`: Planned features include batch operations, desktop templates, snapshots

---

## Appendix

### Complete API Endpoint Summary

| Endpoint | Method | Description | Async |
|----------|--------|-------------|-------|
| `/api/v1/desktops` | POST | Create new desktop | Yes |
| `/api/v1/desktops` | GET | List desktops with filters | No |
| `/api/v1/desktops/{id}` | GET | Get desktop details | No |
| `/api/v1/desktops/user/{userId}` | GET | Get user's desktop | No |
| `/api/v1/desktops/{id}/status` | GET | Get desktop status | No |
| `/api/v1/desktops/{id}/connect` | GET | Get connection URL | No |
| `/api/v1/desktops/{id}/start` | POST | Start desktop | Yes |
| `/api/v1/desktops/{id}/stop` | POST | Stop desktop | Yes |
| `/api/v1/desktops/{id}` | DELETE | Delete desktop | Yes |

### Sample Integration Code

**JavaScript/TypeScript:**
```javascript
// Create desktop
const createDesktop = async (userId, name, plan) => {
  const response = await fetch('https://api.example.com/api/v1/desktops', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ userId, name, plan })
  });
  return await response.json();
};

// Poll for desktop readiness
const waitForDesktop = async (desktopId, maxAttempts = 30) => {
  for (let i = 0; i < maxAttempts; i++) {
    const response = await fetch(
      `https://api.example.com/api/v1/desktops/${desktopId}/status?refresh=true`,
      { headers: { 'Authorization': `Bearer ${token}` } }
    );
    const { data } = await response.json();
    
    if (data.ready) return data;
    if (data.status === 'FAILED') throw new Error('Desktop provisioning failed');
    
    await new Promise(resolve => setTimeout(resolve, 5000)); // Wait 5 seconds
  }
  throw new Error('Desktop provisioning timeout');
};

// Connect to desktop
const connectToDesktop = async (desktopId) => {
  const response = await fetch(
    `https://api.example.com/api/v1/desktops/${desktopId}/connect`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  );
  const { data } = await response.json();
  window.open(data.connectionUrl, '_blank');
};
```

**Python:**
```python
import requests
import time

class DesktopClient:
    def __init__(self, base_url, token):
        self.base_url = base_url
        self.headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }
    
    def create_desktop(self, user_id, name, plan='BASIC'):
        response = requests.post(
            f'{self.base_url}/api/v1/desktops',
            headers=self.headers,
            json={'userId': user_id, 'name': name, 'plan': plan}
        )
        return response.json()
    
    def wait_for_desktop(self, desktop_id, max_attempts=30):
        for _ in range(max_attempts):
            response = requests.get(
                f'{self.base_url}/api/v1/desktops/{desktop_id}/status',
                headers=self.headers,
                params={'refresh': 'true'}
            )
            data = response.json()['data']
            
            if data['ready']:
                return data
            if data['status'] == 'FAILED':
                raise Exception('Desktop provisioning failed')
            
            time.sleep(5)
        
        raise Exception('Desktop provisioning timeout')
    
    def get_connection_url(self, desktop_id):
        response = requests.get(
            f'{self.base_url}/api/v1/desktops/{desktop_id}/connect',
            headers=self.headers
        )
        return response.json()['data']['connectionUrl']
```

### Glossary

- **DaaS**: Desktop as a Service - cloud-based virtual desktop delivery
- **Proxmox VE**: Open-source virtualization management platform
- **Guacamole**: Clientless remote desktop gateway (browser-based)
- **SPICE**: Simple Protocol for Independent Computing Environments - remote display protocol
- **VNC**: Virtual Network Computing - remote desktop protocol
- **Template VM**: Pre-configured virtual machine used for cloning
- **Clone**: Creating a new VM from an existing template
- **Guest Agent**: Software running inside VM for enhanced management
- **JWT**: JSON Web Token - authentication token format
- **QPS**: Queries Per Second - measure of request throughput

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-12-01 | Technical Team | Initial API contracts documentation |

**Review Status:** Draft - Ready for Team Review  
**Next Review Date:** TBD  
**Related Documents:** 
- [Technical PRD](TECHNICAL_PRD.md)
- [README](README.md)

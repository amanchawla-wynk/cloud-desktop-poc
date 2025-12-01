# Cloud Desktop POC

A Spring Boot application for **Cloud Desktop as a Service (DaaS)** - enabling on-demand provisioning of virtual desktop environments accessible through web browsers. Integrates with Proxmox VE for VM management and Apache Guacamole for remote desktop access.

> **🎭 Demo Mode Available**: Run the entire POC **without any infrastructure** using our mock implementation!

## 📋 Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [Running with Mock Infrastructure](#running-with-mock-infrastructure)
- [Project Information](#project-information)
- [Documentation](#documentation)
- [API Endpoints](#api-endpoints)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Development](#development)

## ✨ Features

- ✅ **Desktop Lifecycle Management**: Create, start, stop, and delete virtual desktops
- ✅ **Multi-Tier Plans**: Basic (2 vCPU, 2GB), Standard (4 vCPU, 4GB), Premium (8 vCPU, 8GB)
- ✅ **Browser-Based Access**: Zero-install desktop access via Apache Guacamole
- ✅ **Real-Time Status Tracking**: Monitor provisioning progress through multiple states
- ✅ **RESTful API**: Complete REST API with comprehensive error handling
- ✅ **Mock Infrastructure**: Full POC demonstration without Proxmox/Guacamole
- ✅ **Web UI**: Modern, responsive test interface with real-time updates
- ✅ **Async Operations**: Non-blocking desktop provisioning with status polling

## 🚀 Quick Start

### Prerequisites

- **Java 17 JDK** (required)
- Maven NOT required (project includes Maven wrapper)

**Install Java 17 (macOS)**:
```bash
brew install openjdk@17
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc
java -version  # Verify: should show 17.x.x
```

### Run with Mock Infrastructure (Recommended for POC)

```bash
# Clone the repository
cd cloud-desktop-poc

# Run with mock profile (NO infrastructure required!)
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

**Access the application**:
- Web UI: http://localhost:8080
- API: http://localhost:8080/api/v1/desktops
- Health: http://localhost:8080/actuator/health

### Run with Real Infrastructure

```bash
# Update configuration in application-local.yml first
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## 🎭 Running with Mock Infrastructure

The mock implementation provides a **complete, realistic simulation** of the entire desktop lifecycle without requiring Proxmox or Guacamole servers.

### What Gets Mocked?

- ✅ VM provisioning with realistic delays (30-45 seconds for cloning)
- ✅ VM lifecycle operations (start, stop, delete)
- ✅ IP address assignment (192.168.100.x range)
- ✅ Guacamole connection management
- ✅ Async task tracking with UPIDs
- ✅ Status transitions through all states

### Quick Demo

1. **Start the application**:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
   ```

2. **Open browser**: http://localhost:8080

3. **Create a desktop**:
   - User ID: `demo-user`
   - Name: `My Demo Desktop`
   - Plan: `STANDARD`
   - Click "Create Desktop"

4. **Watch the magic**: Status will progress through:
   ```
   PENDING → PROVISIONING → STARTING → WAITING_FOR_IP → CONFIGURING → RUNNING
   ```
   (Takes ~60-90 seconds with realistic delays)

5. **Test lifecycle**:
   - Click "Show Connection" to see mock desktop viewer
   - Click "Stop Desktop" to shutdown
   - Click "Start Desktop" to restart
   - Click "Delete Desktop" to remove

### Adjusting Mock Timing

Edit `src/main/resources/application-mock.yml` to change delays:

```yaml
mock:
  vm:
    clone-delay-min: 5000   # Faster demo (5 seconds)
    clone-delay-max: 10000  # Instead of 30-45 seconds
```

**See [MOCK_SETUP_GUIDE.md](MOCK_SETUP_GUIDE.md) for complete documentation.**

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [API_CONTRACTS.md](API_CONTRACTS.md) | Complete API documentation with examples |
| [TECHNICAL_PRD.md](TECHNICAL_PRD.md) | Technical architecture and integration guide |
| [MOCK_SETUP_GUIDE.md](MOCK_SETUP_GUIDE.md) | Mock infrastructure setup and troubleshooting |

## 🔌 API Endpoints

### Desktop Management

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/desktops` | POST | Create new desktop |
| `/api/v1/desktops` | GET | List all desktops (with filters) |
| `/api/v1/desktops/{id}` | GET | Get desktop details |
| `/api/v1/desktops/{id}/status` | GET | Get desktop status |
| `/api/v1/desktops/{id}/connect` | GET | Get connection URL |
| `/api/v1/desktops/{id}/start` | POST | Start desktop |
| `/api/v1/desktops/{id}/stop` | POST | Stop desktop |
| `/api/v1/desktops/{id}` | DELETE | Delete desktop |
| `/api/v1/desktops/user/{userId}` | GET | Get user's desktop |

### Example: Create Desktop

```bash
curl -X POST http://localhost:8080/api/v1/desktops \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "name": "My Desktop",
    "plan": "STANDARD"
  }'
```

**Response**:
```json
{
  "success": true,
  "message": "Desktop creation initiated successfully.",
  "data": {
    "id": 1,
    "userId": "user123",
    "name": "My Desktop",
    "status": "PROVISIONING",
    "plan": "STANDARD",
    "cpuCores": 4,
    "memoryMb": 4096
  }
}
```

**See [API_CONTRACTS.md](API_CONTRACTS.md) for complete API documentation.**

## 📁 Project Structure

```
cloud-desktop-poc/
├── src/main/java/com/xstream/clouddesktop/
│   ├── CloudDesktopApplication.java       # Main application
│   ├── client/
│   │   ├── proxmox/
│   │   │   ├── ProxmoxClient.java         # Real Proxmox client
│   │   │   ├── MockProxmoxClient.java     # Mock implementation
│   │   │   ├── ProxmoxClientConfig.java   # HTTP client config
│   │   │   ├── dto/                       # Proxmox DTOs
│   │   │   ├── exception/                 # Proxmox exceptions
│   │   │   └── mock/                      # Mock data structures
│   │   └── guacamole/
│   │       ├── GuacamoleClient.java       # Real Guacamole client
│   │       ├── MockGuacamoleClient.java   # Mock implementation
│   │       ├── GuacamoleClientConfig.java # HTTP client config
│   │       ├── dto/                       # Guacamole DTOs
│   │       └── exception/                 # Guacamole exceptions
│   ├── config/
│   │   ├── ProxmoxProperties.java         # Proxmox configuration
│   │   ├── GuacamoleProperties.java       # Guacamole configuration
│   │   └── MockProperties.java            # Mock timing configuration
│   ├── controller/
│   │   ├── DesktopController.java         # Desktop REST API
│   │   ├── HealthController.java          # Health endpoints
│   │   ├── DiagnosticsController.java     # Diagnostics
│   │   └── advice/                        # Global exception handling
│   ├── service/
│   │   ├── DesktopService.java            # Business logic
│   │   └── exception/                     # Service exceptions
│   ├── repository/
│   │   └── DesktopRepository.java         # JPA repository
│   ├── model/
│   │   ├── Desktop.java                   # Desktop entity
│   │   ├── DesktopStatus.java             # Status enum
│   │   └── DesktopPlan.java               # Plan enum
│   └── dto/
│       ├── request/                       # API request DTOs
│       └── response/                      # API response DTOs
├── src/main/resources/
│   ├── application.yml                    # Main configuration
│   ├── application-local.yml              # Local development config
│   ├── application-mock.yml               # Mock mode config
│   └── static/
│       ├── index.html                     # Main web UI
│       ├── mock-desktop.html              # Mock desktop viewer
│       └── diagnostics.html               # Diagnostics page
├── API_CONTRACTS.md                       # API documentation
├── TECHNICAL_PRD.md                       # Technical PRD
├── MOCK_SETUP_GUIDE.md                    # Mock setup guide
└── README.md                              # This file
```

## ⚙️ Configuration

### Profile-Based Configuration

The application supports multiple profiles:

| Profile | Purpose | Command |
|---------|---------|---------|
| **mock** | Demo mode (no infrastructure) | `./mvnw spring-boot:run -Dspring-boot.run.profiles=mock` |
| **local** | Local development with real infrastructure | `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` |
| **default** | Production configuration | `./mvnw spring-boot:run` |

### Configuring Real Infrastructure

Update `src/main/resources/application-local.yml`:

```yaml
proxmox:
  url: https://your-proxmox-server:8006
  node: pve
  username: root@pam
  token-id: your-token-id
  token-secret: your-token-secret
  template-vm-id: 9000

guacamole:
  url: http://your-guacamole-server:8080/guacamole
  username: guacadmin
  password: your-password
  data-source: mysql
```

## 🛠️ Development

### Build and Test

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock

# Package as JAR
./mvnw clean package
```

### Database Access

**H2 Console** (in-memory database):
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:clouddesktop`
- Username: `sa`
- Password: (empty)

### Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: H2 (POC) / PostgreSQL (Production)
- **Build Tool**: Maven 3.9+
- **Dependencies**:
  - Spring Web
  - Spring Data JPA
  - Spring Validation
  - Spring Actuator
  - Apache HttpClient 5
  - Lombok
  - H2 Database

## 📊 Project Status

- ✅ **Phase 1**: Project foundation and setup
- ✅ **Phase 2**: Proxmox VE client implementation
- ✅ **Phase 3**: Apache Guacamole client implementation
- ✅ **Phase 4**: Domain models and business services
- ✅ **Phase 5**: REST API endpoints
- ✅ **Phase 6**: Mock infrastructure for POC
- ✅ **Phase 7**: Web UI and documentation

**Current Status**: ✅ **Complete POC - Ready for Demo**

## 🐛 Troubleshooting

### Application won't start

**Check Java version**:
```bash
java -version  # Must be 17.x.x
```

**Port 8080 in use**:
```bash
lsof -i :8080  # Find process using port
kill -9 <PID>  # Kill the process
```

### Mock mode not working

**Ensure you're using the mock profile**:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

**Check logs for errors**:
```bash
# Look for "MockProxmoxClient initialized" in logs
```

### Desktop creation fails

**With mock mode**: Should never fail - check application logs

**With real infrastructure**:
- Verify Proxmox server is accessible
- Check API token permissions
- Ensure template VM exists
- Verify Guacamole server is running

**See [MOCK_SETUP_GUIDE.md](MOCK_SETUP_GUIDE.md) for detailed troubleshooting.**

## 📝 License

This is a proof of concept project.

## 👥 Contact

For questions or issues, please contact the development team.

---

**Made with ❤️ using Spring Boot**

# Cloud Desktop POC - README

A Spring Boot application for Cloud Desktop (Desktop-as-a-Service) proof of concept, designed to integrate with Proxmox VE for virtual machine management and Apache Guacamole for remote desktop access.

## Project Information

- **Group ID**: com.xstream
- **Artifact ID**: cloud-desktop-poc
- **Version**: 0.0.1-SNAPSHOT
- **Java Version**: 17
- **Spring Boot Version**: 3.2.0

## Prerequisites

- **Java 17 JDK** (required for compilation and running)
- Maven is NOT required (project includes Maven wrapper)

### Installing Java 17 JDK

**Using Homebrew (Recommended for macOS)**:
```bash
brew install openjdk@17
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc
```

**Alternative**: Download from [Oracle](https://www.oracle.com/java/technologies/downloads/#java17) or [Adoptium](https://adoptium.net/temurin/releases/?version=17)

Verify installation:
```bash
java -version   # Should show version 17.x.x
javac -version  # Should show version 17.x.x
```

## Quick Start

### 1. Compile the Project
```bash
./mvnw clean compile
```

### 2. Run Tests
```bash
./mvnw test
```

### 3. Start the Application
```bash
./mvnw spring-boot:run
```

The application will start on **http://localhost:8080**

### 4. Verify the Application

**Custom Health Endpoint**:
```bash
curl http://localhost:8080/api/health
```

**Actuator Health Endpoint**:
```bash
curl http://localhost:8080/actuator/health
```

**H2 Database Console**: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:clouddesktop`
- Username: `sa`
- Password: (empty)

## Configuration

### Using Local Profile

For local development with custom configuration:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Configuration Files

- **application.yml**: Main configuration with default values
- **application-local.yml**: Local development overrides (update with your values)

### Required Configuration

Before connecting to real Proxmox and Guacamole instances, update `application-local.yml`:

**Proxmox Configuration**:
- Create an API token in Proxmox UI: Datacenter → Permissions → API Tokens
- Update `proxmox.url`, `proxmox.token-id`, and `proxmox.token-secret`
- Set your VM template ID in `proxmox.template-vm-id`

**Guacamole Configuration**:
- Update `guacamole.url` with your Guacamole server address
- Change default credentials in `guacamole.username` and `guacamole.password`

## Project Structure

```
src/main/java/com/xstream/clouddesktop/
├── CloudDesktopApplication.java      # Main application class
├── config/
│   ├── ProxmoxProperties.java        # Proxmox configuration
│   └── GuacamoleProperties.java      # Guacamole configuration
├── controller/
│   └── HealthController.java         # Custom health endpoint
├── client/
│   ├── proxmox/                      # Proxmox client (Phase 2)
│   └── guacamole/                    # Guacamole client (Phase 3)
├── service/                          # Business logic (Phase 4)
├── repository/                       # Data access (Phase 4)
├── model/                            # Domain models (Phase 4)
└── dto/                              # Data Transfer Objects (Phase 5)
```

## Dependencies

- **Spring Web**: REST API development
- **Spring Data JPA**: Database access
- **H2 Database**: In-memory database for POC
- **Lombok**: Reduce boilerplate code
- **Spring Validation**: Request validation
- **Spring Actuator**: Health checks and monitoring

## API Endpoints

### Custom Health Check
- **GET** `/api/health`
- Returns: `{ "status": "UP", "timestamp": "...", "application": "cloud-desktop-poc" }`

### Actuator Endpoints
- **GET** `/actuator/health` - Application health status
- **GET** `/actuator/info` - Application information

## Development Phases

- ✅ **Phase 1**: Project foundation (current)
- ⏳ **Phase 2**: Proxmox VE client implementation
- ⏳ **Phase 3**: Apache Guacamole client implementation
- ⏳ **Phase 4**: Domain models and business services
- ⏳ **Phase 5**: REST API endpoints

## Troubleshooting

### "No compiler is provided in this environment"
- Ensure Java 17 **JDK** is installed (not just JRE)
- Verify with: `javac -version`

### Port 8080 already in use
- Change port in `application.yml`: `server.port: 8081`
- Or set environment variable: `SERVER_PORT=8081`

### Configuration validation errors
- Ensure all required fields in `application.yml` have values
- Check that Proxmox and Guacamole URLs are properly formatted

## License

This is a proof of concept project.

## Contact

For questions or issues, please contact the development team.

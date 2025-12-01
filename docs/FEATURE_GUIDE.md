# Cloud PC Feature Integration Guide

**Target Audience:** Streaming App & IPTV Engineering Team  
**Purpose:** Technical guide for integrating the "Cloud PC" feature into our existing application ecosystem.

---

## 1. Feature Overview

We are introducing a **"Cloud PC"** feature into our existing Streaming and IPTV applications. This allows users to launch and access a full personal computer directly within our app interface.

**The User Experience:**
1.  User clicks "My Cloud PC" in the app.
2.  A WebView opens within the app.
3.  The user is automatically logged in and sees a Windows/Linux desktop.
4.  They can use a mouse/keyboard (or TV remote in pointer mode) to interact with the PC.

---

## 2. Architecture: How It Fits Our Ecosystem

This POC demonstrates the backend infrastructure required to power this feature. It sits alongside our existing backend services.

### System Diagram

```mermaid
graph TB
    subgraph "User Devices"
        MobileApp[Mobile App (WebView)]
        TVApp[IPTV App (WebView)]
        WebApp[Web Portal (IFrame)]
    end

    subgraph "Our Existing Backend"
        MainAPI[Streaming API]
        Auth[Auth Service]
    end

    subgraph "Cloud PC Infrastructure (New)"
        Orchestrator[Desktop Service (This POC)]
        Gateway[Guacamole Gateway]
        Compute[Proxmox Cluster]
    end

    MobileApp -->|1. Request PC| MainAPI
    TVApp -->|1. Request PC| MainAPI
    
    MainAPI -->|2. Provision/Start| Orchestrator
    Orchestrator -->|3. Manage VM| Compute
    
    Orchestrator -->|4. Get Secure URL| Gateway
    MainAPI -->|5. Return URL| MobileApp
    
    MobileApp -->|6. Connect (WebSocket)| Gateway
    Gateway -->|7. RDP Stream| Compute
```

---

## 3. Component Deep Dive (For the Team)

Here is what each new component does and why we need it.

### 3.1 Proxmox VE (The "Render Farm")
**Analogy:** Think of this like our transcoding servers, but instead of processing video, they run operating systems.

*   **What it is:** A cluster of powerful servers running a hypervisor (KVM).
*   **Role:** It hosts the actual Windows/Linux Virtual Machines (VMs).
*   **Why Proxmox?** It's open-source (no licensing fees like VMware), has a great API, and supports "Linked Clones" (creating a new PC takes seconds, not minutes).
*   **Scalability:** We can add more servers to the cluster as user demand grows. It handles load balancing automatically.

### 3.2 Apache Guacamole (The "Player")
**Analogy:** This is like our HLS/DASH video player, but interactive.

*   **What it is:** A protocol gateway that takes RDP (Remote Desktop Protocol) from the VM and converts it to HTML5/WebSocket streams.
*   **Role:** It allows the PC screen to be displayed in a standard WebView. No plugins or native SDKs are needed on the TV or Mobile app.
*   **Key Feature:** It handles the input. When a user moves their mouse on the mobile app, Guacamole sends that command to the VM.

### 3.3 Desktop Service (The "Controller")
**Analogy:** This is the middleware/CMS for the PCs.

*   **What it is:** A Spring Boot application (this POC).
*   **Role:** It bridges our Main API and the infrastructure.
*   **Responsibilities:**
    *   "Spin up a PC for User X."
    *   "Is User X's PC running?"
    *   "Give me a secure, one-time URL to connect to User X's PC."

---

## 4. Integration Steps

### Step 1: Backend Integration (Server-to-Server)

Our **Main Streaming API** needs to talk to the **Desktop Service**.

**Scenario: User clicks "Open PC"**

1.  **Check State:** Main API calls `GET /api/v1/desktops/user/{userId}`.
    *   *If 404:* Call `POST /api/v1/desktops` to create one.
    *   *If Stopped:* Call `POST /api/v1/desktops/{id}/start`.
    *   *If Running:* Proceed.

2.  **Get Connection URL:** Main API calls `GET /api/v1/desktops/{id}/connect`.
    *   **Response:** `https://guac.our-domain.com/client/...?token=xyz`

3.  **Return to Client:** Main API sends this URL to the Mobile/TV app.

### Step 2: Frontend Integration (WebView)

**For Mobile/Web Apps:**
*   Simply open a standard `WebView` (iOS/Android) or `<iframe>` (Web) pointing to the URL.
*   **Authentication:** The URL contains a pre-signed token. The WebView does *not* need to perform any login. It just loads the stream.

**For IPTV/TV Apps:**
*   **Input Handling:** This is the biggest challenge on TV.
*   **Pointer Mode:** The TV app should enable a "mouse cursor" mode where the D-Pad moves a cursor on the WebView.
*   **Virtual Keyboard:** When a text field is focused in the VM, Guacamole can trigger the native on-screen keyboard.
*   **Bluetooth:** Support for Bluetooth keyboard/mouse paired to the TV is supported out-of-the-box by the WebView.

---

## 5. Security & Authentication

We will **not** create new accounts for this. We will use our existing Auth system.

1.  **Trust:** The Desktop Service trusts our Main API.
2.  **Token Exchange:**
    *   When the Main API requests a connection URL, the Desktop Service generates a **Guacamole Token**.
    *   This token is valid for **one connection attempt** and expires in **60 seconds**.
3.  **No Leaks:** The URL is unique to that session. If shared, it won't work again.

---

## 6. Setup for Development

To start experimenting with this feature today without setting up servers:

1.  **Run the Mock Service:**
    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
    ```
2.  **Test the Flow:**
    *   Call `POST /api/v1/desktops` to "provision" a fake PC.
    *   Call `GET /api/v1/desktops/{id}/connect` to get a URL.
    *   Open that URL in your Mobile/TV App's WebView.
    *   *Note: In mock mode, you'll see a simulated desktop UI, proving the WebView integration works.*

---

## 7. Production Scalability

*   **100 Users:** Single Proxmox server, Single Guacamole instance.
*   **10,000 Users:**
    *   **Proxmox:** Cluster of 20+ servers. Shared storage (Ceph) allows VMs to run anywhere.
    *   **Guacamole:** Auto-scaling group of stateless gateway servers behind a Load Balancer.
    *   **Desktop Service:** Stateless microservice, scales horizontally.

# MeshLink: Offline Peer-to-Peer Multi-Hop Mesh Messaging Application

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Transport](https://img.shields.io/badge/Transport-Google%20Nearby%20Connections%20(P2P__CLUSTER)-FF8F00.svg)](https://developers.google.com/nearby/connections/overview)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**MeshLink** is a fully offline, multi-hop peer-to-peer (P2P) mesh messaging system for Android. It empowers mobile devices to autonomously discover one another, form dynamic ad-hoc local mesh clusters, and relay encrypted text messages across multiple intermediate physical hops—**without cellular data, SIM cards, internet access, Wi-Fi routers, Firebase, or external servers**.

---

## 📑 Table of Contents
1. [Key Features](#-key-features)
2. [Architecture Overview](#-architecture-overview)
3. [Mesh Protocol Specification](#-mesh-protocol-specification)
4. [Tech Stack & Dependencies](#-tech-stack--dependencies)
5. [College Project Demo & Viva Guide](#-college-project-demo--viva-guide)
   - [4-Phone Demonstration Setup (A → B → C → D)](#4-phone-demonstration-setup-a--b--c--d)
   - [Academic Defense / Viva Q&A](#academic-defense--viva-qa)
6. [Building and Running](#-building-and-running)
7. [Screenshots & UI Modules](#-screenshots--ui-modules)

---

## 🌟 Key Features

- 📴 **100% Offline Multi-Hop Relaying**: Messages hop dynamically across intermediate phones ($A \rightarrow B \rightarrow C \rightarrow D$) without any central router or cellular gateway.
- ⚡ **Google Nearby Connections (`Strategy.P2P_CLUSTER`)**: High-reliability, low-latency transport layer that autonomously orchestrates Bluetooth Low Energy (BLE) and Wi-Fi Direct/Hotspots.
- 🛡️ **Controlled Flooding & Anti-Storm Engine**:
  - **TTL (Time-To-Live)**: Automatically decremented at each hop to eliminate runaway broadcast packets.
  - **Hop Count Tracking**: Measures the exact hop distance traversed by each packet.
  - **Deduplication Engine**: In-memory LRU cache + SQLite database to drop redundant transmissions.
  - **Self-Loop Rejection**: Inspects `routeHistory` to ensure packets never traverse the same node twice.
- 🔄 **Two-Way Reverse Delivery Acknowledgements (ACK)**: When a packet reaches its final destination node, an automated reverse ACK travels back to origin, updating message status from `Queued` $\rightarrow$ `Sent` $\rightarrow$ `Delivered (3 hops)`.
- 📦 **Store-and-Forward Offline Queue**: Outgoing packets intended for out-of-range nodes are stored in a local SQLite queue and automatically dispatched when intermediate relay nodes connect.
- 🕸️ **Interactive Network Topology Visualizer**: Custom Compose Canvas rendering the live cluster graph with pulsing nodes, direct links, and multi-hop routes.
- 🧪 **College Demo & Routing Lab**:
  - 1-click Preset Node Identities (`NODE A`, `NODE B`, `NODE C`, `NODE D`).
  - Automated 4-node multi-hop test dispatcher.
  - Live in-app structured event logger (`[Mesh]`, `[Transport]`, `[Routing]`, `[Forwarding]`, `[Delivery]`, `[ACK]`).
- 🎨 **Modern Cyber-Mesh Aesthetic**: Dark theme built with Jetpack Compose, Material 3, and rich micro-interactions.

---

## 🏛 Architecture Overview

MeshLink follows Clean Architecture principles with clear separation of concerns:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION (UI)                             │
│   HomeScreen  │  MessagesScreen  │  ChatScreen  │  TopologyScreen     │
│                     │  DevicesScreen  │  DemoScreen                    │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ StateFlow / Compose
┌───────────────────────────────────┴────────────────────────────────────┐
│                           VIEW MODELS                                  │
│   HomeViewModel  │  MessagesViewModel  │  ChatViewModel                │
│   DevicesViewModel  │  NetworkViewModel  │  DemoViewModel              │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ Coroutines
┌───────────────────────────────────┴────────────────────────────────────┐
│                        DATA & REPOSITORY LAYER                         │
│  MessageRepository  │  PeerRepository  │  StoreAndForwardManager       │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                          MESH ROUTING ENGINE                           │
│   MeshRouter  │  TTL Decrementer  │  Deduplication  │  AckManager      │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                         PEER MANAGEMENT LAYER                          │
│   PeerManager  │  NodeIdentityManager  │  RoutingTable (RouteDao)      │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                           TRANSPORT LAYER                              │
│       MeshTransport (Interface)  │  NearbyConnectionsTransport         │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                    LOCAL ROOM SQLITE PERSISTENCE                       │
│  Messages  │  Conversations  │  Peers  │  ProcessedPackets  │  Queue   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 📡 Mesh Protocol Specification

### Packet Wire Format (`MeshPacket.kt`)

All network packets are serialized as JSON over the physical transport layer:

```json
{
  "messageId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "senderId": "NODE-A",
  "senderAlias": "Node Alpha",
  "destinationId": "NODE-D",
  "timestamp": 1700000000000,
  "ttl": 7,
  "hopCount": 0,
  "payload": "Emergency medical aid needed at Sector 4",
  "packetType": "CHAT",
  "routeHistory": ["NODE-A"],
  "originalMessageId": null
}
```

### Packet Routing Flowchart

```
           [ Packet Received on Physical Transport ]
                              │
                              ▼
                 [ Is Packet in LRU / Cache? ] ──── Yes ───► [ DROP (Duplicate) ]
                              │ No
                              ▼
                [ Is Local Node in routeHistory? ] ── Yes ──► [ DROP (Loop Detected) ]
                              │ No
                              ▼
            [ Record Sender in Route Table & Peers ]
                              │
          ┌───────────────────┴───────────────────┐
          ▼                                       ▼
  [ Is Target Me or Broadcast? ]          [ Is Destination Another Node? ]
          │                                       │
         Yes                                     Yes
          │                                       │
          ▼                                       ▼
  [ Deliver to Room DB / UI ]               [ Is TTL > 1? ]
          │                                       │
  [ Generate Reverse ACK ]                ┌───────┴───────┐
          │                               ▼               ▼
          ▼                              Yes              No
   [ Send ACK to Sender ]                 │               │
                                          ▼               ▼
                           [ TTL = TTL - 1, Hops = Hops + 1 ] [ DROP (Expired) ]
                           [ Append Me to routeHistory ]
                                          │
                                          ▼
                             [ Forward to Neighbors ]
```

---

## 🛠 Tech Stack & Dependencies

| Component | Technology | Version |
| :--- | :--- | :--- |
| **Language** | Kotlin | 2.0.20 |
| **Build Tool** | Gradle | 8.9 |
| **UI Framework** | Jetpack Compose + Material 3 | 2024.09.00 (BOM) |
| **P2P Transport** | Google Play Services Nearby Connections | 19.3.0 (`P2P_CLUSTER`) |
| **Database** | AndroidX Room + KSP | 2.6.1 |
| **Concurrency** | Kotlin Coroutines & StateFlow | 1.8.1 |
| **Serialization**| Kotlinx Serialization JSON | 1.7.2 |
| **Architecture** | MVVM + Repository + Clean Architecture | AndroidX Lifecycle 2.8.5 |

---

## 🎓 College Project Demo & Viva Guide

### 4-Phone Demonstration Setup (A → B → C → D)

This experiment demonstrates multi-hop communication when Phone A is out of physical range of Phone D:

```
[ PHONE A ]  ◄──── Bluetooth / Wi-Fi ────►  [ PHONE B ]
(Node A)                                    (Relay Node)
                                                 │
                                           Bluetooth / Wi-Fi
                                                 │
                                                 ▼
[ PHONE D ]  ◄──── Bluetooth / Wi-Fi ────►  [ PHONE C ]
(Node D)                                    (Relay Node)
```

#### Step-by-Step Demo Procedure:
1. **Install APK**: Install `app-debug.apk` on 4 Android smartphones.
2. **Grant Permissions**: Launch MeshLink on all 4 phones and tap **Grant Mesh Permissions** (Bluetooth & Wi-Fi Direct).
3. **Assign Identities**:
   - Open **Demo & Routing Lab** on each phone.
   - On Phone 1, tap `A`.
   - On Phone 2, tap `B`.
   - On Phone 3, tap `C`.
   - On Phone 4, tap `D`.
4. **Physical Placement**:
   - Place Phone A and Phone B together.
   - Place Phone B and Phone C in the middle.
   - Place Phone C and Phone D at the far end (such that Phone A cannot reach Phone D directly).
5. **Send Multi-Hop Message**:
   - On Phone A, open Chat with `NODE-D` and send: `"Test multi-hop payload"`.
6. **Observe the Propagation**:
   - In Phone B and Phone C's **Live Mesh Event Stream**, observe:
     `[Forwarding] Forwarding packet: new TTL=5, Hops=2, Route=NODE-A -> NODE-B -> NODE-C`
   - In Phone D, the chat opens with incoming message from `NODE A`!
   - In Phone D, an automatic `[ACK]` packet is generated and returned along the reverse path.
   - On Phone A, the checkmark turns from `✓ Sent` to `✓✓ Delivered (3 hops)` with the route trace accordion: `NODE-A → NODE-B → NODE-C → NODE-D`!

---

### 🎤 Academic Defense / Viva Q&A

**Q1: Why use Google Nearby Connections (`Strategy.P2P_CLUSTER`) instead of raw Wi-Fi Direct or raw BLE sockets?**
> *Answer*: Raw BLE has a very low throughput (MTU ~20-512 bytes) and cannot sustain fast multi-hop topologies reliably. Raw Wi-Fi Direct requires one device to be a Group Owner (GO) in an asymmetric star topology. Nearby Connections `P2P_CLUSTER` dynamically combines BLE for zero-configuration discovery and Wi-Fi Direct / Local Hotspot for high-speed M:N mesh clusters automatically.

**Q2: How does MeshLink prevent broadcast storms in dense networks?**
> *Answer*:
> 1. **Deduplication**: Every packet has a globally unique UUID `messageId`. Received packets are recorded in an in-memory LRU cache and SQLite `processed_packets` table. Identical packets received from multiple paths are dropped immediately.
> 2. **TTL (Time-To-Live)**: Each packet starts with a default TTL (e.g. 7). Every forwarder decrements TTL by 1. When TTL reaches 1, forwarding terminates.
> 3. **Loop Rejection**: Forwarders inspect `routeHistory`. If the local node ID is already in the list, the packet is rejected.
> 4. **Split-Horizon Forwarding**: Forwarded packets are broadcast to all neighbors *except* the neighbor that delivered the packet.

**Q3: How does delivery confirmation work without a central server?**
> *Answer*: When the target node detects that `destinationId == localNodeId`, it triggers `createDeliveryAck()`. This generates a lightweight `DELIVERY_ACK` packet with `destinationId = original.senderId` and `originalMessageId = original.messageId`. It propagates back through the mesh. When the original sender receives this ACK, Room DB updates the message status to `MessageStatus.DELIVERED`.

**Q4: What happens if a receiver is temporarily disconnected?**
> *Answer*: The `StoreAndForwardManager` intercepts messages that fail direct delivery and stores them in the SQLite `queued_packets` table. When the target node or an intermediate relay re-enters range and completes discovery, the queue is automatically drained and retried.

---

## 🔨 Building and Running

### Prerequisites
- Android Studio Hedgehog / Iguana / Jellyfish (or command line)
- JDK 17 or JDK 21
- Android SDK 34 / 35

### Command Line Build

1. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest --info
   ```

2. **Assemble Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on Connected Device via ADB**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📱 Screenshots & UI Modules

- **Home Dashboard**: Mesh master power switch, Node ID with 1-click clipboard copy, active peer counters, quick navigation cards.
- **Messages & Broadcast**: Direct chat threads and cluster broadcast channel.
- **Chat Bubble Screen**: Outgoing and incoming bubbles, live delivery status chips (`✓✓ Delivered`), interactive multi-hop path trace card (`NODE-A → NODE-B → NODE-C → NODE-D`), and configurable TTL slider.
- **Devices Screen**: Discovered cluster nodes, connection states (`CONNECTED`, `DISCOVERED`, `DISCONNECTED`), and manual connect/disconnect triggers.
- **Network Topology Screen**: 2D custom Canvas rendering glowing center node, solid direct links, dotted multi-hop links, and routing table.
- **Demo & Routing Lab**: 1-click preset identities, 4-node simulation injector, and live tag-filtered event stream (`[Mesh]`, `[Transport]`, `[Routing]`, `[Forwarding]`, `[Delivery]`, `[ACK]`).

---

## 📄 License
This project is licensed under the Apache License 2.0.

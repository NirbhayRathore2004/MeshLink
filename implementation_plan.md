# Implementation Plan: MeshLink Offline Peer-to-Peer Mesh Messaging Application

MeshLink is a fully offline, multi-hop peer-to-peer mesh messaging Android application. It allows multiple Android devices to discover each other, form an ad-hoc local mesh, and exchange text messages over multiple hops without cellular, internet, Wi-Fi routers, Firebase, or external servers.

---

## User Review Required

> [!IMPORTANT]
> **Transport Selection Rationale**: We will use **Google Play Services Nearby Connections (`Strategy.P2P_CLUSTER`)** as the primary high-reliability offline transport layer.
> - **Why `P2P_CLUSTER`?** It enables true M:N mesh topologies offline by dynamically orchestrating Bluetooth and Wi-Fi Direct/Hotspot links across multiple devices without requiring an active internet connection.
> - **Architecture Independence**: The transport layer is abstracted behind a clean `MeshTransport` interface, allowing alternative transports (e.g. raw Bluetooth RFCOMM sockets or Wi-Fi Direct) to be swapped or tested without modifying the routing, messaging, or UI layers.

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────┐
│                   PRESENTATION (UI)                    │
│   Home  │  Messages  │  Chat  │  Devices  │  Network   │
│         │            │        │           │  & Demo    │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / Compose
┌───────────────────────────┴────────────────────────────┐
│                    VIEW MODELS / APP                   │
│   HomeViewModel  │  ChatViewModel  │  NetworkViewModel │
└───────────────────────────▲────────────────────────────┘
                            │ Coroutines
┌───────────────────────────┴────────────────────────────┐
│                   MESSAGING & REPO                     │
│  ConversationRepository  │  MessageRepository          │
│  StoreAndForwardQueue    │  DeliveryAckManager         │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                   MESH ROUTING ENGINE                  │
│  PacketRouter  │  ControlledFlooding  │  Deduplication │
│  TTL/Hop Engine│  RoutingTable        │  RouteTracer   │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                    PEER MANAGEMENT                     │
│  PeerManager   │  NodeIdentityManager │ TopologyTracker│
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                   TRANSPORT INTERFACE                  │
│             MeshTransport (P2P_CLUSTER)                │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                   ROOM DATABASE (LOCAL)                │
│  Messages │ Conversations │ Peers │ PacketCache │ Queue│
└────────────────────────────────────────────────────────┘
```

---

## Proposed Changes

### Project Build & Configuration

#### [NEW] [settings.gradle.kts](file:///Users/virendra/Desktop/minor%20project/settings.gradle.kts)
- Gradle settings with Google, Maven Central repositories and project naming (`MeshLink`).

#### [NEW] [build.gradle.kts](file:///Users/virendra/Desktop/minor%20project/build.gradle.kts)
- Root build script configuring Android application plugin, Kotlin, KSP, and Compose compiler.

#### [NEW] [gradle.properties](file:///Users/virendra/Desktop/minor%20project/gradle.properties)
- JVM memory arguments, AndroidX flags, Kotlin code style.

#### [NEW] [local.properties](file:///Users/virendra/Desktop/minor%20project/local.properties)
- Points to the local Android SDK (`/Users/virendra/Library/Android/sdk`).

#### [NEW] [gradle/wrapper/gradle-wrapper.properties](file:///Users/virendra/Desktop/minor%20project/gradle/wrapper/gradle-wrapper.properties)
- Gradle 8.7 / 8.9 wrapper configuration.

#### [NEW] [app/build.gradle.kts](file:///Users/virendra/Desktop/minor%20project/app/build.gradle.kts)
- App module build script configured for:
  - compileSdk = 34 / 35, minSdk = 26, targetSdk = 34
  - Jetpack Compose & Material 3
  - Google Play Services Nearby Connections (`19.3.0`)
  - Room DB (`2.6.1`) with KSP
  - Kotlinx Serialization JSON (`1.6.3`)
  - Kotlinx Coroutines (`1.8.1`)
  - JUnit, Roborlectric / Coroutines Test for unit tests

---

### Core Mesh & Networking Engine

#### [NEW] [app/src/main/java/com/meshlink/mesh/model/MeshPacket.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/mesh/model/MeshPacket.kt)
- Core network packet representation:
  - `messageId: String` (UUID)
  - `senderId: String` (Origin Node ID)
  - `senderAlias: String` (e.g. "NODE-A")
  - `destinationId: String` (Target Node ID or `BROADCAST_ALL`)
  - `timestamp: Long`
  - `ttl: Int` (Default 7, decremented at every forward)
  - `hopCount: Int` (Incremented at every forward)
  - `payload: String` (Message text or control payload)
  - `packetType: PacketType` (`CHAT`, `DELIVERY_ACK`, `ROUTE_REQ`, `ROUTE_RESP`, `PEER_ANNOUNCE`, `PING`)
  - `routeHistory: List<String>` (Tracks hops: `["NODE-A", "NODE-B", "NODE-C", "NODE-D"]`)
  - `originalMessageId: String?` (Referenced message ID for ACKs)
  - JSON serialization/deserialization with integrity checks.

#### [NEW] [app/src/main/java/com/meshlink/mesh/transport/MeshTransport.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/mesh/transport/MeshTransport.kt)
- Clean abstraction interface for physical peer-to-peer transport:
  - `startAdvertising(nodeId: String, nodeAlias: String)`
  - `startDiscovery()`
  - `stopAdvertising()`
  - `stopDiscovery()`
  - `connectToPeer(endpointId: String)`
  - `disconnectPeer(endpointId: String)`
  - `sendBytes(endpointId: String, bytes: ByteArray): Boolean`
  - `observeEvents(): SharedFlow<TransportEvent>`

#### [NEW] [app/src/main/java/com/meshlink/mesh/transport/NearbyConnectionsTransport.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/mesh/transport/NearbyConnectionsTransport.kt)
- Concrete implementation using Google Nearby Connections `Strategy.P2P_CLUSTER`:
  - Autonomous discovery and advertisement callbacks
  - Automatic connection handshake and encrypted offline channel validation
  - Endpoint to Node ID bidirectional mapping
  - Disconnect and link failure detection

#### [NEW] [app/src/main/java/com/meshlink/mesh/peer/PeerManager.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/mesh/peer/PeerManager.kt)
- Manages connected peers, endpoint mappings, connection state lifecycle (`DISCOVERED`, `CONNECTING`, `CONNECTED`, `DISCONNECTED`), heartbeat, and signal metrics.

#### [NEW] [app/src/main/java/com/meshlink/mesh/routing/MeshRouter.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/mesh/routing/MeshRouter.kt)
- Central mesh routing engine:
  - Controlled flooding algorithm with TTL decrement and hop count tracking
  - Message deduplication check via in-memory cache + Room DB
  - Self-loop prevention using `routeHistory` and origin check
  - Destination matching: local delivery vs. forward
  - Immediate reverse-path ACK generation on message arrival
  - Multi-hop route recording for visual network mapping

#### [NEW] [app/src/main/java/com/meshlink/mesh/forwarding/StoreAndForwardManager.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/mesh/forwarding/StoreAndForwardManager.kt)
- Queues undelivered packets when peers are disconnected or offline
- Resends queued packets upon peer reconnection
- Enforces packet expiration (TTL / timestamp window)

#### [NEW] [app/src/main/java/com/meshlink/mesh/identity/NodeIdentityManager.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/mesh/identity/NodeIdentityManager.kt)
- Generates and persists a unique Node ID (`UUID` formatted or custom demo tag like `NODE-A`) across app restarts using EncryptedSharedPreferences / DataStore.

---

### Data & Room Persistence

#### [NEW] [app/src/main/java/com/meshlink/data/database/MeshDatabase.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/data/database/MeshDatabase.kt)
- Room database containing:
  - `MessageEntity` (ID, conversationId, senderId, destinationId, text, timestamp, status, hopCount, route)
  - `ConversationEntity` (peerId, peerAlias, lastMessage, lastTimestamp, unreadCount)
  - `PeerEntity` (nodeId, alias, endpointId, isConnected, lastSeen, hopsAway)
  - `ProcessedPacketEntity` (messageId, timestamp, receivedFrom)
  - `QueuedPacketEntity` (id, packetJson, destinationId, queuedTimestamp, retryCount)
  - `RouteEntity` (destinationNodeId, nextHopNodeId, hopCount, updatedTimestamp)

#### [NEW] [app/src/main/java/com/meshlink/data/repository/MessageRepository.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/data/repository/MessageRepository.kt)
- Coordinates message creation, database queries, status updates (`PENDING`, `SENT_TO_PEER`, `FORWARDED`, `DELIVERED`, `FAILED`), and conversation threading.

---

### Background Service & Logging

#### [NEW] [app/src/main/java/com/meshlink/service/MeshService.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/service/MeshService.kt)
- Foreground service with persistent notification keeping mesh discovery, transport, and routing active in the background.

#### [NEW] [app/src/main/java/com/meshlink/util/MeshLogger.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/util/MeshLogger.kt)
- Structured logging system (`[Mesh]`, `[Transport]`, `[Routing]`, `[Forwarding]`, `[Delivery]`, `[ACK]`) with an in-memory observable buffer for the live in-app log viewer.

---

### Jetpack Compose UI (Material 3)

#### [NEW] [app/src/main/java/com/meshlink/ui/theme/Theme.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/ui/theme/Theme.kt)
- Modern Material 3 dark/light theme, custom color palette (cyber/mesh vibrant aesthetics).

#### [NEW] [app/src/main/java/com/meshlink/ui/home/HomeScreen.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/ui/home/HomeScreen.kt)
- Dashboard displaying:
  - My Node ID (with 1-click copy & share)
  - Mesh Status (Active / Inactive toggle)
  - Connected Peers count & Active routes count
  - Quick action cards to Messages, Devices, Network Topology, and Demo mode

#### [NEW] [app/src/main/java/com/meshlink/ui/messages/MessagesScreen.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/ui/messages/MessagesScreen.kt)
- Conversation list with peer node ID, last message preview, timestamp, status indicator, and FAB to start a new chat.

#### [NEW] [app/src/main/java/com/meshlink/ui/chat/ChatScreen.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/ui/chat/ChatScreen.kt)
- Full interactive chat view with chat bubbles:
  - Delivery status icons (`✓ Queued`, `✓ Sent`, `✓✓ Delivered (3 hops)`)
  - Route display accordion (e.g. `NODE-A → NODE-B → NODE-C → NODE-D`)
  - Message input bar with destination node selector and TTL configuration.

#### [NEW] [app/src/main/java/com/meshlink/ui/devices/DevicesScreen.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/ui/devices/DevicesScreen.kt)
- Discovered & connected peers list with signal strength, connection status, manual Connect/Disconnect triggers, and Ping test.

#### [NEW] [app/src/main/java/com/meshlink/ui/network/NetworkTopologyScreen.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/ui/network/NetworkTopologyScreen.kt)
- Visual canvas rendering the live mesh network graph:
  - This device (center glowing node)
  - Direct neighbors (connected with solid links)
  - Multi-hop distant nodes (connected via neighbors with hop badges)
  - Animated packet pulse along routes during active transmission.

#### [NEW] [app/src/main/java/com/meshlink/ui/demo/DemoScreen.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/ui/demo/DemoScreen.kt)
- College presentation demo & developer screen:
  - 1-click node identity presets (`NODE A`, `NODE B`, `NODE C`, `NODE D`)
  - Automated 4-phone multi-hop test dispatcher (A sends to D via B and C)
  - Live packet forwarding monitor & routing table viewer
  - Real-time structured log stream with search/filtering
  - Cache reset and network flush controls.

#### [NEW] [app/src/main/java/com/meshlink/ui/permissions/PermissionHandler.kt](file:///Users/virendra/Desktop/minor%20project/app/src/main/java/com/meshlink/ui/permissions/PermissionHandler.kt)
- Clean, user-friendly permission request dialog for Android 12+ (Bluetooth Scan/Connect/Advertise, Nearby Wi-Fi Devices, Location).

---

### Manifest & Tests

#### [NEW] [app/src/main/AndroidManifest.xml](file:///Users/virendra/Desktop/minor%20project/app/src/main/AndroidManifest.xml)
- Declares necessary permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`), registers `MeshService` and `MainActivity`.

#### [NEW] [app/src/test/java/com/meshlink/mesh/MeshPacketTest.kt](file:///Users/virendra/Desktop/minor%20project/app/src/test/java/com/meshlink/mesh/MeshPacketTest.kt)
- Unit tests for packet serialization, deserialization, JSON integrity.

#### [NEW] [app/src/test/java/com/meshlink/mesh/MeshRouterTest.kt](file:///Users/virendra/Desktop/minor%20project/app/src/test/java/com/meshlink/mesh/MeshRouterTest.kt)
- Unit tests for:
  - TTL decrement & expiration at TTL = 0
  - Duplicate packet detection & suppression
  - Loop prevention (rejecting packets that contain local node in route history)
  - Multi-hop forwarding logic and peer exclusion (not reflecting to sender)
  - Local delivery recognition and ACK generation.

#### [NEW] [README.md](file:///Users/virendra/Desktop/minor%20project/README.md)
- Complete technical documentation, architecture diagrams, transport analysis, physical demonstration guide for 4 phones (A → B → C → D), viva prep notes, and troubleshooting.

---

## Verification Plan

### Automated Tests
- Run Gradle unit tests:
  ```bash
  ./gradlew testDebugUnitTest --info
  ```
- Build debug APK:
  ```bash
  ./gradlew assembleDebug --stacktrace
  ```
- Verify generated APK artifact exists at `app/build/outputs/apk/debug/app-debug.apk`.

### Manual & Demonstration Verification
- Verify that the app starts up, initializes Node ID, displays UI tabs, handles runtime permissions gracefully, renders network topology graph, records structured logs, and simulates/executes multi-hop packet forwarding.

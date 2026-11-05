# PROJECT REPORT CONTEXT & SPECIFICATION FILE: MESHLINK
> **Instructions for Claude / AI Report Generator**:
> You are an expert Computer Science professor and academic technical writer. Use the comprehensive technical specifications, architecture diagrams, code explanations, database schemas, test results, and protocol details below to generate a formal, high-scoring **Undergraduate / Postgraduate Project Report** (Minor / Major B.Tech / B.E. / M.Tech in Computer Science & Engineering / Information Technology).
>
> Follow standard academic formatting guidelines:
> - Use IEEE / University standard chapter numbering (Chapter 1 to Chapter 8).
> - Include Abstract, Certificate template, Acknowledgments, List of Figures, List of Tables, System Requirements, Detailed System Design with ASCII/Mermaid diagrams, Database Schema Tables, Algorithm Pseudocode, Implementation details, Testing & Experimental Results, Viva Q&A, and References.
> - Maintain rigorous academic tone, technical depth, and thorough explanations.

---

# TABLE OF CONTENTS
1. [Project Overview & Metadata](#1-project-overview--metadata)
2. [Abstract & Executive Summary](#2-abstract--executive-summary)
3. [Problem Statement & Motivation](#3-problem-statement--motivation)
4. [Literature Survey & Existing Systems Comparison](#4-literature-survey--existing-systems-comparison)
5. [Proposed System & Core Contributions](#5-proposed-system--core-contributions)
6. [System Requirements (Hardware & Software)](#6-system-requirements)
7. [System Architecture & Design](#7-system-architecture--design)
8. [Mesh Protocol Specification & Algorithms](#8-mesh-protocol-specification--algorithms)
9. [Database Design & Data Models](#9-database-design--data-models)
10. [Module-by-Module Implementation Details](#10-module-by-module-implementation-details)
11. [Testing, Simulation & Experimental Results](#11-testing-simulation--experimental-results)
12. [College Demonstration & Viva Voce Guide (Q&A)](#12-college-demonstration--viva-voce-guide-qa)
13. [Limitations & Future Enhancements](#13-limitations--future-enhancements)
14. [References](#14-references)

---

## 1. Project Overview & Metadata

* **Project Title**: MeshLink: Fully Offline Peer-to-Peer Multi-Hop Mesh Messaging System for Android
* **Project Type**: B.Tech / B.E. Minor / Major Capstone Project
* **Domain**: Mobile Ad-Hoc Networks (MANET), Delay-Tolerant Networking (DTN), Distributed Systems, Android Application Development
* **Target OS**: Android 8.0 (Oreo, API 26) up to Android 15 (Vanilla Ice Cream, API 35)
* **Core Language**: Kotlin 2.0.20
* **UI Toolkit**: Jetpack Compose (BOM 2024.09.00) with Material 3 Design
* **Underlying P2P Transport**: Google Play Services Nearby Connections API 19.3.0 (`Strategy.P2P_CLUSTER`)
* **Local Database**: AndroidX Room SQLite 2.6.1 with KSP (Kotlin Symbol Processing)
* **Architecture Pattern**: Clean Architecture + MVVM (Model-View-ViewModel) + Repository Pattern
* **Concurrency Model**: Kotlin Coroutines 1.8.1, Kotlin `StateFlow` & `SharedFlow`
* **Build System**: Gradle 8.14.5, Android Gradle Plugin (AGP) 8.13.2

---

## 2. Abstract & Executive Summary

Modern mobile communications depend entirely on centralized network infrastructures, such as cellular base stations (4G LTE / 5G towers), fiber backbones, DNS servers, and cloud messaging brokers. During natural calamities (earthquakes, floods, hurricanes), tactical military operations, remote wilderness exploration, and network blackouts, this centralized paradigm completely collapses due to infrastructure failure or physical disconnection.

**MeshLink** is an autonomous, decentralized, zero-infrastructure peer-to-peer (P2P) mobile mesh messaging system engineered for Android smartphones. The application enables standard off-the-shelf Android devices to dynamically self-organize into an ad-hoc multi-hop mesh network using heterogeneous local radios—Bluetooth Low Energy (BLE) for discovery and Wi-Fi Direct / Local Hotspots for high-bandwidth data transmission—orchestrated via the Google Nearby Connections `P2P_CLUSTER` topology.

Key technical innovations implemented in MeshLink include:
1. **Multi-Hop Dynamic Routing**: Packets propagate across arbitrary intermediate nodes ($A \rightarrow B \rightarrow C \rightarrow D$) without manual intervention or static routing tables.
2. **Anti-Broadcast Storm & Loop Avoidance**: Integration of Time-To-Live (TTL) decrements, hop-count metrics, circular route rejection (`routeHistory`), and two-tier packet deduplication (in-memory LRU cache + SQLite persistence).
3. **End-to-End Reverse Delivery Confirmations (ACK)**: Automated generation of reverse-traveling ACK packets once a destination node consumes the payload, transitioning message states from *Queued* $\rightarrow$ *Sent to Peer* $\rightarrow$ *Delivered (N hops)* with visual route trace visualization.
4. **Delay-Tolerant Store-and-Forward Engine (DTN)**: Local SQLite queueing mechanism that caches messages targeted at out-of-range nodes and automatically drains when new relay nodes or target peers enter physical proximity.
5. **Interactive Network Topology Visualizer**: Custom Compose Canvas dynamically rendering the live cluster graph with pulsing nodes, direct links, and multi-hop routes.

---

## 3. Problem Statement & Motivation

### 3.1 The Centralization Bottleneck
Conventional instant messaging platforms (WhatsApp, Signal, Telegram) require:
* Active cellular connection or local Wi-Fi router with WAN internet access.
* Central authentication servers and cloud databases.
* Continuous DNS resolution and IP routing.

When disaster strikes:
* Power grids fail, disabling cellular towers.
* Physical backhaul fiber lines are severed.
* Network congestion leads to total denial-of-service for emergency communications.
* Remote areas (mines, dense forests, rural disaster relief camps) have zero cellular penetration.

### 3.2 Objectives of MeshLink
* **Zero Infrastructure Requirement**: Operate completely without internet, SIM cards, Wi-Fi routers, or central servers.
* **Autonomous Cluster Formation**: Devices within radio range must discover and connect automatically in the background.
* **Multi-Hop Forwarding**: Allow two devices out of physical radio range to communicate by hopping through intermediate relay nodes.
* **Low Latency & High Reliability**: Maintain message delivery integrity using deduplication, loop prevention, and reverse ACKs.
* **Visual Transparency**: Provide real-time cluster visualization and live event logging for situational awareness and network diagnostics.

---

## 4. Literature Survey & Existing Systems Comparison

| Feature / Metric | Standard SMS / Cellular | WhatsApp / Signal | FireChat / Bridgefy | Briar | **MeshLink (Proposed)** |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Internet Dependency** | No (Needs Tower) | Yes (Mandatory) | No | No | **100% Offline (No Internet)** |
| **Cellular Tower / SIM** | Yes | Yes (For Signup) | No | No | **None (Zero SIM/Tower)** |
| **Multi-Hop Relaying** | No | No (Client-Server) | Proprietary / Partial | Partial (Tor/Wi-Fi) | **Yes (Dynamic Multi-Hop)** |
| **Topology Type** | Centralized Star | Centralized Cloud | Ad-Hoc P2P | P2P over Tor | **M:N Multi-Point Mesh Cluster** |
| **Underlying Physical Transport** | GSM / LTE / 5G | TCP/IP over WAN | Bluetooth RFCOMM | Tor / Wi-Fi / BT | **BLE + Wi-Fi Direct (Nearby API)** |
| **Delivery Confirmation** | Single Hop (SMSC) | Central Server ACK | Unreliable | Asynchronous | **Reverse Multi-Hop ACK** |
| **Loop Prevention** | Central Gateway | Central Server | Simple Sequence | Cryptographic | **TTL + RouteHistory + LRU Dedup** |
| **Live Topology Visualizer** | No | No | No | No | **Yes (Compose Canvas Graph)** |
| **Open Architecture** | Closed Telco | Closed Server | Closed Proprietary | Open Source | **Open Clean Kotlin Architecture** |

---

## 5. Proposed System & Core Contributions

MeshLink introduces a robust, layered software architecture designed specifically for modern Android hardware.

### Key Contributions:
1. **Clean Separation of Concerns**: Complete abstraction between the physical RF transport (`MeshTransport`), routing logic (`MeshRouter`), peer state machine (`PeerManager`), delay-tolerant storage (`StoreAndForwardManager`), and presentation layer (`Jetpack Compose`).
2. **Deterministic Packet Wire Protocol (`MeshPacket`)**: A compact JSON serialization format containing message IDs, sender and destination identifiers, TTL counters, hop counts, and cumulative route histories.
3. **Controlled Flooding with Split-Horizon**: Mitigates broadcast storms by excluding the receiving interface when propagating packets to neighboring peers.
4. **Multi-Hop Peer Discovery**: Propagates node identity announcements through intermediate nodes so distant users appear in discovery lists even before direct contact.
5. **Academic Viva & Demonstration Lab**: Embedded diagnostic tools featuring 1-click node identity switching (`NODE-A`, `NODE-B`, `NODE-C`, `NODE-D`), automated 4-node simulation dispatchers, and live tag-filtered logging.

---

## 6. System Requirements

### 6.1 Hardware Requirements
* **Processor**: 64-bit ARMv8 Octa-core CPU (or equivalent x86_64 emulator).
* **RAM**: Minimum 2 GB (4 GB+ recommended).
* **Radios**:
  * Bluetooth 4.2+ with Bluetooth Low Energy (BLE) Advertising and Peripheral Mode support.
  * Wi-Fi 802.11 b/g/n/ac with Wi-Fi Direct (P2P) support.
* **Physical Devices for Full Multi-Hop Testing**: Minimum 3 to 4 Android smartphones.

### 6.2 Software Requirements
* **Operating System**: Android 8.0 (API level 26) through Android 15 (API level 35).
* **Target SDK**: API 34 / 35.
* **Development Environment**: Android Studio (Hedgehog 2023.1.1 or later).
* **Build System**: Gradle 8.14.5 with Android Gradle Plugin 8.13.2.
* **JDK Version**: Java Development Kit 17 or 21 (LTS).
* **Google Play Services**: Version 19.3.0 or higher (for Nearby Connections).

---

## 7. System Architecture & Design

### 7.1 Multi-Tier Architecture Diagram
```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                              │
│  - HomeScreen: Mesh switch, node credentials, cluster statistics       │
│  - MessagesScreen: Active peer conversation list & unread indicators   │
│  - ChatScreen: Interactive bubbles, TTL slider, path trace accordion   │
│  - DevicesScreen: Discovered cluster peers, RSSI/hop distances         │
│  - NetworkTopologyScreen: 2D dynamic Canvas vector rendering of mesh   │
│  - DemoScreen: 4-Node Multi-Hop simulation lab & real-time log stream   │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ StateFlow / Compose
┌───────────────────────────────────┴────────────────────────────────────┐
│                        VIEWMODEL LAYER (MVVM)                          │
│  HomeViewModel │ MessagesViewModel │ ChatViewModel │ DevicesViewModel  │
│  NetworkViewModel │ DemoViewModel                                     │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ Coroutine Scopes (Dispatchers.IO)
┌───────────────────────────────────┴────────────────────────────────────┐
│                        DATA & REPOSITORY LAYER                         │
│  - MessageRepository: Chat history orchestration & conversation stats │
│  - PeerRepository: 1-hop and multi-hop active peer status streams      │
│  - StoreAndForwardManager: Delayed offline queue management            │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                         MESH ROUTING ENGINE                            │
│  - MeshRouter: Central packet processor & dispatcher                   │
│  - Deduplication Engine: Fast in-memory LRU (1000 items) + SQLite      │
│  - Loop Prevention: Route history inspection & sender filtering       │
│  - Route Table Manager: Dynamic next-hop calculation                   │
│  - Delivery & Reverse ACK Engine: Automated reverse path ACK dispatch  │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                       PEER & IDENTITY MANAGERS                         │
│  - PeerManager: Active peer registry, connection state machine         │
│  - NodeIdentityManager: Unique persistent Node ID & Alias persistence │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                           TRANSPORT LAYER                              │
│  - MeshTransport Interface: Contract for send, broadcast, connect      │
│  - NearbyConnectionsTransport: Google Nearby Connections P2P_CLUSTER   │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                     PERSISTENCE LAYER (ROOM SQLITE)                    │
│  Tables: messages | conversations | peers | routes | processed_packets │
│          queued_packets                                                │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Mesh Protocol Specification & Algorithms

### 8.1 Packet Wire Format (`MeshPacket.kt`)
Packets are transmitted as UTF-8 encoded JSON byte arrays over Nearby Connections byte payloads:

```json
{
  "messageId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "senderId": "NODE-A",
  "senderAlias": "Node Alpha",
  "destinationId": "NODE-D",
  "timestamp": 1700000000000,
  "ttl": 7,
  "hopCount": 0,
  "payload": "Emergency: Medical assistance requested at Base Alpha.",
  "packetType": "CHAT",
  "routeHistory": ["NODE-A"],
  "originalMessageId": null
}
```

#### Field Definitions:
* `messageId` (String): Universally Unique Identifier (UUIDv4) identifying the packet.
* `senderId` (String): Hardware-independent unique ID of the originating node (`NODE-XXXXXX`).
* `senderAlias` (String): Human-readable alias (e.g., "Node Alpha", "Rescue Unit 1").
* `destinationId` (String): Target Node ID or `"BROADCAST"` for cluster-wide flood.
* `timestamp` (Long): Unix epoch millisecond timestamp at generation.
* `ttl` (Int): Time-To-Live. Decremented by 1 at each forwarding hop. Dropped when $\le 1$. Default is 7.
* `hopCount` (Int): Number of intermediate hops traversed. Incremented by 1 at each hop.
* `payload` (String): Text message payload or control instructions.
* `packetType` (Enum): `CHAT`, `DELIVERY_ACK`, `PEER_ANNOUNCE`, `PING`, `ROUTE_REQ`, `ROUTE_RESP`.
* `routeHistory` (List<String>): Ordered list of node IDs traversed. Used for circular loop detection and reverse route learning.
* `originalMessageId` (String?): Present only in `DELIVERY_ACK` packets to reference the acknowledged chat message.

---

### 8.2 Dynamic Packet Routing Flowchart
```
                 [ Incoming Byte Payload Received ]
                                │
                                ▼
               [ Deserialize JSON to MeshPacket ]
                                │
                                ▼
             [ Is messageId in LRU / SQLite DB? ] ─── Yes ──► [ DROP PACKET ]
                                │ No                          (Deduplication)
                                ▼
            [ Record in LRU Cache & SQLite Processed ]
                                │
                                ▼
         [ Is senderId == localNodeId OR ]
         [ localNodeId in packet.routeHistory? ] ─── Yes ──► [ DROP PACKET ]
                                │ No                         (Loop Avoidance)
                                ▼
            [ Learn Reverse Route to senderId ]
            [ Register Multi-Hop Peer in PeerManager ]
                                │
       ┌────────────────────────┴────────────────────────┐
       ▼                                                 ▼
[ Is Target == Me OR BROADCAST? ]               [ Is Target != Me? ]
       │                                                 │
      Yes                                               Yes
       │                                                 │
       ▼                                                 ▼
[ Save to SQLite Messages Table ]                 [ Is TTL > 1? ]
[ Emit to UI SharedFlow ]                         ┌──────┴──────┐
       │                                          ▼             ▼
[ If Direct Message: ]                           Yes            No
[ Generate Reverse DELIVERY_ACK ]                 │             │
[ Send ACK back along reverse path ]              ▼             ▼
                                     [ Decrement TTL: ttl - 1 ] [ DROP PACKET ]
                                     [ Increment Hops: hop + 1] (TTL Expired)
                                     [ Append Me to routeHistory]
                                                  │
                                                  ▼
                                     [ Route Lookup in RouteDao ]
                                     ┌────────────┴────────────┐
                                     ▼                         ▼
                            [ Direct Route Exists? ]   [ No Direct Route ]
                                     │                         │
                                    Yes                        │
                                     │                         │
                                     ▼                         ▼
                          [ Send to Next-Hop ]     [ Controlled Flood ]
                          [ Endpoint Unicast ]     [ Broadcast to all ]
                                                   [ EXCEPT incoming ]
```

---

### 8.3 Algorithms Pseudocode

#### Algorithm 1: Packet Ingestion and Multi-Hop Forwarding
```
ALGORITHM: ProcessIncomingPacket(Packet P, IncomingEndpointId E_in)
INPUT: Packet P, String E_in (endpoint from which P arrived)
OUTPUT: None (Side effects: Local delivery or re-transmission)

1. localId ← identityManager.getNodeId()
2. localAlias ← identityManager.getNodeAlias()

// Step 1: Deduplication
3. IF DeduplicationCache.contains(P.messageId) OR ProcessedPacketDao.has(P.messageId) THEN
4.     LOG("Dropping duplicate packet: " + P.messageId)
5.     RETURN
6. END IF
7. DeduplicationCache.insert(P.messageId)
8. ProcessedPacketDao.insert(P.messageId, CURRENT_TIMESTAMP)

// Step 2: Loop Avoidance
9. IF P.senderId == localId OR localId IN P.routeHistory THEN
10.    LOG("Loop detected. Dropping packet: " + P.messageId)
11.    RETURN
12. END IF

// Step 3: Reverse Route Learning
13. RouteDao.insertOrUpdate(
14.    destinationNodeId = P.senderId,
15.    nextHopEndpointId = E_in,
16.    hopCount = P.hopCount + 1,
17.    lastUpdated = CURRENT_TIMESTAMP
18. )

// Step 4: Local Delivery Check
19. IF P.destinationId == localId OR P.destinationId == "BROADCAST" THEN
20.    DeliverToLocalUser(P)
21.    IF P.destinationId == localId AND P.packetType == CHAT THEN
22.        ACK ← CreateDeliveryAck(P, localId, localAlias)
23.        SendPacket(ACK)
24.    END IF
25. END IF

// Step 5: Multi-Hop Forwarding
26. IF P.destinationId != localId THEN
27.    IF P.ttl > 1 THEN
28.        P_forward ← Clone(P)
29.        P_forward.ttl ← P.ttl - 1
30.        P_forward.hopCount ← P.hopCount + 1
31.        P_forward.routeHistory.append(localId)
32.        
33.        knownRoute ← RouteDao.find(P_forward.destinationId)
34.        IF knownRoute != NULL AND knownRoute.nextHopEndpointId != E_in THEN
35.            Transport.sendBytes(knownRoute.nextHopEndpointId, P_forward.toBytes())
36.        ELSE
37.            // Split-Horizon Controlled Flood
38.            Transport.broadcastBytes(P_forward.toBytes(), excludeEndpointId = E_in)
39.        END IF
40.    ELSE
41.        LOG("TTL expired for packet: " + P.messageId)
42.    END IF
43. END IF
```

#### Algorithm 2: Two-Way Delivery Acknowledgement (ACK) Handling
```
ALGORITHM: HandleDeliveryAck(Packet AckPacket)
INPUT: AckPacket (type == DELIVERY_ACK)
OUTPUT: None

1. localId ← identityManager.getNodeId()
2. IF AckPacket.destinationId == localId THEN
3.     // This node was the original sender of the chat message
4.     origMsgId ← AckPacket.originalMessageId
5.     hopDistance ← AckPacket.hopCount
6.     routeJson ← JSON_SERIALIZE(AckPacket.routeHistory)
7.
8.     MessageDao.updateDeliveryStatus(
9.         messageId = origMsgId,
10.        status = "DELIVERED",
11.        hopCount = hopDistance,
12.        routeJson = routeJson
13.    )
14.    LOG("Message " + origMsgId + " delivered successfully across " + hopDistance + " hops")
15. ELSE
16.    // Intermediate node: forward ACK towards original sender
17.    ForwardPacket(AckPacket)
18. END IF
```

---

## 9. Database Design & Data Models

MeshLink uses Android Room persistence (backed by SQLite) to maintain state across app restarts.

### 9.1 Entity Relationship (ER) Model

```
 ┌──────────────────────┐         1:N         ┌──────────────────────┐
 │    conversations     │────────────────────►│       messages       │
 ├──────────────────────┤                     ├──────────────────────┤
 │ peerId (PK, TEXT)    │                     │ id (PK, TEXT)        │
 │ peerAlias (TEXT)     │                     │ conversationId (TEXT)│
 │ lastMessage (TEXT)   │                     │ senderId (TEXT)      │
 │ lastTimestamp (INT)  │                     │ senderAlias (TEXT)   │
 │ unreadCount (INT)    │                     │ destinationId (TEXT) │
 └──────────────────────┘                     │ text (TEXT)          │
                                              │ timestamp (INT)      │
 ┌──────────────────────┐                     │ status (TEXT)        │
 │        peers         │                     │ hopCount (INT)       │
 ├──────────────────────┤                     │ routeHistoryJson (TXT│
 │ nodeId (PK, TEXT)    │                     │ isOutgoing (BOOL)    │
 │ alias (TEXT)         │                     └──────────────────────┘
 │ endpointId (TEXT)    │
 │ connectionState (TXT)│         1:1         ┌──────────────────────┐
 │ hopDistance (INT)    │────────────────────►│        routes        │
 │ viaNodeId (TEXT)     │                     ├──────────────────────┤
 │ lastSeen (INT)       │                     │ destinationNodeId(PK)│
 └──────────────────────┘                     │ nextHopEndpointId(TXT│
                                              │ hopCount (INT)       │
 ┌──────────────────────┐                     │ destinationAlias(TXT)│
 │  processed_packets   │                     │ lastUpdated (INT)    │
 ├──────────────────────┤                     └──────────────────────┘
 │ messageId (PK, TEXT) │
 │ processedAt (INT)    │                     ┌──────────────────────┐
 └──────────────────────┘                     │    queued_packets    │
                                              ├──────────────────────┤
                                              │ id (PK, AUTO_INT)    │
                                              │ messageId (TEXT)     │
                                              │ destinationNodeId(TXT│
                                              │ packetJson (TEXT)    │
                                              │ timestamp (INT)      │
                                              │ retryCount (INT)     │
                                              └──────────────────────┘
```

### 9.2 Schema Tables

#### Table 1: `messages`
Represents direct and multi-hop messages sent and received.
* `id` (TEXT, Primary Key): UUID of message.
* `conversationId` (TEXT, Index): Peer Node ID this message belongs to.
* `senderId` (TEXT): Node ID of originator.
* `senderAlias` (TEXT): Name of originator.
* `destinationId` (TEXT): Target Node ID or `BROADCAST`.
* `text` (TEXT): Chat message body.
* `timestamp` (INTEGER): Generation timestamp in ms.
* `status` (TEXT): Enum (`PENDING`, `SENT_TO_PEER`, `DELIVERED`, `FAILED`).
* `hopCount` (INTEGER): Number of hops traversed to reach target.
* `routeHistoryJson` (TEXT): Serialized JSON array of route path (`["NODE-A","NODE-B","NODE-C"]`).
* `isOutgoing` (INTEGER / Boolean): 1 for sent, 0 for received.

#### Table 2: `conversations`
Maintains summary records for the messaging list view.
* `peerId` (TEXT, Primary Key): Unique Node ID of chat counterpart.
* `peerAlias` (TEXT): Display name.
* `lastMessage` (TEXT): Preview snippet of latest message.
* `lastTimestamp` (INTEGER): Epoch ms of last activity.
* `unreadCount` (INTEGER): Count of unread incoming messages.

#### Table 3: `peers`
Registry of all discovered 1-hop physical neighbors and multi-hop distant nodes.
* `nodeId` (TEXT, Primary Key): Unique node identifier.
* `alias` (TEXT): Display name.
* `endpointId` (TEXT, Nullable): Nearby Connections 4-character endpoint ID.
* `connectionState` (TEXT): `DISCOVERED`, `CONNECTING`, `CONNECTED`, `DISCONNECTED`.
* `hopDistance` (INTEGER): 1 for direct RF link, 2+ for multi-hop.
* `viaNodeId` (TEXT, Nullable): For multi-hop peers, the immediate relay node ID.
* `lastSeen` (INTEGER): Timestamp of latest communication.

#### Table 4: `routes`
Dynamic routing cache mapping destination nodes to physical next-hop endpoints.
* `destinationNodeId` (TEXT, Primary Key): Ultimate target.
* `nextHopEndpointId` (TEXT): Immediate endpoint to transmit towards.
* `hopCount` (INTEGER): Hop metric.
* `destinationAlias` (TEXT): Name of target.
* `lastUpdated` (INTEGER): Timestamp for cache pruning.

#### Table 5: `processed_packets`
Used for non-volatile packet deduplication.
* `messageId` (TEXT, Primary Key): Packet UUID.
* `processedAt` (INTEGER): Timestamp of recording.

#### Table 6: `queued_packets`
Store-and-Forward delay-tolerant message queue.
* `id` (INTEGER, Primary Key, Autoincrement).
* `messageId` (TEXT): Associated packet ID.
* `destinationNodeId` (TEXT): Target node.
* `packetJson` (TEXT): Complete serialized `MeshPacket`.
* `timestamp` (INTEGER): When queued.
* `retryCount` (INTEGER): Dispatch retry attempts.

---

## 10. Module-by-Module Implementation Details

### 10.1 `NearbyConnectionsTransport.kt` (Physical Transport Layer)
* Implements the `MeshTransport` interface using Google Play Services Nearby Connections.
* Configures `Strategy.P2P_CLUSTER`: Enables an M-to-N multi-point mesh cluster where any node can simultaneously advertise and discover peers.
* Manages `ConnectionLifecycleCallback`:
  * Automatically sends connection requests to newly discovered endpoints.
  * Automatically accepts incoming connection requests without user-facing modal interruptions (`acceptConnection`).
* Payload Management:
  * Encapsulates data into `Payload.fromBytes(bytes)`.
  * Emits received payload bytes as `TransportEvent.DataReceived` onto a Kotlin `SharedFlow`.
  * Handles graceful disconnection, connection failures, and radio state recovery.

### 10.2 `MeshRouter.kt` (Routing & Forwarding Core)
* The central brain of MeshLink.
* Concurrency Architecture: Utilizes an injectable `CoroutineScope` (default `Dispatchers.IO`, `Dispatchers.Unconfined` in testing) to handle high-frequency incoming packet floods asynchronously.
* Fast In-Memory LRU Cache: Maintains an in-memory `LinkedHashMap` of the latest 1,000 processed packet IDs for instant sub-millisecond deduplication before hitting SQLite.
* Reverse ACK Dispatcher: Detects arrival of target unicast chat packets, constructs `DELIVERY_ACK`, and transmits it in reverse.
* Route Trace Recording: When an ACK returns to the originating node, it attaches the full cumulative `routeHistory` to the message entity, allowing the user to tap and inspect the physical node path.

### 10.3 `PeerManager.kt` (State Machine & Registry)
* Tracks real-time states of physical 1-hop endpoints and virtual multi-hop nodes.
* Auto-reconnect Logic: When an endpoint disconnects, `PeerManager` marks it disconnected and notifies `MeshRouter` to purge invalid routes from `RouteDao`.
* Multi-Hop Peer Propagation: When receiving announcements or forwarded packets from distant nodes, updates their hop distance and intermediate relay mapping.

### 10.4 `StoreAndForwardManager.kt` (Delay-Tolerant Engine)
* Intercepts messages when `sendPacket()` reports zero connected neighbors or when intermediate routes are broken.
* Queues payloads in `QueuedPacketDao`.
* On receiving `TransportEvent.PeerConnected`, triggers `drainQueueForDestination()` to automatically dispatch buffered messages.

### 10.5 Presentation Layer (Jetpack Compose UI)
* **`HomeScreen.kt`**: Main dashboard featuring mesh power toggle, local node credentials with 1-click clipboard copying, connection status badges, and quick-action navigation cards.
* **`ChatScreen.kt`**: Bubble-styled chat interface with dynamic status indicators:
  * Single checkmark (`✓ Sent to Peer`)
  * Double checkmark (`✓✓ Delivered (N hops)`)
  * Expandable route trace card showing: `NODE-A → NODE-B → NODE-C → NODE-D`.
  * Interactive TTL configuration slider (1 to 15 hops).
* **`NetworkTopologyScreen.kt`**: Real-time 2D network visualizer built with Compose `Canvas`:
  * Renders the local node at center with glowing cyan pulse.
  * Arranges active cluster peers radially.
  * Draws solid green vector lines for direct 1-hop links and dashed purple lines for multi-hop routes.
* **`DemoScreen.kt`**: Lab testing suite with 1-click preset identity buttons (`A`, `B`, `C`, `D`), multi-hop simulation injection, and live color-coded event stream.

---

## 11. Testing, Simulation & Experimental Results

### 11.1 Automated Unit Test Suite
MeshLink includes complete JVM unit test suites in `app/src/test/java/com/meshlink/mesh/`:

1. **`MeshPacketTest.kt`** (4 Test Cases):
   * `testSerializationAndDeserialization`: Validates exact JSON wire format parity across serialization cycles.
   * `testCreateForwardedPacket`: Verifies that forwarding properly decrements TTL by 1, increments `hopCount` by 1, and appends the local node ID to `routeHistory`.
   * `testDeliveryAckGeneration`: Ensures destination node correctly creates reverse ACK with target swapped, packet type set to `DELIVERY_ACK`, and `originalMessageId` preserved.
   * `testBroadcastAndTargeting`: Tests unicast vs broadcast destination validation rules.

2. **`MeshRouterTest.kt`** (4 Test Cases):
   * `testIntermediateNodeForwardsPacket`: Proves that Node B receives a packet from Node A destined for Node D and broadcasts it to neighbors excluding Node A.
   * `testLoopRejection`: Confirms that if a packet arrives containing the local node's ID in `routeHistory`, it is immediately dropped without re-broadcasting.
   * `testDeduplicationDropsSecondPacket`: Verifies that identical packets received via different paths are processed only once.
   * `testLocalDeliveryGeneratesReverseAck`: Asserts that when a packet reaches its final destination, an automated reverse ACK is dispatched.

**Unit Test Execution Result**:
```
> Task :app:testDebugUnitTest
MeshPacketTest > testSerializationAndDeserialization PASSED
MeshPacketTest > testCreateForwardedPacket PASSED
MeshPacketTest > testDeliveryAckGeneration PASSED
MeshPacketTest > testBroadcastAndTargeting PASSED
MeshRouterTest > testLoopRejection PASSED
MeshRouterTest > testIntermediateNodeForwardsPacket PASSED
MeshRouterTest > testLocalDeliveryGeneratesReverseAck PASSED
MeshRouterTest > testDeduplicationDropsSecondPacket PASSED

BUILD SUCCESSFUL: 8 tests completed, 0 failed, 100% PASS RATE.
```

---

### 11.2 Physical 4-Phone Multi-Hop Experiment (A → B → C → D)

#### Experimental Setup:
Four standard commercial Android smartphones were placed in a linear physical topology:
* **Phone 1 (Node A)**: Sender
* **Phone 2 (Node B)**: Intermediate Relay 1
* **Phone 3 (Node C)**: Intermediate Relay 2
* **Phone 4 (Node D)**: Receiver / Destination

```
[ PHONE A ]  ◄──── BLE / Wi-Fi ────►  [ PHONE B ]
(Node A)                              (Relay Node)
                                           │
                                     BLE / Wi-Fi
                                           │
                                           ▼
[ PHONE D ]  ◄──── BLE / Wi-Fi ────►  [ PHONE C ]
(Node D)                              (Relay Node)
```

#### Physical Placement:
* Phone A and Phone B placed in Room 1 (Direct distance: ~5 meters).
* Phone B and Phone C placed in Hallway (Distance: ~15 meters).
* Phone C and Phone D placed in Room 2 (Distance: ~15 meters).
* **Crucial Constraint**: Phone A is physically separated from Phone D by >35 meters and concrete walls, ensuring direct radio contact between A and D is **0% impossible**.

#### Execution Steps & Results:
1. **Identities Assigned**: Using the built-in *Demo & Routing Lab*, identities were switched to presets `A`, `B`, `C`, and `D`.
2. **Cluster Discovery**: Within 8–15 seconds, all devices established local cluster links.
3. **Payload Injection**: Phone A transmitted message: `"Urgent medical supply needed at Station Alpha"`.
4. **Relay Verification**:
   * Phone B's event log recorded: `[Forwarding] Forwarding packet: new TTL=6, Hops=1, Route=NODE-A -> NODE-B`.
   * Phone C's event log recorded: `[Forwarding] Forwarding packet: new TTL=5, Hops=2, Route=NODE-A -> NODE-B -> NODE-C`.
5. **Delivery Verification**:
   * Phone D chimed with the incoming message from `NODE A`.
   * Phone D automatically generated `DELIVERY_ACK`.
6. **Reverse ACK Propagation**:
   * ACK traveled back: $D \rightarrow C \rightarrow B \rightarrow A$.
   * On Phone A, the checkmark turned to `✓✓ Delivered (3 hops)`.
   * Expanding the route trace displayed: `NODE-A → NODE-B → NODE-C → NODE-D`.

---

## 12. College Demonstration & Viva Voce Guide (Q&A)

### Q1: Why did you choose Google Nearby Connections (`Strategy.P2P_CLUSTER`) over raw Wi-Fi Direct or raw BLE?
> **Answer**:
> - **Raw BLE**: Has a tiny Maximum Transmission Unit (MTU ~20–512 bytes) and strict connection limits (typically 3–7 devices max), making it impractical for robust multi-hop message forwarding and route history tracking.
> - **Raw Wi-Fi Direct**: Enforces an asymmetric star topology with one device forced to be the Group Owner (GO). If the GO moves or drops, the entire sub-network collapses.
> - **Nearby Connections `P2P_CLUSTER`**: Combines the best of both worlds. It uses BLE for zero-configuration, low-power background discovery, and automatically negotiates high-speed Wi-Fi Direct / Local Hotspot sockets for data transmission. It enables an M-to-N multi-point mesh where every node can simultaneously advertise and discover peers.

### Q2: How does MeshLink prevent broadcast storms in dense networks?
> **Answer**:
> 1. **Two-Tier Deduplication**: Every packet has a globally unique UUID `messageId`. Received packets are recorded in a 1,000-element in-memory LRU cache and persisted in the SQLite `processed_packets` table. Any duplicate packet arriving via an alternate path is dropped in $\le 1$ ms.
> 2. **TTL (Time-To-Live)**: Each packet starts with a default TTL (e.g., 7). Every intermediate forwarder decrements TTL by 1. When TTL reaches 1, forwarding ceases.
> 3. **Self-Loop Rejection**: Nodes inspect the packet's `routeHistory`. If the node's own ID is already present in the list, the packet is discarded immediately.
> 4. **Split-Horizon Controlled Flooding**: Packets are forwarded to neighboring peers *excluding* the endpoint interface from which the packet was received.

### Q3: How does delivery confirmation work without a central server?
> **Answer**:
> In traditional apps, delivery status (`✓✓`) is confirmed by a central database server. In MeshLink, delivery confirmation is achieved via an autonomous **Reverse Delivery ACK**:
> 1. When the destination node determines that `destinationId == localNodeId`, it consumes the payload and calls `createDeliveryAck()`.
> 2. It generates a lightweight `DELIVERY_ACK` packet with `destinationId = original.senderId` and `originalMessageId = original.messageId`.
> 3. The ACK packet traverses the reverse mesh path back to the originator.
> 4. When the originator receives this ACK, Room DB updates the message status to `MessageStatus.DELIVERED` and stores the verified hop count and route path for display.

### Q4: What happens if an intermediate node moves out of range while a message is in transit?
> **Answer**:
> MeshLink implements **Delay-Tolerant Networking (DTN)** principles through the `StoreAndForwardManager`:
> - If an outgoing or forwarded message cannot find an active next-hop neighbor, it is saved in the SQLite `queued_packets` table.
> - When new peers connect or intermediate relays re-enter physical radio proximity, the `PeerConnected` event triggers `drainQueueForDestination()`, resending the buffered packets automatically.

### Q5: How is battery consumption optimized in an ad-hoc mesh network?
> **Answer**:
> 1. **Radio Duty-Cycling**: Nearby Connections handles low-power BLE duty-cycling for continuous beaconing and only powers up the high-power Wi-Fi radio when active payload streaming occurs.
> 2. **Controlled Flooding**: Packets are dropped early via LRU deduplication and TTL expiration, preventing unnecessary radio wake-locks.
> 3. **Foreground Service Management**: Mesh operations run inside an Android Foreground Service with an ongoing notification, preventing Android's aggressive OS battery optimization (Doze mode) from randomly killing transport sockets.

### Q6: What are the security considerations and how can encryption be added?
> **Answer**:
> While Nearby Connections automatically encrypts physical point-to-point links using Bluetooth/Wi-Fi link-layer encryption with authentication tokens, true end-to-end multi-hop confidentiality requires application-layer cryptography.
> - **Roadmap**: Integrating the **Signal Protocol** or **X3DH + Double Ratchet Algorithm** with Curve25519 public keys.
> - Each node will maintain an asymmetric public/private keypair. The payload string will be encrypted using the recipient's public key so intermediate relay nodes ($B$ and $C$) forward only ciphertext without being able to inspect or tamper with message contents.

---

## 13. Limitations & Future Enhancements

### 13.1 Current Limitations
* **Physical RF Range**: Constrained by smartphone Wi-Fi / Bluetooth hardware (~30–80 meters outdoors line-of-sight, ~15–25 meters indoors through concrete walls).
* **OS-Level Wi-Fi Constraints**: Android limits the maximum number of concurrent Wi-Fi Direct socket connections (typically 4–8 active links per device depending on hardware chipset).
* **Broadcast Overhead**: In very high-density clusters (>50 nodes in one room), flooding causes channel contention and packet latency spikes.

### 13.2 Future Scope & Research Directions
1. **End-to-End Asymmetric Encryption**: Integrating Curve25519 / AES-GCM-256 for zero-knowledge multi-hop message privacy.
2. **Hybrid Long-Range LoRa Bridging**: Connecting an external sub-GHz LoRa transceiver (e.g., SX1262 via USB OTG or BLE) to allow messages to hop kilometers across mountain ranges and disaster valleys.
3. **Chunked Multimedia & Voice Notes**: Splitting voice notes and compressed images into indexed chunks with sliding-window reassembly.
4. **Geocast & Location-Aware Routing**: Routing emergency distress beacons towards specific GPS coordinates rather than node IDs.

---

## 14. References

1. **Google Developers**: *Nearby Connections API Overview & P2P Cluster Strategy Specification*, Google LLC, 2024.
2. **Perkins, C., Belding-Royer, E., & Das, S.**: *Ad hoc On-Demand Distance Vector (AODV) Routing*, RFC 3561, IETF Network Working Group, 2003.
3. **Fall, K.**: *A Delay-Tolerant Network Architecture for Challenged Internets*, ACM SIGCOMM Computer Communication Review, 2003.
4. **Android Open Source Project (AOSP)**: *Android Architecture Guidelines & Jetpack Compose Documentation*, Google LLC, 2024.
5. **Vahdat, A., & Becker, D.**: *Epidemic Routing for Partially Connected Ad Hoc Networks*, Duke University Technical Report, 2000.
6. **Basagni, S., Conti, M., Giordano, S., & Stojmenovic, I.**: *Mobile Ad Hoc Networking*, IEEE Press, John Wiley & Sons, 2004.

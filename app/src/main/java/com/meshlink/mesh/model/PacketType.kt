package com.meshlink.mesh.model

import kotlinx.serialization.Serializable

@Serializable
enum class PacketType {
    CHAT,           // Direct or multi-hop user text message
    DELIVERY_ACK,   // Delivery acknowledgement returned along reverse route or broadcast
    ROUTE_REQ,      // Route discovery request
    ROUTE_RESP,     // Route discovery response
    PEER_ANNOUNCE,  // Node presence announcement to neighborhood
    PING            // Latency and link alive check
}

package com.meshlink.mesh.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionState {
    DISCOVERED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

@Serializable
data class MeshNode(
    val nodeId: String,
    val alias: String,
    val endpointId: String? = null,
    val connectionState: ConnectionState = ConnectionState.DISCOVERED,
    val isDirectNeighbor: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val hopDistance: Int = 1,
    val viaNodeId: String? = null,
    val rssi: Int? = null
)

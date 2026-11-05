package com.meshlink.mesh.transport

import com.meshlink.mesh.model.TransportEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean abstraction interface for peer-to-peer mesh transport.
 */
interface MeshTransport {
    val events: SharedFlow<TransportEvent>
    val isAdvertising: StateFlow<Boolean>
    val isDiscovering: StateFlow<Boolean>

    suspend fun startAdvertising(nodeId: String, nodeAlias: String): Boolean
    suspend fun startDiscovery(): Boolean
    suspend fun stopAdvertising()
    suspend fun stopDiscovery()
    suspend fun connectToPeer(endpointId: String): Boolean
    suspend fun disconnectPeer(endpointId: String)
    suspend fun sendBytes(endpointId: String, bytes: ByteArray): Boolean
    suspend fun broadcastBytes(bytes: ByteArray, excludeEndpointId: String? = null): Int
    fun stopAll()
}

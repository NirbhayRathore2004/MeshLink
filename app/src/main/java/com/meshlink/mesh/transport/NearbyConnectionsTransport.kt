package com.meshlink.mesh.transport

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.meshlink.mesh.model.TransportEvent
import com.meshlink.util.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class NearbyConnectionsTransport(
    private val context: Context,
    private val serviceId: String = "com.meshlink.mesh.CLUSTER_SERVICE"
) : MeshTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(context.applicationContext)
    }

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<TransportEvent> = _events.asSharedFlow()

    private val _isAdvertising = MutableStateFlow(false)
    override val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    override val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Active connected endpoints map: endpointId -> endpointName
    private val connectedEndpoints = ConcurrentHashMap<String, String>()
    // Discovered endpoints map: endpointId -> endpointName
    private val discoveredEndpoints = ConcurrentHashMap<String, String>()

    private var localNodeName: String = "NODE-UNKNOWN"
    private var localNodeId: String = ""

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                MeshLogger.d(MeshLogger.TAG_TRANSPORT, "Received ${bytes.size} bytes from endpoint $endpointId")
                scope.launch {
                    _events.emit(TransportEvent.DataReceived(endpointId, bytes))
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Can be used to monitor chunked transfer progress
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Connection initiated with endpoint $endpointId (${info.endpointName})")
            discoveredEndpoints[endpointId] = info.endpointName
            scope.launch {
                _events.emit(
                    TransportEvent.ConnectionInitiated(
                        endpointId = endpointId,
                        endpointName = info.endpointName,
                        authenticationDigits = info.authenticationDigits
                    )
                )
            }
            // Auto-accept connection in cluster mesh mode
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    MeshLogger.d(MeshLogger.TAG_TRANSPORT, "Auto-accepted connection with $endpointId")
                }
                .addOnFailureListener { e ->
                    MeshLogger.e(MeshLogger.TAG_TRANSPORT, "Failed to accept connection with $endpointId", e)
                }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    val name = discoveredEndpoints[endpointId] ?: "Unknown Peer"
                    connectedEndpoints[endpointId] = name
                    MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Connected to endpoint: $endpointId ($name)")
                    scope.launch {
                        _events.emit(TransportEvent.PeerConnected(endpointId, name))
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    discoveredEndpoints.remove(endpointId)
                    MeshLogger.w(MeshLogger.TAG_TRANSPORT, "Connection rejected by endpoint: $endpointId")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    discoveredEndpoints.remove(endpointId)
                    MeshLogger.e(MeshLogger.TAG_TRANSPORT, "Connection error with endpoint: $endpointId (code: ${resolution.status.statusCode})")
                }
                else -> {
                    discoveredEndpoints.remove(endpointId)
                    MeshLogger.w(MeshLogger.TAG_TRANSPORT, "Connection finished with status: ${resolution.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            val name = connectedEndpoints.remove(endpointId) ?: discoveredEndpoints.remove(endpointId) ?: "Unknown"
            discoveredEndpoints.remove(endpointId)
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Disconnected from endpoint: $endpointId ($name)")
            scope.launch {
                _events.emit(TransportEvent.PeerDisconnected(endpointId))
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Discovered endpoint: $endpointId (${info.endpointName})")
            discoveredEndpoints[endpointId] = info.endpointName
            scope.launch {
                _events.emit(
                    TransportEvent.PeerDiscovered(
                        endpointId = endpointId,
                        endpointName = info.endpointName,
                        serviceId = info.serviceId
                    )
                )
                // Auto connect to build mesh topology
                connectToPeerInternal(endpointId)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            val name = discoveredEndpoints.remove(endpointId)
            MeshLogger.d(MeshLogger.TAG_TRANSPORT, "Lost endpoint: $endpointId ($name)")
            scope.launch {
                _events.emit(TransportEvent.PeerLost(endpointId))
            }
        }
    }

    override suspend fun startAdvertising(nodeId: String, nodeAlias: String): Boolean {
        this.localNodeId = nodeId
        this.localNodeName = "$nodeAlias|$nodeId"
        MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Starting Nearby Advertising as: $localNodeName")
        return try {
            val options = AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
            connectionsClient.startAdvertising(
                localNodeName,
                serviceId,
                connectionLifecycleCallback,
                options
            ).await()
            _isAdvertising.value = true
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Nearby Advertising started successfully")
            true
        } catch (e: Exception) {
            MeshLogger.e(MeshLogger.TAG_TRANSPORT, "Failed to start Nearby Advertising: ${e.message}", e)
            _isAdvertising.value = false
            false
        }
    }

    override suspend fun startDiscovery(): Boolean {
        MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Starting Nearby Discovery (Strategy.P2P_CLUSTER)")
        return try {
            val options = DiscoveryOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
            connectionsClient.startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                options
            ).await()
            _isDiscovering.value = true
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Nearby Discovery started successfully")
            true
        } catch (e: Exception) {
            MeshLogger.e(MeshLogger.TAG_TRANSPORT, "Failed to start Nearby Discovery: ${e.message}", e)
            _isDiscovering.value = false
            false
        }
    }

    override suspend fun stopAdvertising() {
        try {
            connectionsClient.stopAdvertising()
            _isAdvertising.value = false
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Nearby Advertising stopped")
        } catch (e: Exception) {
            MeshLogger.e(MeshLogger.TAG_TRANSPORT, "Error stopping advertising", e)
        }
    }

    override suspend fun stopDiscovery() {
        try {
            connectionsClient.stopDiscovery()
            _isDiscovering.value = false
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Nearby Discovery stopped")
        } catch (e: Exception) {
            MeshLogger.e(MeshLogger.TAG_TRANSPORT, "Error stopping discovery", e)
        }
    }

    override suspend fun connectToPeer(endpointId: String): Boolean {
        return connectToPeerInternal(endpointId)
    }

    private suspend fun connectToPeerInternal(endpointId: String): Boolean {
        if (connectedEndpoints.containsKey(endpointId)) {
            return true
        }
        MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Requesting connection to endpoint $endpointId")
        return try {
            connectionsClient.requestConnection(
                localNodeName,
                endpointId,
                connectionLifecycleCallback
            ).await()
            MeshLogger.d(MeshLogger.TAG_TRANSPORT, "Connection request sent to $endpointId")
            true
        } catch (e: Exception) {
            MeshLogger.w(MeshLogger.TAG_TRANSPORT, "Failed to request connection to $endpointId: ${e.message}")
            false
        }
    }

    override suspend fun disconnectPeer(endpointId: String) {
        try {
            connectionsClient.disconnectFromEndpoint(endpointId)
            connectedEndpoints.remove(endpointId)
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Disconnected from endpoint $endpointId")
        } catch (e: Exception) {
            MeshLogger.e(MeshLogger.TAG_TRANSPORT, "Error disconnecting from $endpointId", e)
        }
    }

    override suspend fun sendBytes(endpointId: String, bytes: ByteArray): Boolean {
        return try {
            val payload = Payload.fromBytes(bytes)
            connectionsClient.sendPayload(endpointId, payload).await()
            MeshLogger.d(MeshLogger.TAG_TRANSPORT, "Sent ${bytes.size} bytes to endpoint $endpointId")
            true
        } catch (e: Exception) {
            MeshLogger.e(MeshLogger.TAG_TRANSPORT, "Failed to send bytes to $endpointId: ${e.message}", e)
            false
        }
    }

    override suspend fun broadcastBytes(bytes: ByteArray, excludeEndpointId: String?): Int {
        var successCount = 0
        val targets = connectedEndpoints.keys.filter { it != excludeEndpointId }
        if (targets.isEmpty()) {
            MeshLogger.d(MeshLogger.TAG_TRANSPORT, "No connected endpoints to broadcast to (excluding: $excludeEndpointId)")
            return 0
        }
        for (endpointId in targets) {
            val success = sendBytes(endpointId, bytes)
            if (success) successCount++
        }
        MeshLogger.i(MeshLogger.TAG_TRANSPORT, "Broadcasted packet to $successCount/${targets.size} direct peers")
        return successCount
    }

    override fun stopAll() {
        try {
            connectionsClient.stopAllEndpoints()
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectedEndpoints.clear()
            discoveredEndpoints.clear()
            _isAdvertising.value = false
            _isDiscovering.value = false
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "All Nearby Connections endpoints stopped and reset")
        } catch (e: Exception) {
            MeshLogger.e(MeshLogger.TAG_TRANSPORT, "Error during stopAll", e)
        }
    }
}

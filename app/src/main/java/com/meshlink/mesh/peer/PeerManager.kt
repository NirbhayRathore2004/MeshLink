package com.meshlink.mesh.peer

import com.meshlink.data.database.dao.PeerDao
import com.meshlink.data.database.entity.PeerEntity
import com.meshlink.mesh.model.ConnectionState
import com.meshlink.mesh.model.MeshNode
import com.meshlink.mesh.model.TransportEvent
import com.meshlink.util.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PeerManager(
    private val peerDao: PeerDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Maps endpointId -> NodeId
    private val endpointToNodeMap = ConcurrentHashMap<String, String>()
    // Maps NodeId -> endpointId (for direct neighbors)
    private val nodeToEndpointMap = ConcurrentHashMap<String, String>()

    // In-memory list of active nodes
    private val _activePeers = MutableStateFlow<Map<String, MeshNode>>(emptyMap())
    val activePeers: StateFlow<Map<String, MeshNode>> = _activePeers.asStateFlow()

    init {
        scope.launch {
            peerDao.markAllDisconnected()
        }
    }

    fun handleTransportEvent(event: TransportEvent) {
        when (event) {
            is TransportEvent.PeerDiscovered -> {
                val (alias, nodeId) = parseEndpointName(event.endpointName, event.endpointId)
                endpointToNodeMap[event.endpointId] = nodeId
                updatePeerDiscovered(nodeId, alias, event.endpointId)
            }
            is TransportEvent.PeerConnected -> {
                val (alias, nodeId) = parseEndpointName(event.endpointName, event.endpointId)
                endpointToNodeMap[event.endpointId] = nodeId
                nodeToEndpointMap[nodeId] = event.endpointId
                updatePeerConnected(nodeId, alias, event.endpointId)
            }
            is TransportEvent.PeerDisconnected -> {
                val nodeId = endpointToNodeMap.remove(event.endpointId)
                if (nodeId != null) {
                    nodeToEndpointMap.remove(nodeId)
                    updatePeerDisconnected(nodeId, event.endpointId)
                }
            }
            is TransportEvent.PeerLost -> {
                val nodeId = endpointToNodeMap.remove(event.endpointId)
                if (nodeId != null) {
                    _activePeers.update { it - nodeId }
                }
            }
            else -> { /* Other events handled by router */ }
        }
    }

    fun registerMultiHopPeer(nodeId: String, alias: String, hopDistance: Int, viaNodeId: String) {
        _activePeers.update { current ->
            val existing = current[nodeId]
            // If the peer is already a direct neighbor, preserve that direct status
            val isDirect = existing?.isDirectNeighbor ?: (hopDistance <= 1)
            val updated = existing?.copy(
                alias = alias,
                lastSeenTimestamp = System.currentTimeMillis(),
                hopDistance = if (isDirect) 1 else hopDistance,
                viaNodeId = if (isDirect) null else viaNodeId,
                isDirectNeighbor = isDirect
            ) ?: MeshNode(
                nodeId = nodeId,
                alias = alias,
                connectionState = ConnectionState.CONNECTED,
                isDirectNeighbor = isDirect,
                lastSeenTimestamp = System.currentTimeMillis(),
                hopDistance = hopDistance,
                viaNodeId = viaNodeId
            )
            current + (nodeId to updated)
        }

        scope.launch {
            peerDao.insertOrUpdatePeer(
                PeerEntity(
                    nodeId = nodeId,
                    alias = alias,
                    endpointId = null,
                    isConnected = true,
                    isDirectNeighbor = false,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    hopDistance = hopDistance,
                    viaNodeId = viaNodeId
                )
            )
        }
    }

    private fun updatePeerDiscovered(nodeId: String, alias: String, endpointId: String) {
        _activePeers.update { current ->
            val existing = current[nodeId]
            val updated = existing?.copy(
                alias = alias,
                endpointId = endpointId,
                lastSeenTimestamp = System.currentTimeMillis()
            ) ?: MeshNode(
                nodeId = nodeId,
                alias = alias,
                endpointId = endpointId,
                connectionState = ConnectionState.DISCOVERED,
                isDirectNeighbor = true
            )
            current + (nodeId to updated)
        }

        scope.launch {
            peerDao.insertOrUpdatePeer(
                PeerEntity(
                    nodeId = nodeId,
                    alias = alias,
                    endpointId = endpointId,
                    isConnected = false,
                    isDirectNeighbor = true,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    hopDistance = 1
                )
            )
        }
    }

    private fun updatePeerConnected(nodeId: String, alias: String, endpointId: String) {
        MeshLogger.i(MeshLogger.TAG_MESH, "PeerManager: Node $nodeId ($alias) connected via endpoint $endpointId")
        _activePeers.update { current ->
            val node = MeshNode(
                nodeId = nodeId,
                alias = alias,
                endpointId = endpointId,
                connectionState = ConnectionState.CONNECTED,
                isDirectNeighbor = true,
                lastSeenTimestamp = System.currentTimeMillis(),
                hopDistance = 1
            )
            current + (nodeId to node)
        }

        scope.launch {
            peerDao.insertOrUpdatePeer(
                PeerEntity(
                    nodeId = nodeId,
                    alias = alias,
                    endpointId = endpointId,
                    isConnected = true,
                    isDirectNeighbor = true,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    hopDistance = 1
                )
            )
        }
    }

    private fun updatePeerDisconnected(nodeId: String, endpointId: String) {
        MeshLogger.i(MeshLogger.TAG_MESH, "PeerManager: Node $nodeId disconnected from endpoint $endpointId")
        _activePeers.update { current ->
            val existing = current[nodeId]
            if (existing != null) {
                current + (nodeId to existing.copy(connectionState = ConnectionState.DISCONNECTED))
            } else {
                current
            }
        }

        scope.launch {
            peerDao.updateConnectionState(nodeId, false)
        }
    }

    fun getEndpointIdForNode(nodeId: String): String? {
        return nodeToEndpointMap[nodeId]
    }

    fun getNodeIdForEndpoint(endpointId: String): String? {
        return endpointToNodeMap[endpointId]
    }

    fun getDirectNeighbors(): List<MeshNode> {
        return _activePeers.value.values.filter { it.isDirectNeighbor && it.connectionState == ConnectionState.CONNECTED }
    }

    fun getAllActiveNodes(): List<MeshNode> {
        return _activePeers.value.values.toList()
    }

    private fun parseEndpointName(endpointName: String, fallbackEndpointId: String): Pair<String, String> {
        return if (endpointName.contains("|")) {
            val parts = endpointName.split("|", limit = 2)
            val alias = parts[0].ifBlank { "Node" }
            val nodeId = parts[1].ifBlank { fallbackEndpointId }
            Pair(alias, nodeId)
        } else {
            Pair(endpointName.ifBlank { "Node-$fallbackEndpointId" }, fallbackEndpointId)
        }
    }

    fun clear() {
        endpointToNodeMap.clear()
        nodeToEndpointMap.clear()
        _activePeers.value = emptyMap()
        scope.launch {
            peerDao.markAllDisconnected()
        }
    }
}

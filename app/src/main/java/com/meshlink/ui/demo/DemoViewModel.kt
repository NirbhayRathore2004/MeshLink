package com.meshlink.ui.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meshlink.data.repository.MessageRepository
import com.meshlink.data.repository.PeerRepository
import com.meshlink.di.AppContainer
import com.meshlink.mesh.identity.NodeIdentityManager
import com.meshlink.mesh.model.MeshPacket
import com.meshlink.mesh.model.PacketType
import com.meshlink.mesh.routing.MeshRouter
import com.meshlink.util.LogEntry
import com.meshlink.util.MeshLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DemoUiState(
    val currentNodeId: String = "",
    val currentNodeAlias: String = "",
    val logs: List<LogEntry> = emptyList(),
    val isSimulationRunning: Boolean = false
)

class DemoViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val identityManager: NodeIdentityManager = appContainer.identityManager
    private val meshRouter: MeshRouter = appContainer.meshRouter
    private val messageRepository: MessageRepository = appContainer.messageRepository
    private val peerRepository: PeerRepository = appContainer.peerRepository

    val uiState: StateFlow<DemoUiState> = combine(
        identityManager.nodeId,
        identityManager.nodeAlias,
        MeshLogger.logs
    ) { nodeId, alias, logs ->
        DemoUiState(
            currentNodeId = nodeId,
            currentNodeAlias = alias,
            logs = logs
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DemoUiState()
    )

    fun selectPresetIdentity(preset: String) {
        appContainer.restartMeshWithNewIdentity(preset, preset.replace("-", " "))
    }

    fun dispatch4NodeHopSimulation() {
        viewModelScope.launch {
            MeshLogger.i(MeshLogger.TAG_MESH, "--- INITIATING 4-NODE MULTI-HOP LAB SIMULATION ---")
            val currentId = identityManager.nodeId.value
            val currentAlias = identityManager.nodeAlias.value

            // Simulate message from NODE-A targeting NODE-D with initial hop path
            val packet = MeshPacket(
                messageId = UUID.randomUUID().toString(),
                senderId = "NODE-A",
                senderAlias = "NODE A",
                destinationId = "NODE-D",
                timestamp = System.currentTimeMillis(),
                ttl = 7,
                hopCount = 0,
                payload = "Hello Node D! This message travelled offline via Node B and Node C.",
                packetType = PacketType.CHAT,
                routeHistory = listOf("NODE-A")
            )

            MeshLogger.i(MeshLogger.TAG_ROUTING, "Node A created packet ${packet.messageId.take(8)} -> Target: NODE-D")
            meshRouter.processIncomingPacket(packet, incomingEndpointId = "mock-endpoint-b")
        }
    }

    fun sendBroadcastPing() {
        viewModelScope.launch {
            val localId = identityManager.nodeId.value
            val localAlias = identityManager.nodeAlias.value
            val pingPacket = MeshPacket(
                messageId = UUID.randomUUID().toString(),
                senderId = localId,
                senderAlias = localAlias,
                destinationId = MeshPacket.BROADCAST_DESTINATION,
                packetType = PacketType.PING,
                payload = "PING_ALL",
                ttl = 4,
                hopCount = 0,
                routeHistory = listOf(localId)
            )
            MeshLogger.i(MeshLogger.TAG_MESH, "Broadcasting Mesh Ping to all cluster nodes...")
            meshRouter.sendPacket(pingPacket)
        }
    }

    fun resetAllState() {
        viewModelScope.launch {
            MeshLogger.w(MeshLogger.TAG_MESH, "Resetting local database, routes, and packet caches...")
            messageRepository.clearAll()
            peerRepository.clearAll()
            meshRouter.clearAllState()
            MeshLogger.clear()
            MeshLogger.i(MeshLogger.TAG_MESH, "MeshLink state clean and reset.")
        }
    }

    class Factory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DemoViewModel(appContainer) as T
        }
    }
}

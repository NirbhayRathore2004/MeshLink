package com.meshlink.di

import android.content.Context
import com.meshlink.data.database.MeshDatabase
import com.meshlink.data.repository.MessageRepository
import com.meshlink.data.repository.PeerRepository
import com.meshlink.mesh.forwarding.StoreAndForwardManager
import com.meshlink.mesh.identity.NodeIdentityManager
import com.meshlink.mesh.peer.PeerManager
import com.meshlink.mesh.routing.MeshRouter
import com.meshlink.mesh.transport.MeshTransport
import com.meshlink.mesh.transport.NearbyConnectionsTransport
import com.meshlink.util.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppContainer(val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: MeshDatabase by lazy {
        MeshDatabase.getDatabase(context)
    }

    val identityManager: NodeIdentityManager by lazy {
        NodeIdentityManager(context)
    }

    val transport: MeshTransport by lazy {
        NearbyConnectionsTransport(context)
    }

    val peerManager: PeerManager by lazy {
        PeerManager(database.peerDao())
    }

    val storeAndForwardManager: StoreAndForwardManager by lazy {
        StoreAndForwardManager(database.queuedPacketDao())
    }

    val meshRouter: MeshRouter by lazy {
        MeshRouter(
            transport = transport,
            peerManager = peerManager,
            identityManager = identityManager,
            storeAndForwardManager = storeAndForwardManager,
            messageDao = database.messageDao(),
            conversationDao = database.conversationDao(),
            routeDao = database.routeDao(),
            processedPacketDao = database.processedPacketDao()
        )
    }

    val messageRepository: MessageRepository by lazy {
        MessageRepository(
            messageDao = database.messageDao(),
            conversationDao = database.conversationDao(),
            meshRouter = meshRouter
        )
    }

    val peerRepository: PeerRepository by lazy {
        PeerRepository(
            peerDao = database.peerDao(),
            routeDao = database.routeDao(),
            peerManager = peerManager
        )
    }

    private val _isMeshActive = MutableStateFlow(false)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    fun startMesh() {
        scope.launch {
            val nodeId = identityManager.nodeId.value
            val alias = identityManager.nodeAlias.value
            MeshLogger.i(MeshLogger.TAG_MESH, "Starting MeshLink Engine as $alias ($nodeId)...")

            // Ensure router is initialized
            val router = meshRouter

            val advStarted = transport.startAdvertising(nodeId, alias)
            val discStarted = transport.startDiscovery()
            _isMeshActive.value = advStarted || discStarted
            MeshLogger.i(MeshLogger.TAG_MESH, "MeshLink Engine started (Active: ${_isMeshActive.value})")
        }
    }

    fun stopMesh() {
        scope.launch {
            MeshLogger.i(MeshLogger.TAG_MESH, "Stopping MeshLink Engine...")
            transport.stopAll()
            _isMeshActive.value = false
            MeshLogger.i(MeshLogger.TAG_MESH, "MeshLink Engine stopped")
        }
    }

    fun restartMeshWithNewIdentity(newId: String, newAlias: String) {
        scope.launch {
            stopMesh()
            identityManager.setCustomIdentity(newId, newAlias)
            meshRouter.clearAllState()
            peerRepository.clearAll()
            startMesh()
        }
    }
}

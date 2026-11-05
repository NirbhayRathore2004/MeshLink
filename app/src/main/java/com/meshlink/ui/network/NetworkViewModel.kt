package com.meshlink.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meshlink.data.database.entity.RouteEntity
import com.meshlink.data.repository.PeerRepository
import com.meshlink.di.AppContainer
import com.meshlink.mesh.identity.NodeIdentityManager
import com.meshlink.mesh.model.MeshNode
import com.meshlink.mesh.model.MeshPacket
import com.meshlink.mesh.routing.MeshRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NetworkTopologyUiState(
    val localNodeId: String = "",
    val localNodeAlias: String = "",
    val activeNodes: List<MeshNode> = emptyList(),
    val routes: List<RouteEntity> = emptyList(),
    val latestRoutedPacket: MeshPacket? = null
)

class NetworkViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val identityManager: NodeIdentityManager = appContainer.identityManager
    private val peerRepository: PeerRepository = appContainer.peerRepository
    private val meshRouter: MeshRouter = appContainer.meshRouter

    private val _latestRoutedPacket = MutableStateFlow<MeshPacket?>(null)

    init {
        viewModelScope.launch {
            meshRouter.routedPackets.collect { packet ->
                _latestRoutedPacket.value = packet
            }
        }
    }

    val uiState: StateFlow<NetworkTopologyUiState> = combine(
        identityManager.nodeId,
        identityManager.nodeAlias,
        peerRepository.activePeers,
        peerRepository.getAllRoutes(),
        _latestRoutedPacket
    ) { nodeId, alias, peersMap, routes, packet ->
        NetworkTopologyUiState(
            localNodeId = nodeId,
            localNodeAlias = alias,
            activeNodes = peersMap.values.toList(),
            routes = routes,
            latestRoutedPacket = packet
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NetworkTopologyUiState()
    )

    class Factory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NetworkViewModel(appContainer) as T
        }
    }
}

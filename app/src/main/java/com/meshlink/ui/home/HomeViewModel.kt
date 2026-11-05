package com.meshlink.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meshlink.data.database.entity.PeerEntity
import com.meshlink.data.database.entity.RouteEntity
import com.meshlink.data.repository.MessageRepository
import com.meshlink.data.repository.PeerRepository
import com.meshlink.di.AppContainer
import com.meshlink.mesh.identity.NodeIdentityManager
import com.meshlink.service.MeshService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val nodeId: String = "",
    val nodeAlias: String = "",
    val isMeshActive: Boolean = false,
    val connectedPeersCount: Int = 0,
    val totalDiscoveredNodesCount: Int = 0,
    val activeRoutesCount: Int = 0
)

class HomeViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val identityManager: NodeIdentityManager = appContainer.identityManager
    private val peerRepository: PeerRepository = appContainer.peerRepository

    val uiState: StateFlow<HomeUiState> = combine(
        identityManager.nodeId,
        identityManager.nodeAlias,
        appContainer.isMeshActive,
        peerRepository.getConnectedPeers(),
        peerRepository.getAllPeers()
    ) { nodeId: String, alias: String, isActive: Boolean, connectedPeers: List<PeerEntity>, allPeers: List<PeerEntity> ->
        HomeUiState(
            nodeId = nodeId,
            nodeAlias = alias,
            isMeshActive = isActive,
            connectedPeersCount = connectedPeers.size,
            totalDiscoveredNodesCount = allPeers.size,
            activeRoutesCount = 0
        )
    }.combine(
        peerRepository.getAllRoutes()
    ) { state: HomeUiState, routes: List<RouteEntity> ->
        state.copy(
            activeRoutesCount = routes.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = HomeUiState()
    )

    fun toggleMesh(context: Context) {
        if (uiState.value.isMeshActive) {
            MeshService.stop(context)
        } else {
            MeshService.start(context)
        }
    }

    fun updateAlias(newAlias: String) {
        identityManager.setNodeAlias(newAlias)
    }

    class Factory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(appContainer) as T
        }
    }
}

package com.meshlink.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meshlink.data.database.entity.PeerEntity
import com.meshlink.data.repository.PeerRepository
import com.meshlink.di.AppContainer
import com.meshlink.mesh.model.MeshNode
import com.meshlink.mesh.transport.MeshTransport
import com.meshlink.util.MeshLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DevicesUiState(
    val activeNodes: List<MeshNode> = emptyList(),
    val isAdvertising: Boolean = false,
    val isDiscovering: Boolean = false
)

class DevicesViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val peerRepository: PeerRepository = appContainer.peerRepository
    private val transport: MeshTransport = appContainer.transport

    val uiState: StateFlow<DevicesUiState> = combine(
        peerRepository.activePeers,
        transport.isAdvertising,
        transport.isDiscovering
    ) { activePeersMap, isAdv, isDisc ->
        DevicesUiState(
            activeNodes = activePeersMap.values.toList().sortedWith(
                compareByDescending<MeshNode> { it.connectionState.ordinal }
                    .thenBy { it.hopDistance }
            ),
            isAdvertising = isAdv,
            isDiscovering = isDisc
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DevicesUiState()
    )

    fun connectToEndpoint(endpointId: String) {
        viewModelScope.launch {
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "User triggered manual connection to endpoint: $endpointId")
            transport.connectToPeer(endpointId)
        }
    }

    fun disconnectEndpoint(endpointId: String) {
        viewModelScope.launch {
            MeshLogger.i(MeshLogger.TAG_TRANSPORT, "User triggered manual disconnect from endpoint: $endpointId")
            transport.disconnectPeer(endpointId)
        }
    }

    fun restartDiscovery() {
        viewModelScope.launch {
            transport.stopDiscovery()
            transport.startDiscovery()
        }
    }

    class Factory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DevicesViewModel(appContainer) as T
        }
    }
}

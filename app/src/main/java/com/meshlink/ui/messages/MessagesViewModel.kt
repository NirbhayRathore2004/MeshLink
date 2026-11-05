package com.meshlink.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meshlink.data.database.entity.ConversationEntity
import com.meshlink.data.database.entity.PeerEntity
import com.meshlink.data.repository.MessageRepository
import com.meshlink.data.repository.PeerRepository
import com.meshlink.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MessagesUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val availablePeers: List<PeerEntity> = emptyList()
)

class MessagesViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val messageRepository: MessageRepository = appContainer.messageRepository
    private val peerRepository: PeerRepository = appContainer.peerRepository

    val uiState: StateFlow<MessagesUiState> = combine(
        messageRepository.getConversations(),
        peerRepository.getAllPeers()
    ) { conversations, peers ->
        MessagesUiState(
            conversations = conversations,
            availablePeers = peers
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MessagesUiState()
    )

    fun deleteConversation(peerId: String) {
        viewModelScope.launch {
            messageRepository.deleteConversation(peerId)
        }
    }

    class Factory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessagesViewModel(appContainer) as T
        }
    }
}

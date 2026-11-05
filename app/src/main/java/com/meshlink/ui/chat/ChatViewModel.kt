package com.meshlink.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meshlink.data.database.entity.MessageEntity
import com.meshlink.data.repository.MessageRepository
import com.meshlink.di.AppContainer
import com.meshlink.mesh.model.MeshPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val peerId: String = "",
    val peerAlias: String = "",
    val messages: List<MessageEntity> = emptyList(),
    val inputText: String = "",
    val ttl: Int = MeshPacket.DEFAULT_TTL,
    val isSending: Boolean = false
)

class ChatViewModel(
    private val peerId: String,
    private val peerAlias: String,
    private val appContainer: AppContainer
) : ViewModel() {

    private val messageRepository: MessageRepository = appContainer.messageRepository

    private val _inputText = MutableStateFlow("")
    private val _ttl = MutableStateFlow(MeshPacket.DEFAULT_TTL)
    private val _isSending = MutableStateFlow(false)

    val uiState: StateFlow<ChatUiState> = combine(
        messageRepository.getMessages(peerId),
        _inputText,
        _ttl,
        _isSending
    ) { messages, text, ttl, sending ->
        ChatUiState(
            peerId = peerId,
            peerAlias = peerAlias,
            messages = messages,
            inputText = text,
            ttl = ttl,
            isSending = sending
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ChatUiState(peerId = peerId, peerAlias = peerAlias)
    )

    init {
        viewModelScope.launch {
            messageRepository.getMessages(peerId).collect {
                if (it.isNotEmpty()) {
                    messageRepository.markAsRead(peerId)
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun onTtlChanged(newTtl: Int) {
        _ttl.value = newTtl.coerceIn(1, 15)
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return

        val currentTtl = _ttl.value
        _inputText.value = ""

        viewModelScope.launch {
            _isSending.value = true
            try {
                messageRepository.sendMessage(
                    destinationId = peerId,
                    destinationAlias = peerAlias,
                    text = text,
                    ttl = currentTtl
                )
            } finally {
                _isSending.value = false
            }
        }
    }

    class Factory(
        private val peerId: String,
        private val peerAlias: String,
        private val appContainer: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(peerId, peerAlias, appContainer) as T
        }
    }
}

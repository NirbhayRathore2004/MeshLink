package com.meshlink.data.repository

import com.meshlink.data.database.dao.ConversationDao
import com.meshlink.data.database.dao.MessageDao
import com.meshlink.data.database.entity.ConversationEntity
import com.meshlink.data.database.entity.MessageEntity
import com.meshlink.mesh.model.MeshPacket
import com.meshlink.mesh.routing.MeshRouter
import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val meshRouter: MeshRouter
) {
    fun getConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun sendMessage(
        destinationId: String,
        destinationAlias: String,
        text: String,
        ttl: Int = MeshPacket.DEFAULT_TTL
    ): MeshPacket {
        return meshRouter.sendChatMessage(destinationId, destinationAlias, text, ttl)
    }

    suspend fun markAsRead(conversationId: String) {
        conversationDao.markAsRead(conversationId)
    }

    suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteConversationMessages(conversationId)
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun clearAll() {
        messageDao.clearAll()
        conversationDao.clearAll()
    }
}

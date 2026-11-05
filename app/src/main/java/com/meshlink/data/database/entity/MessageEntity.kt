package com.meshlink.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meshlink.data.model.MessageStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderAlias: String,
    val destinationId: String,
    val destinationAlias: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.PENDING,
    val hopCount: Int = 0,
    val routeHistoryJson: String = "[]",
    val isOutgoing: Boolean = true
)

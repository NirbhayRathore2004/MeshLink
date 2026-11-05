package com.meshlink.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queued_packets")
data class QueuedPacketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: String,
    val destinationNodeId: String,
    val packetJson: String,
    val queuedTimestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

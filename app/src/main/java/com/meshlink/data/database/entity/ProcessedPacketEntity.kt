package com.meshlink.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_packets")
data class ProcessedPacketEntity(
    @PrimaryKey
    val messageId: String,
    val receivedTimestamp: Long = System.currentTimeMillis(),
    val originNodeId: String,
    val packetType: String
)

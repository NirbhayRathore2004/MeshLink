package com.meshlink.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey
    val nodeId: String,
    val alias: String,
    val endpointId: String? = null,
    val isConnected: Boolean = false,
    val isDirectNeighbor: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val hopDistance: Int = 1,
    val viaNodeId: String? = null
)

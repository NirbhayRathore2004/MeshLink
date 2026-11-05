package com.meshlink.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey
    val destinationNodeId: String,
    val nextHopEndpointId: String,
    val nextHopNodeId: String,
    val hopCount: Int,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

package com.meshlink.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meshlink.data.database.entity.QueuedPacketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueuedPacketDao {

    @Query("SELECT * FROM queued_packets ORDER BY queuedTimestamp ASC")
    fun getAllQueuedPackets(): Flow<List<QueuedPacketEntity>>

    @Query("SELECT * FROM queued_packets WHERE destinationNodeId = :destinationNodeId OR destinationNodeId = 'BROADCAST_ALL' ORDER BY queuedTimestamp ASC")
    suspend fun getPacketsForDestination(destinationNodeId: String): List<QueuedPacketEntity>

    @Query("SELECT * FROM queued_packets ORDER BY queuedTimestamp ASC")
    suspend fun getAllQueuedList(): List<QueuedPacketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueuedPacket(packet: QueuedPacketEntity): Long

    @Update
    suspend fun updateQueuedPacket(packet: QueuedPacketEntity)

    @Delete
    suspend fun deleteQueuedPacket(packet: QueuedPacketEntity)

    @Query("DELETE FROM queued_packets WHERE messageId = :messageId")
    suspend fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM queued_packets WHERE queuedTimestamp < :cutoffTimestamp")
    suspend fun pruneExpiredPackets(cutoffTimestamp: Long)

    @Query("DELETE FROM queued_packets")
    suspend fun clearAll()
}

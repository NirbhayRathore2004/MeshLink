package com.meshlink.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.meshlink.data.database.entity.ProcessedPacketEntity

@Dao
interface ProcessedPacketDao {

    @Query("SELECT COUNT(*) FROM processed_packets WHERE messageId = :messageId")
    suspend fun hasPacket(messageId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPacket(packet: ProcessedPacketEntity)

    @Query("DELETE FROM processed_packets WHERE receivedTimestamp < :cutoffTimestamp")
    suspend fun pruneOldPackets(cutoffTimestamp: Long)

    @Query("DELETE FROM processed_packets")
    suspend fun clearAll()
}

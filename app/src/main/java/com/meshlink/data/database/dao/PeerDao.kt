package com.meshlink.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meshlink.data.database.entity.PeerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {

    @Query("SELECT * FROM peers ORDER BY isConnected DESC, lastSeenTimestamp DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE isConnected = 1")
    fun getConnectedPeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getPeerByNodeId(nodeId: String): PeerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePeer(peer: PeerEntity)

    @Update
    suspend fun updatePeer(peer: PeerEntity)

    @Query("UPDATE peers SET isConnected = :connected WHERE nodeId = :nodeId")
    suspend fun updateConnectionState(nodeId: String, connected: Boolean)

    @Query("UPDATE peers SET isConnected = 0")
    suspend fun markAllDisconnected()

    @Query("DELETE FROM peers WHERE nodeId = :nodeId")
    suspend fun deletePeer(nodeId: String)

    @Query("DELETE FROM peers")
    suspend fun clearAll()
}

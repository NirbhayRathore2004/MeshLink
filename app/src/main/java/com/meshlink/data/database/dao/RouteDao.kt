package com.meshlink.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meshlink.data.database.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes ORDER BY hopCount ASC")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE destinationNodeId = :destinationNodeId LIMIT 1")
    suspend fun getRouteForDestination(destinationNodeId: String): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRoute(route: RouteEntity)

    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Query("DELETE FROM routes WHERE destinationNodeId = :destinationNodeId")
    suspend fun deleteRoute(destinationNodeId: String)

    @Query("DELETE FROM routes WHERE updatedTimestamp < :cutoffTimestamp")
    suspend fun pruneOldRoutes(cutoffTimestamp: Long)

    @Query("DELETE FROM routes")
    suspend fun clearAll()
}

package com.meshlink.data.repository

import com.meshlink.data.database.dao.PeerDao
import com.meshlink.data.database.dao.RouteDao
import com.meshlink.data.database.entity.PeerEntity
import com.meshlink.data.database.entity.RouteEntity
import com.meshlink.mesh.model.MeshNode
import com.meshlink.mesh.peer.PeerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class PeerRepository(
    private val peerDao: PeerDao,
    private val routeDao: RouteDao,
    private val peerManager: PeerManager
) {
    fun getAllPeers(): Flow<List<PeerEntity>> = peerDao.getAllPeers()

    fun getConnectedPeers(): Flow<List<PeerEntity>> = peerDao.getConnectedPeers()

    fun getAllRoutes(): Flow<List<RouteEntity>> = routeDao.getAllRoutes()

    val activePeers: StateFlow<Map<String, MeshNode>> = peerManager.activePeers

    suspend fun clearAll() {
        peerDao.clearAll()
        routeDao.clearAll()
        peerManager.clear()
    }
}

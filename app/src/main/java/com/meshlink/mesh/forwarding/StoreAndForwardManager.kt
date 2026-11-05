package com.meshlink.mesh.forwarding

import com.meshlink.data.database.dao.QueuedPacketDao
import com.meshlink.data.database.entity.QueuedPacketEntity
import com.meshlink.mesh.model.MeshPacket
import com.meshlink.util.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StoreAndForwardManager(
    private val queuedPacketDao: QueuedPacketDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun queuePacket(packet: MeshPacket) {
        scope.launch {
            MeshLogger.i(MeshLogger.TAG_FORWARDING, "Queuing packet ${packet.messageId} for destination ${packet.destinationId} in Store-and-Forward")
            val entity = QueuedPacketEntity(
                messageId = packet.messageId,
                destinationNodeId = packet.destinationId,
                packetJson = packet.toJson(),
                queuedTimestamp = System.currentTimeMillis(),
                retryCount = 0
            )
            queuedPacketDao.insertQueuedPacket(entity)
        }
    }

    suspend fun getQueuedPacketsForNode(nodeId: String): List<MeshPacket> {
        val list = queuedPacketDao.getPacketsForDestination(nodeId)
        return list.mapNotNull { MeshPacket.fromJson(it.packetJson) }
    }

    suspend fun getAllQueuedPackets(): List<MeshPacket> {
        val list = queuedPacketDao.getAllQueuedList()
        return list.mapNotNull { MeshPacket.fromJson(it.packetJson) }
    }

    fun removePacketFromQueue(messageId: String) {
        scope.launch {
            queuedPacketDao.deleteByMessageId(messageId)
            MeshLogger.d(MeshLogger.TAG_FORWARDING, "Removed packet $messageId from Store-and-Forward queue")
        }
    }

    fun pruneExpiredPackets(maxAgeMs: Long = 12 * 60 * 60 * 1000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        scope.launch {
            queuedPacketDao.pruneExpiredPackets(cutoff)
        }
    }
}

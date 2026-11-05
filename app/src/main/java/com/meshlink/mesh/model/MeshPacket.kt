package com.meshlink.mesh.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class MeshPacket(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderAlias: String,
    val destinationId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = DEFAULT_TTL,
    val hopCount: Int = 0,
    val payload: String = "",
    val packetType: PacketType = PacketType.CHAT,
    val routeHistory: List<String> = emptyList(),
    val originalMessageId: String? = null
) {
    companion object {
        const val BROADCAST_DESTINATION = "BROADCAST_ALL"
        const val DEFAULT_TTL = 7
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromJson(jsonString: String): MeshPacket? {
            return try {
                json.decodeFromString<MeshPacket>(jsonString)
            } catch (e: Exception) {
                null
            }
        }

        fun fromBytes(bytes: ByteArray): MeshPacket? {
            return try {
                fromJson(String(bytes, Charsets.UTF_8))
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toJson(): String {
        return json.encodeToString(this)
    }

    fun toBytes(): ByteArray {
        return toJson().toByteArray(Charsets.UTF_8)
    }

    /**
     * Creates a new forwarded packet with decremented TTL and appended route history.
     */
    fun createForwardedPacket(forwardingNodeId: String): MeshPacket {
        val updatedRoute = if (!routeHistory.contains(forwardingNodeId)) {
            routeHistory + forwardingNodeId
        } else {
            routeHistory
        }
        return copy(
            ttl = ttl - 1,
            hopCount = hopCount + 1,
            routeHistory = updatedRoute
        )
    }

    /**
     * Generates a delivery acknowledgement for this packet.
     */
    fun createDeliveryAck(acknowledgingNodeId: String, acknowledgingNodeAlias: String): MeshPacket {
        return MeshPacket(
            messageId = UUID.randomUUID().toString(),
            senderId = acknowledgingNodeId,
            senderAlias = acknowledgingNodeAlias,
            destinationId = this.senderId,
            packetType = PacketType.DELIVERY_ACK,
            payload = "ACK_DELIVERED",
            originalMessageId = this.messageId,
            ttl = DEFAULT_TTL,
            hopCount = 0,
            routeHistory = listOf(acknowledgingNodeId)
        )
    }

    fun isBroadcast(): Boolean = destinationId == BROADCAST_DESTINATION

    fun isTargetedTo(nodeId: String): Boolean = isBroadcast() || destinationId == nodeId
}

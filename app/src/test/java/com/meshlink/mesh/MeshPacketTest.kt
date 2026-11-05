package com.meshlink.mesh

import com.meshlink.mesh.model.MeshPacket
import com.meshlink.mesh.model.PacketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshPacketTest {

    @Test
    fun testSerializationAndDeserialization() {
        val originalPacket = MeshPacket(
            messageId = "msg-12345",
            senderId = "NODE-A",
            senderAlias = "Node Alpha",
            destinationId = "NODE-D",
            timestamp = 1700000000000L,
            ttl = 7,
            hopCount = 0,
            payload = "Hello MeshLink!",
            packetType = PacketType.CHAT,
            routeHistory = listOf("NODE-A")
        )

        val jsonString = originalPacket.toJson()
        val deserialized = MeshPacket.fromJson(jsonString)

        assertNotNull(deserialized)
        assertEquals(originalPacket.messageId, deserialized?.messageId)
        assertEquals(originalPacket.senderId, deserialized?.senderId)
        assertEquals(originalPacket.senderAlias, deserialized?.senderAlias)
        assertEquals(originalPacket.destinationId, deserialized?.destinationId)
        assertEquals(originalPacket.payload, deserialized?.payload)
        assertEquals(originalPacket.ttl, deserialized?.ttl)
        assertEquals(originalPacket.hopCount, deserialized?.hopCount)
        assertEquals(originalPacket.routeHistory, deserialized?.routeHistory)
    }

    @Test
    fun testCreateForwardedPacket() {
        val packet = MeshPacket(
            messageId = "msg-100",
            senderId = "NODE-A",
            senderAlias = "NODE A",
            destinationId = "NODE-D",
            ttl = 7,
            hopCount = 0,
            payload = "Hop test",
            routeHistory = listOf("NODE-A")
        )

        val hop1 = packet.createForwardedPacket("NODE-B")
        assertEquals(6, hop1.ttl)
        assertEquals(1, hop1.hopCount)
        assertEquals(listOf("NODE-A", "NODE-B"), hop1.routeHistory)

        val hop2 = hop1.createForwardedPacket("NODE-C")
        assertEquals(5, hop2.ttl)
        assertEquals(2, hop2.hopCount)
        assertEquals(listOf("NODE-A", "NODE-B", "NODE-C"), hop2.routeHistory)
    }

    @Test
    fun testDeliveryAckGeneration() {
        val chatPacket = MeshPacket(
            messageId = "original-chat-999",
            senderId = "NODE-A",
            senderAlias = "NODE A",
            destinationId = "NODE-D",
            payload = "Ping D",
            packetType = PacketType.CHAT,
            routeHistory = listOf("NODE-A", "NODE-B", "NODE-C")
        )

        val ackPacket = chatPacket.createDeliveryAck("NODE-D", "NODE D")

        assertEquals("NODE-D", ackPacket.senderId)
        assertEquals("NODE-A", ackPacket.destinationId)
        assertEquals(PacketType.DELIVERY_ACK, ackPacket.packetType)
        assertEquals("original-chat-999", ackPacket.originalMessageId)
        assertEquals("ACK_DELIVERED", ackPacket.payload)
    }

    @Test
    fun testBroadcastAndTargeting() {
        val broadcastPacket = MeshPacket(
            senderId = "NODE-A",
            senderAlias = "NODE A",
            destinationId = MeshPacket.BROADCAST_DESTINATION
        )
        assertTrue(broadcastPacket.isBroadcast())
        assertTrue(broadcastPacket.isTargetedTo("NODE-B"))
        assertTrue(broadcastPacket.isTargetedTo("NODE-Z"))

        val unicastPacket = MeshPacket(
            senderId = "NODE-A",
            senderAlias = "NODE A",
            destinationId = "NODE-D"
        )
        assertFalse(unicastPacket.isBroadcast())
        assertTrue(unicastPacket.isTargetedTo("NODE-D"))
        assertFalse(unicastPacket.isTargetedTo("NODE-B"))
    }
}

package com.meshlink.mesh

import com.meshlink.data.database.dao.ConversationDao
import com.meshlink.data.database.dao.MessageDao
import com.meshlink.data.database.dao.PeerDao
import com.meshlink.data.database.dao.ProcessedPacketDao
import com.meshlink.data.database.dao.QueuedPacketDao
import com.meshlink.data.database.dao.RouteDao
import com.meshlink.data.database.entity.ConversationEntity
import com.meshlink.data.database.entity.MessageEntity
import com.meshlink.data.database.entity.PeerEntity
import com.meshlink.data.database.entity.ProcessedPacketEntity
import com.meshlink.data.database.entity.QueuedPacketEntity
import com.meshlink.data.database.entity.RouteEntity
import com.meshlink.data.model.MessageStatus
import com.meshlink.mesh.forwarding.StoreAndForwardManager
import com.meshlink.mesh.identity.NodeIdentityManager
import com.meshlink.mesh.model.MeshPacket
import com.meshlink.mesh.model.PacketType
import com.meshlink.mesh.model.TransportEvent
import com.meshlink.mesh.peer.PeerManager
import com.meshlink.mesh.routing.MeshRouter
import com.meshlink.mesh.transport.MeshTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MeshRouterTest {

    private lateinit var mockTransport: MockTransport
    private lateinit var mockPeerDao: MockPeerDao
    private lateinit var mockMessageDao: MockMessageDao
    private lateinit var mockConversationDao: MockConversationDao
    private lateinit var mockRouteDao: MockRouteDao
    private lateinit var mockProcessedPacketDao: MockProcessedPacketDao
    private lateinit var mockQueuedPacketDao: MockQueuedPacketDao
    private lateinit var peerManager: PeerManager
    private lateinit var mockIdentityManager: NodeIdentityManager
    private lateinit var storeAndForwardManager: StoreAndForwardManager
    private lateinit var meshRouter: MeshRouter

    @Before
    fun setup() {
        mockTransport = MockTransport()
        mockPeerDao = MockPeerDao()
        mockMessageDao = MockMessageDao()
        mockConversationDao = MockConversationDao()
        mockRouteDao = MockRouteDao()
        mockProcessedPacketDao = MockProcessedPacketDao()
        mockQueuedPacketDao = MockQueuedPacketDao()

        peerManager = PeerManager(mockPeerDao)
        mockIdentityManager = NodeIdentityManager(null, initialNodeId = "NODE-B", initialAlias = "NODE B")
        storeAndForwardManager = StoreAndForwardManager(mockQueuedPacketDao)

        meshRouter = MeshRouter(
            transport = mockTransport,
            peerManager = peerManager,
            identityManager = mockIdentityManager,
            storeAndForwardManager = storeAndForwardManager,
            messageDao = mockMessageDao,
            conversationDao = mockConversationDao,
            routeDao = mockRouteDao,
            processedPacketDao = mockProcessedPacketDao,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )
    }

    @Test
    fun testIntermediateNodeForwardsPacket() = runBlocking {
        // Packet from NODE-A targeting NODE-D arriving at NODE-B
        val incomingPacket = MeshPacket(
            messageId = "test-msg-001",
            senderId = "NODE-A",
            senderAlias = "NODE A",
            destinationId = "NODE-D",
            ttl = 7,
            hopCount = 0,
            payload = "Hello D via B",
            packetType = PacketType.CHAT,
            routeHistory = listOf("NODE-A")
        )

        meshRouter.processIncomingPacket(incomingPacket, incomingEndpointId = "endpoint-a")

        // NODE-B should have broadcast the forwarded packet
        assertEquals(1, mockTransport.broadcastHistory.size)
        val forwardedBytes = mockTransport.broadcastHistory.first().first
        val excludedEndpoint = mockTransport.broadcastHistory.first().second

        val forwardedPacket = MeshPacket.fromBytes(forwardedBytes)
        assertNotNull(forwardedPacket)
        assertEquals(6, forwardedPacket?.ttl)
        assertEquals(1, forwardedPacket?.hopCount)
        assertEquals(listOf("NODE-A", "NODE-B"), forwardedPacket?.routeHistory)
        assertEquals("endpoint-a", excludedEndpoint) // Does not echo back to sender
    }

    @Test
    fun testLoopRejection() = runBlocking {
        // Packet that already contains NODE-B in route history
        val loopingPacket = MeshPacket(
            messageId = "loop-msg-002",
            senderId = "NODE-A",
            senderAlias = "NODE A",
            destinationId = "NODE-D",
            ttl = 5,
            hopCount = 2,
            payload = "Loop test",
            packetType = PacketType.CHAT,
            routeHistory = listOf("NODE-A", "NODE-B", "NODE-C")
        )

        meshRouter.processIncomingPacket(loopingPacket, incomingEndpointId = "endpoint-c")

        // Should NOT forward since NODE-B is in the route history
        assertEquals(0, mockTransport.broadcastHistory.size)
    }

    @Test
    fun testDeduplicationDropsSecondPacket() = runBlocking {
        val packet = MeshPacket(
            messageId = "duplicate-msg-003",
            senderId = "NODE-A",
            senderAlias = "NODE A",
            destinationId = "NODE-D",
            ttl = 5,
            hopCount = 0,
            payload = "Dup test",
            packetType = PacketType.CHAT,
            routeHistory = listOf("NODE-A")
        )

        // First arrival
        meshRouter.processIncomingPacket(packet, incomingEndpointId = "endpoint-a")
        assertEquals(1, mockTransport.broadcastHistory.size)

        // Second arrival of same messageId from a different path
        meshRouter.processIncomingPacket(packet, incomingEndpointId = "endpoint-x")
        assertEquals(1, mockTransport.broadcastHistory.size) // No second broadcast
    }

    @Test
    fun testLocalDeliveryGeneratesReverseAck() = runBlocking {
        // Re-target to NODE-B (local node)
        val packetForMe = MeshPacket(
            messageId = "msg-for-me-004",
            senderId = "NODE-A",
            senderAlias = "NODE A",
            destinationId = "NODE-B",
            ttl = 7,
            hopCount = 0,
            payload = "Direct to B",
            packetType = PacketType.CHAT,
            routeHistory = listOf("NODE-A")
        )

        meshRouter.processIncomingPacket(packetForMe, incomingEndpointId = "endpoint-a")

        // Should generate reverse ACK packet and broadcast/send it back
        assertTrue(mockTransport.broadcastHistory.isNotEmpty() || mockTransport.sentHistory.isNotEmpty())
    }

    // --- Mock Implementations for Unit Testing ---

    class MockTransport : MeshTransport {
        val broadcastHistory = mutableListOf<Pair<ByteArray, String?>>()
        val sentHistory = mutableListOf<Pair<String, ByteArray>>()
        private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 16)
        override val events: SharedFlow<TransportEvent> = _events
        override val isAdvertising: StateFlow<Boolean> = MutableStateFlow(true)
        override val isDiscovering: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun startAdvertising(nodeId: String, nodeAlias: String): Boolean = true
        override suspend fun startDiscovery(): Boolean = true
        override suspend fun stopAdvertising() {}
        override suspend fun stopDiscovery() {}
        override suspend fun connectToPeer(endpointId: String): Boolean = true
        override suspend fun disconnectPeer(endpointId: String) {}

        override suspend fun sendBytes(endpointId: String, bytes: ByteArray): Boolean {
            sentHistory.add(Pair(endpointId, bytes))
            return true
        }

        override suspend fun broadcastBytes(bytes: ByteArray, excludeEndpointId: String?): Int {
            broadcastHistory.add(Pair(bytes, excludeEndpointId))
            return 1
        }

        override fun stopAll() {}
    }

    class MockPeerDao : PeerDao {
        override fun getAllPeers(): Flow<List<PeerEntity>> = flowOf(emptyList())
        override fun getConnectedPeers(): Flow<List<PeerEntity>> = flowOf(emptyList())
        override suspend fun getPeerByNodeId(nodeId: String): PeerEntity? = null
        override suspend fun insertOrUpdatePeer(peer: PeerEntity) {}
        override suspend fun updatePeer(peer: PeerEntity) {}
        override suspend fun updateConnectionState(nodeId: String, connected: Boolean) {}
        override suspend fun markAllDisconnected() {}
        override suspend fun deletePeer(nodeId: String) {}
        override suspend fun clearAll() {}
    }

    class MockMessageDao : MessageDao {
        val messages = mutableListOf<MessageEntity>()
        override fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> = flowOf(messages)
        override suspend fun getMessageById(messageId: String): MessageEntity? = messages.find { it.id == messageId }
        override suspend fun insertMessage(message: MessageEntity) { messages.add(message) }
        override suspend fun updateMessage(message: MessageEntity) {}
        override suspend fun updateDeliveryStatus(messageId: String, status: MessageStatus, hopCount: Int, routeJson: String) {}
        override suspend fun updateStatus(messageId: String, status: MessageStatus) {}
        override fun getTotalMessageCount(): Flow<Int> = flowOf(messages.size)
        override suspend fun deleteConversationMessages(conversationId: String) {}
        override suspend fun clearAll() { messages.clear() }
    }

    class MockConversationDao : ConversationDao {
        override fun getAllConversations(): Flow<List<ConversationEntity>> = flowOf(emptyList())
        override suspend fun getConversationById(peerId: String): ConversationEntity? = null
        override suspend fun insertOrUpdate(conversation: ConversationEntity) {}
        override suspend fun update(conversation: ConversationEntity) {}
        override suspend fun markAsRead(peerId: String) {}
        override suspend fun deleteConversation(peerId: String) {}
        override suspend fun clearAll() {}
    }

    class MockRouteDao : RouteDao {
        val routes = mutableMapOf<String, RouteEntity>()
        override fun getAllRoutes(): Flow<List<RouteEntity>> = flowOf(routes.values.toList())
        override suspend fun getRouteForDestination(destinationNodeId: String): RouteEntity? = routes[destinationNodeId]
        override suspend fun insertOrUpdateRoute(route: RouteEntity) { routes[route.destinationNodeId] = route }
        override suspend fun updateRoute(route: RouteEntity) { routes[route.destinationNodeId] = route }
        override suspend fun deleteRoute(destinationNodeId: String) { routes.remove(destinationNodeId) }
        override suspend fun pruneOldRoutes(cutoffTimestamp: Long) {}
        override suspend fun clearAll() { routes.clear() }
    }

    class MockProcessedPacketDao : ProcessedPacketDao {
        val processed = mutableSetOf<String>()
        override suspend fun hasPacket(messageId: String): Int = if (processed.contains(messageId)) 1 else 0
        override suspend fun insertPacket(packet: ProcessedPacketEntity) { processed.add(packet.messageId) }
        override suspend fun pruneOldPackets(cutoffTimestamp: Long) {}
        override suspend fun clearAll() { processed.clear() }
    }

    class MockQueuedPacketDao : QueuedPacketDao {
        val queued = mutableListOf<QueuedPacketEntity>()
        override fun getAllQueuedPackets(): Flow<List<QueuedPacketEntity>> = flowOf(queued)
        override suspend fun getPacketsForDestination(destinationNodeId: String): List<QueuedPacketEntity> =
            queued.filter { it.destinationNodeId == destinationNodeId }
        override suspend fun getAllQueuedList(): List<QueuedPacketEntity> = queued
        override suspend fun insertQueuedPacket(packet: QueuedPacketEntity): Long {
            queued.add(packet)
            return queued.size.toLong()
        }
        override suspend fun updateQueuedPacket(packet: QueuedPacketEntity) {}
        override suspend fun deleteQueuedPacket(packet: QueuedPacketEntity) { queued.remove(packet) }
        override suspend fun deleteByMessageId(messageId: String) { queued.removeAll { it.messageId == messageId } }
        override suspend fun pruneExpiredPackets(cutoffTimestamp: Long) {}
        override suspend fun clearAll() { queued.clear() }
    }
}

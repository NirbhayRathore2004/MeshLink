package com.meshlink.mesh.routing

import com.meshlink.data.database.dao.ConversationDao
import com.meshlink.data.database.dao.MessageDao
import com.meshlink.data.database.dao.ProcessedPacketDao
import com.meshlink.data.database.dao.RouteDao
import com.meshlink.data.database.entity.ConversationEntity
import com.meshlink.data.database.entity.MessageEntity
import com.meshlink.data.database.entity.ProcessedPacketEntity
import com.meshlink.data.database.entity.RouteEntity
import com.meshlink.data.model.MessageStatus
import com.meshlink.mesh.forwarding.StoreAndForwardManager
import com.meshlink.mesh.identity.NodeIdentityManager
import com.meshlink.mesh.model.MeshPacket
import com.meshlink.mesh.model.PacketType
import com.meshlink.mesh.model.TransportEvent
import com.meshlink.mesh.peer.PeerManager
import com.meshlink.mesh.transport.MeshTransport
import com.meshlink.util.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

class MeshRouter(
    private val transport: MeshTransport,
    private val peerManager: PeerManager,
    private val identityManager: NodeIdentityManager,
    private val storeAndForwardManager: StoreAndForwardManager,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val routeDao: RouteDao,
    private val processedPacketDao: ProcessedPacketDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    // In-memory fast LRU cache for processed message IDs (max 1000 items)
    private val processedMessageIds = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > 1000
            }
        }
    )

    private val _incomingMessages = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<MeshPacket> = _incomingMessages.asSharedFlow()

    private val _routedPackets = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 64)
    val routedPackets: SharedFlow<MeshPacket> = _routedPackets.asSharedFlow()

    init {
        observeTransportEvents()
    }

    private fun observeTransportEvents() {
        scope.launch {
            transport.events.collect { event ->
                peerManager.handleTransportEvent(event)
                when (event) {
                    is TransportEvent.DataReceived -> {
                        handleIncomingBytes(event.endpointId, event.bytes)
                    }
                    is TransportEvent.PeerConnected -> {
                        // Announce self to newly connected peer and drain queued packets
                        val peerNodeId = peerManager.getNodeIdForEndpoint(event.endpointId)
                        sendPeerAnnouncement(event.endpointId)
                        if (peerNodeId != null) {
                            drainQueuedPacketsForPeer(peerNodeId)
                        }
                    }
                    else -> { /* No-op */ }
                }
            }
        }
    }

    /**
     * Entry point for incoming raw network bytes.
     */
    fun handleIncomingBytes(incomingEndpointId: String, bytes: ByteArray) {
        val packet = MeshPacket.fromBytes(bytes)
        if (packet == null) {
            MeshLogger.w(MeshLogger.TAG_ROUTING, "Received invalid or unparseable byte packet from endpoint $incomingEndpointId")
            return
        }
        processIncomingPacket(packet, incomingEndpointId)
    }

    /**
     * Core mesh routing and controlled flooding logic.
     */
    fun processIncomingPacket(packet: MeshPacket, incomingEndpointId: String?) {
        val localNodeId = identityManager.nodeId.value
        val localAlias = identityManager.nodeAlias.value

        MeshLogger.d(
            MeshLogger.TAG_ROUTING,
            "Incoming packet [${packet.packetType}]: ID=${packet.messageId.take(8)} from ${packet.senderAlias}(${packet.senderId.take(6)}) -> Dest=${packet.destinationId.take(6)} (TTL=${packet.ttl}, Hops=${packet.hopCount})"
        )

        // 1. Deduplication check
        if (isDuplicate(packet.messageId)) {
            MeshLogger.d(MeshLogger.TAG_ROUTING, "Duplicate packet ${packet.messageId.take(8)} dropped")
            return
        }
        markPacketProcessed(packet)

        // 2. Self-loop check (did this node originate or already forward this packet?)
        if (packet.senderId == localNodeId || packet.routeHistory.contains(localNodeId)) {
            MeshLogger.d(MeshLogger.TAG_ROUTING, "Loop detected for packet ${packet.messageId.take(8)}; dropped")
            return
        }

        // 3. Learn reverse route to sender
        learnRoute(packet.senderId, incomingEndpointId, packet.senderAlias, packet.hopCount + 1)

        // 4. Multi-hop peer discovery (only for peers beyond direct neighbors)
        if (packet.senderId != localNodeId) {
            val directNodeId = incomingEndpointId?.let { peerManager.getNodeIdForEndpoint(it) }
            val isDirectNeighbor = directNodeId == packet.senderId || packet.hopCount == 0

            if (!isDirectNeighbor) {
                val viaNode = directNodeId ?: "neighbor"
                peerManager.registerMultiHopPeer(
                    nodeId = packet.senderId,
                    alias = packet.senderAlias,
                    hopDistance = packet.hopCount + 1,
                    viaNodeId = viaNode
                )
            }
        }

        // 5. Destination Delivery Check
        val isTargetedToMe = packet.isTargetedTo(localNodeId)
        if (isTargetedToMe) {
            handleLocalDelivery(packet, localNodeId, localAlias)
        }

        // 6. Forwarding check (if packet is broadcast OR targeted to another node and TTL > 1)
        val shouldForward = packet.isBroadcast() || packet.destinationId != localNodeId
        if (shouldForward) {
            if (packet.ttl > 1) {
                forwardPacket(packet, localNodeId, incomingEndpointId)
            } else {
                MeshLogger.w(MeshLogger.TAG_ROUTING, "TTL expired for packet ${packet.messageId.take(8)} (TTL=${packet.ttl}); packet dropped")
            }
        }
    }

    private fun handleLocalDelivery(packet: MeshPacket, localNodeId: String, localAlias: String) {
        MeshLogger.i(
            MeshLogger.TAG_DELIVERY,
            "Packet ${packet.messageId.take(8)} DELIVERED to local node ($localAlias) via ${packet.hopCount} hops"
        )

        scope.launch {
            when (packet.packetType) {
                PacketType.CHAT -> {
                    saveIncomingChatMessage(packet)
                    _incomingMessages.emit(packet)

                    // If it's a direct message (not broadcast), send reverse delivery ACK
                    if (!packet.isBroadcast()) {
                        val ackPacket = packet.createDeliveryAck(localNodeId, localAlias)
                        MeshLogger.i(MeshLogger.TAG_ACK, "Generating Delivery ACK for packet ${packet.messageId.take(8)} back to ${packet.senderId}")
                        sendPacket(ackPacket)
                    }
                }
                PacketType.DELIVERY_ACK -> {
                    handleDeliveryAckReceived(packet)
                }
                PacketType.PEER_ANNOUNCE -> {
                    MeshLogger.d(MeshLogger.TAG_ROUTING, "Peer announcement received from ${packet.senderAlias} (${packet.senderId})")
                }
                PacketType.PING -> {
                    MeshLogger.d(MeshLogger.TAG_ROUTING, "Ping received from ${packet.senderAlias}")
                }
                PacketType.ROUTE_REQ -> {
                    // Respond with route if known
                }
                PacketType.ROUTE_RESP -> {
                    // Update routing table
                }
            }
        }
    }

    private fun forwardPacket(packet: MeshPacket, localNodeId: String, incomingEndpointId: String?) {
        val forwarded = packet.createForwardedPacket(localNodeId)
        MeshLogger.i(
            MeshLogger.TAG_FORWARDING,
            "Forwarding packet ${packet.messageId.take(8)}: new TTL=${forwarded.ttl}, Hops=${forwarded.hopCount}, Route=${forwarded.routeHistory.joinToString(" -> ")}"
        )

        scope.launch {
            _routedPackets.emit(forwarded)
            val bytes = forwarded.toBytes()

            // Check if we have a direct known route for this destination
            val route = routeDao.getRouteForDestination(forwarded.destinationId)
            if (route != null && route.nextHopEndpointId != incomingEndpointId) {
                val sent = transport.sendBytes(route.nextHopEndpointId, bytes)
                if (sent) {
                    MeshLogger.d(MeshLogger.TAG_FORWARDING, "Forwarded packet directly along next-hop endpoint ${route.nextHopEndpointId}")
                    return@launch
                }
            }

            // Controlled flooding to all neighbors except the sender
            transport.broadcastBytes(bytes, excludeEndpointId = incomingEndpointId)
        }
    }

    /**
     * Sends a new outgoing chat message originated from this node.
     */
    suspend fun sendChatMessage(
        destinationId: String,
        destinationAlias: String,
        content: String,
        ttl: Int = MeshPacket.DEFAULT_TTL
    ): MeshPacket {
        val localNodeId = identityManager.nodeId.value
        val localAlias = identityManager.nodeAlias.value

        val packet = MeshPacket(
            messageId = UUID.randomUUID().toString(),
            senderId = localNodeId,
            senderAlias = localAlias,
            destinationId = destinationId,
            timestamp = System.currentTimeMillis(),
            ttl = ttl,
            hopCount = 0,
            payload = content,
            packetType = PacketType.CHAT,
            routeHistory = listOf(localNodeId)
        )

        // Save to local database
        saveOutgoingChatMessage(packet, destinationAlias)

        // Mark local copy as processed so we don't re-digest our own packet if echoed back
        markPacketProcessed(packet)

        // Send over physical transport
        val sentPeersCount = sendPacket(packet)

        if (sentPeersCount == 0) {
            MeshLogger.w(MeshLogger.TAG_ROUTING, "No connected peers available; queued packet in Store-and-Forward")
            storeAndForwardManager.queuePacket(packet)
            messageDao.updateStatus(packet.messageId, MessageStatus.PENDING)
        } else {
            messageDao.updateStatus(packet.messageId, MessageStatus.SENT_TO_PEER)
        }

        return packet
    }

    /**
     * Low-level packet dispatch: checks route table or broadcasts to direct peers.
     */
    suspend fun sendPacket(packet: MeshPacket): Int {
        val bytes = packet.toBytes()

        // Check unicast routing table first
        if (!packet.isBroadcast()) {
            val route = routeDao.getRouteForDestination(packet.destinationId)
            if (route != null) {
                val directSent = transport.sendBytes(route.nextHopEndpointId, bytes)
                if (directSent) {
                    MeshLogger.d(MeshLogger.TAG_ROUTING, "Dispatched packet ${packet.messageId.take(8)} via routed endpoint ${route.nextHopEndpointId}")
                    return 1
                }
            }
        }

        // Fallback to broadcasting to all direct neighbors
        return transport.broadcastBytes(bytes)
    }

    private suspend fun saveOutgoingChatMessage(packet: MeshPacket, destinationAlias: String) {
        val conversationId = if (packet.isBroadcast()) MeshPacket.BROADCAST_DESTINATION else packet.destinationId

        val messageEntity = MessageEntity(
            id = packet.messageId,
            conversationId = conversationId,
            senderId = packet.senderId,
            senderAlias = packet.senderAlias,
            destinationId = packet.destinationId,
            destinationAlias = destinationAlias,
            content = packet.payload,
            timestamp = packet.timestamp,
            status = MessageStatus.PENDING,
            hopCount = 0,
            routeHistoryJson = Json.encodeToString(packet.routeHistory),
            isOutgoing = true
        )
        messageDao.insertMessage(messageEntity)

        val conversationEntity = ConversationEntity(
            peerId = conversationId,
            peerAlias = if (packet.isBroadcast()) "All Mesh (Broadcast)" else destinationAlias,
            lastMessage = packet.payload,
            lastTimestamp = packet.timestamp,
            unreadCount = 0,
            isBroadcast = packet.isBroadcast()
        )
        conversationDao.insertOrUpdate(conversationEntity)
    }

    private suspend fun saveIncomingChatMessage(packet: MeshPacket) {
        val conversationId = if (packet.isBroadcast()) MeshPacket.BROADCAST_DESTINATION else packet.senderId

        val messageEntity = MessageEntity(
            id = packet.messageId,
            conversationId = conversationId,
            senderId = packet.senderId,
            senderAlias = packet.senderAlias,
            destinationId = packet.destinationId,
            destinationAlias = identityManager.nodeAlias.value,
            content = packet.payload,
            timestamp = packet.timestamp,
            status = MessageStatus.DELIVERED,
            hopCount = packet.hopCount,
            routeHistoryJson = Json.encodeToString(packet.routeHistory),
            isOutgoing = false
        )
        messageDao.insertMessage(messageEntity)

        val existingConv = conversationDao.getConversationById(conversationId)
        val unread = (existingConv?.unreadCount ?: 0) + 1

        val conversationEntity = ConversationEntity(
            peerId = conversationId,
            peerAlias = if (packet.isBroadcast()) "All Mesh (Broadcast)" else packet.senderAlias,
            lastMessage = packet.payload,
            lastTimestamp = packet.timestamp,
            unreadCount = unread,
            isBroadcast = packet.isBroadcast()
        )
        conversationDao.insertOrUpdate(conversationEntity)
    }

    private suspend fun handleDeliveryAckReceived(ackPacket: MeshPacket) {
        val targetMessageId = ackPacket.originalMessageId ?: return
        MeshLogger.i(
            MeshLogger.TAG_ACK,
            "Received Delivery ACK for message ${targetMessageId.take(8)} from ${ackPacket.senderAlias} (${ackPacket.senderId.take(6)}) via ${ackPacket.hopCount} hops! Route: ${ackPacket.routeHistory.joinToString(" -> ")}"
        )

        messageDao.updateDeliveryStatus(
            messageId = targetMessageId,
            status = MessageStatus.DELIVERED,
            hopCount = ackPacket.hopCount,
            routeJson = Json.encodeToString(ackPacket.routeHistory)
        )

        // Remove from Store and Forward queue
        storeAndForwardManager.removePacketFromQueue(targetMessageId)
    }

    private fun sendPeerAnnouncement(endpointId: String) {
        val localNodeId = identityManager.nodeId.value
        val localAlias = identityManager.nodeAlias.value
        val announcePacket = MeshPacket(
            messageId = UUID.randomUUID().toString(),
            senderId = localNodeId,
            senderAlias = localAlias,
            destinationId = MeshPacket.BROADCAST_DESTINATION,
            packetType = PacketType.PEER_ANNOUNCE,
            payload = "ANNOUNCE",
            ttl = 1,
            hopCount = 0,
            routeHistory = listOf(localNodeId)
        )
        scope.launch {
            transport.sendBytes(endpointId, announcePacket.toBytes())
        }
    }

    private fun drainQueuedPacketsForPeer(peerNodeId: String) {
        scope.launch {
            val queued = storeAndForwardManager.getQueuedPacketsForNode(peerNodeId)
            if (queued.isNotEmpty()) {
                MeshLogger.i(MeshLogger.TAG_FORWARDING, "Draining ${queued.size} queued packets for peer $peerNodeId")
                for (packet in queued) {
                    sendPacket(packet)
                }
            }
        }
    }

    private fun learnRoute(senderId: String, incomingEndpointId: String?, senderAlias: String, hopCount: Int) {
        if (incomingEndpointId == null) return
        scope.launch {
            val existing = routeDao.getRouteForDestination(senderId)
            if (existing == null || existing.hopCount > hopCount) {
                routeDao.insertOrUpdateRoute(
                    RouteEntity(
                        destinationNodeId = senderId,
                        nextHopEndpointId = incomingEndpointId,
                        nextHopNodeId = peerManager.getNodeIdForEndpoint(incomingEndpointId) ?: senderId,
                        hopCount = hopCount,
                        updatedTimestamp = System.currentTimeMillis()
                    )
                )
                MeshLogger.d(MeshLogger.TAG_ROUTING, "Learned route: To $senderId ($senderAlias) via endpoint $incomingEndpointId with hop distance $hopCount")
            }
        }
    }

    private fun isDuplicate(messageId: String): Boolean {
        if (processedMessageIds.containsKey(messageId)) {
            return true
        }
        return false
    }

    private fun markPacketProcessed(packet: MeshPacket) {
        processedMessageIds[packet.messageId] = System.currentTimeMillis()
        scope.launch {
            processedPacketDao.insertPacket(
                ProcessedPacketEntity(
                    messageId = packet.messageId,
                    receivedTimestamp = System.currentTimeMillis(),
                    originNodeId = packet.senderId,
                    packetType = packet.packetType.name
                )
            )
        }
    }

    fun clearAllState() {
        processedMessageIds.clear()
        scope.launch {
            processedPacketDao.clearAll()
            routeDao.clearAll()
        }
    }
}

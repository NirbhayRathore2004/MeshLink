package com.meshlink.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.data.database.entity.ConversationEntity
import com.meshlink.mesh.model.MeshPacket
import com.meshlink.ui.theme.CyanNeon
import com.meshlink.ui.theme.EmeraldGreen
import com.meshlink.ui.theme.MeshBorder
import com.meshlink.ui.theme.MeshNavyDark
import com.meshlink.ui.theme.MeshSurfaceDark
import com.meshlink.ui.theme.MeshSurfaceElevated
import com.meshlink.ui.theme.PurpleNeon
import com.meshlink.ui.theme.RoseRed
import com.meshlink.ui.theme.TextPrimary
import com.meshlink.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (peerId: String, peerAlias: String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewChatDialog by remember { mutableStateOf(false) }
    var manualNodeIdInput by remember { mutableStateOf("") }
    var manualAliasInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mesh Conversations",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanNeon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MeshNavyDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = CyanNeon,
                contentColor = MeshNavyDark,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.AddComment, contentDescription = "New Chat")
            }
        },
        containerColor = MeshNavyDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Broadcast Banner (Always available)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PurpleNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable {
                        onNavigateToChat(MeshPacket.BROADCAST_DESTINATION, "All Mesh (Broadcast)")
                    },
                colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PurpleNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Broadcast",
                            tint = PurpleNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Broadcast Channel",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Send a message to all connected mesh nodes in range",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DIRECT CHATS",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Conversations List
            val directConversations = state.conversations.filter { !it.isBroadcast }

            if (directConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No direct conversations yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + to start a chat with a nearby peer or enter a Node ID",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(directConversations, key = { it.peerId }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = { onNavigateToChat(conv.peerId, conv.peerAlias) },
                            onDelete = { viewModel.deleteConversation(conv.peerId) }
                        )
                    }
                }
            }
        }
    }

    // New Chat Dialog
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            containerColor = MeshSurfaceDark,
            title = { Text("Start New Chat", color = TextPrimary) },
            text = {
                Column {
                    Text("Select from discovered peers:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (state.availablePeers.isEmpty()) {
                        Text("No peers discovered yet.", fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.6f))
                    } else {
                        state.availablePeers.take(4).forEach { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MeshSurfaceElevated)
                                    .clickable {
                                        showNewChatDialog = false
                                        onNavigateToChat(peer.nodeId, peer.alias)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (peer.isConnected) EmeraldGreen else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(peer.alias, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(peer.nodeId, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Or enter target Node ID directly:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = manualNodeIdInput,
                        onValueChange = { manualNodeIdInput = it },
                        label = { Text("Node ID (e.g. NODE-D)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = manualAliasInput,
                        onValueChange = { manualAliasInput = it },
                        label = { Text("Friendly Name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualNodeIdInput.isNotBlank()) {
                            val targetId = manualNodeIdInput.trim()
                            val targetAlias = manualAliasInput.trim().ifEmpty { targetId }
                            showNewChatDialog = false
                            onNavigateToChat(targetId, targetAlias)
                        }
                    }
                ) {
                    Text("Open Chat", color = CyanNeon, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MeshBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CyanNeon.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.peerAlias,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = formatTime(conversation.lastTimestamp),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage.ifBlank { "No messages" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Badge(containerColor = CyanNeon, contentColor = MeshNavyDark) {
                            Text("${conversation.unreadCount}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

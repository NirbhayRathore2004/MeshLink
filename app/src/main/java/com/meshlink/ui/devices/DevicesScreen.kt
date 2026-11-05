package com.meshlink.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.meshlink.mesh.model.ConnectionState
import com.meshlink.mesh.model.MeshNode
import com.meshlink.ui.theme.AmberOrange
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel,
    onNavigateBack: () -> Unit,
    onOpenChat: (nodeId: String, alias: String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mesh Neighbors & Peers",
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
                actions = {
                    IconButton(onClick = { viewModel.restartDiscovery() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Discovery",
                            tint = CyanNeon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MeshNavyDark)
            )
        },
        containerColor = MeshNavyDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Radio State Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MeshSurfaceDark)
                    .border(1.dp, MeshBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioStatusIndicator("Nearby Advertising", state.isAdvertising)
                RadioStatusIndicator("Nearby Discovery", state.isDiscovering)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ACTIVE CLUSTER NODES (${state.activeNodes.size})",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            if (state.activeNodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Scanning for offline mesh peers...",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ensure other devices have MeshLink open with Bluetooth and Wi-Fi enabled.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.activeNodes, key = { it.nodeId }) { node ->
                        MeshNodeItem(
                            node = node,
                            onConnect = { node.endpointId?.let { viewModel.connectToEndpoint(it) } },
                            onDisconnect = { node.endpointId?.let { viewModel.disconnectEndpoint(it) } },
                            onChat = { onOpenChat(node.nodeId, node.alias) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioStatusIndicator(label: String, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) EmeraldGreen else Color.Gray)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive) TextPrimary else TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MeshNodeItem(
    node: MeshNode,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onChat: () -> Unit
) {
    val stateColor = when (node.connectionState) {
        ConnectionState.CONNECTED -> EmeraldGreen
        ConnectionState.CONNECTING -> AmberOrange
        ConnectionState.DISCOVERED -> CyanNeon
        ConnectionState.DISCONNECTED -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MeshBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(stateColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = node.alias,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                // Hop badge
                val hopLabel = if (node.isDirectNeighbor) "Direct (1 hop)" else "Multi-Hop (${node.hopDistance} hops)"
                Text(
                    text = hopLabel,
                    fontSize = 11.sp,
                    color = if (node.isDirectNeighbor) CyanNeon else PurpleNeon,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MeshSurfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Node ID: ${node.nodeId}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )

            if (!node.endpointId.isNullOrBlank()) {
                Text(
                    text = "Nearby Endpoint: ${node.endpointId}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }

            if (!node.viaNodeId.isNullOrBlank()) {
                Text(
                    text = "Routed via: ${node.viaNodeId}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = PurpleNeon
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.isDirectNeighbor && node.endpointId != null) {
                    when (node.connectionState) {
                        ConnectionState.CONNECTED -> {
                            OutlinedButton(
                                onClick = onDisconnect,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disconnect", fontSize = 11.sp)
                            }
                        }
                        ConnectionState.CONNECTING -> {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AmberOrange,
                                    disabledContentColor = AmberOrange
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = AmberOrange
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Connecting...", fontSize = 11.sp)
                            }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = onConnect,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Connect", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                OutlinedButton(
                    onClick = onChat,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.meshlink.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.permissions.PermissionRequester
import com.meshlink.ui.theme.AmberOrange
import com.meshlink.ui.theme.CyanNeon
import com.meshlink.ui.theme.EmeraldGreen
import com.meshlink.ui.theme.MeshBorder
import com.meshlink.ui.theme.MeshNavyDark
import com.meshlink.ui.theme.MeshSurfaceDark
import com.meshlink.ui.theme.MeshSurfaceElevated
import com.meshlink.ui.theme.PurpleNeon
import com.meshlink.ui.theme.TextPrimary
import com.meshlink.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToMessages: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToTopology: () -> Unit,
    onNavigateToDemo: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEditAliasDialog by remember { mutableStateOf(false) }
    var aliasInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeshNavyDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // App Header: Title & Mesh Power Switch Button (adaptive & responsive)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "MeshLink",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = CyanNeon,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Offline Peer-to-Peer Mesh Network",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Mesh Power Switch Pill Button (Tap entire pill to toggle, fits safely on all screen widths)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MeshSurfaceElevated)
                    .border(
                        1.dp,
                        if (state.isMeshActive) CyanNeon else MeshBorder,
                        RoundedCornerShape(24.dp)
                    )
                    .clickable { viewModel.toggleMesh(context) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (state.isMeshActive) EmeraldGreen else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.isMeshActive) "ACTIVE" else "OFFLINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isMeshActive) EmeraldGreen else TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = state.isMeshActive,
                    onCheckedChange = { viewModel.toggleMesh(context) },
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MeshNavyDark,
                        checkedTrackColor = CyanNeon,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = MeshSurfaceDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permission Requester
        PermissionRequester(onAllPermissionsGranted = {})

        Spacer(modifier = Modifier.height(12.dp))

        // Node Identity Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MeshBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THIS NODE IDENTITY",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    IconButton(
                        onClick = {
                            aliasInput = state.nodeAlias
                            showEditAliasDialog = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Alias",
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = state.nodeAlias.ifBlank { "Node" },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MeshSurfaceElevated)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Node ID", state.nodeId))
                            Toast.makeText(context, "Node ID copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.nodeId,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CyanNeon,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Node ID",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Direct Peers",
                value = "${state.connectedPeersCount}",
                icon = Icons.Default.Devices,
                tint = CyanNeon,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Mesh Routes",
                value = "${state.activeRoutesCount}",
                icon = Icons.AutoMirrored.Filled.AltRoute,
                tint = PurpleNeon,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Cluster Nodes",
                value = "${state.totalDiscoveredNodesCount}",
                icon = Icons.Default.Hub,
                tint = EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ACTIONS & NAVIGATION",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        // Action Navigation Cards
        NavigationCard(
            title = "Messages & Broadcast",
            subtitle = "Send direct multi-hop messages or cluster broadcasts",
            icon = Icons.AutoMirrored.Filled.Chat,
            tint = CyanNeon,
            badgeText = if (state.connectedPeersCount > 0) "${state.connectedPeersCount} peers online" else null,
            onClick = onNavigateToMessages
        )

        Spacer(modifier = Modifier.height(10.dp))

        NavigationCard(
            title = "Discovered Devices",
            subtitle = "View direct neighbors, link states, and manual connect",
            icon = Icons.Default.Devices,
            tint = EmeraldGreen,
            badgeText = "${state.totalDiscoveredNodesCount} discovered",
            onClick = onNavigateToDevices
        )

        Spacer(modifier = Modifier.height(10.dp))

        NavigationCard(
            title = "Network Topology Graph",
            subtitle = "Interactive 2D visual map of the multi-hop mesh cluster",
            icon = Icons.Default.Hub,
            tint = PurpleNeon,
            badgeText = "${state.activeRoutesCount} routes",
            onClick = onNavigateToTopology
        )

        Spacer(modifier = Modifier.height(10.dp))

        NavigationCard(
            title = "College Demo & Routing Lab",
            subtitle = "4-node simulation, preset identities (A, B, C, D) & live logger",
            icon = Icons.Default.Science,
            tint = AmberOrange,
            onClick = onNavigateToDemo
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Edit Alias Dialog
    if (showEditAliasDialog) {
        AlertDialog(
            onDismissRequest = { showEditAliasDialog = false },
            containerColor = MeshSurfaceDark,
            title = { Text("Change Node Alias", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Set a friendly display name for this device (e.g. NODE-A or Alice).",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        label = { Text("Alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (aliasInput.isNotBlank()) {
                            viewModel.updateAlias(aliasInput.trim())
                        }
                        showEditAliasDialog = false
                    }
                ) {
                    Text("Save", color = CyanNeon, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAliasDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, MeshBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = TextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MeshBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = TextSecondary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tint.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

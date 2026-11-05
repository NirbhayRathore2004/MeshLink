package com.meshlink.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.data.database.entity.MessageEntity
import com.meshlink.ui.components.RoutePathCard
import com.meshlink.ui.components.StatusChip
import com.meshlink.ui.theme.ChatBubbleIncoming
import com.meshlink.ui.theme.ChatBubbleOutgoing
import com.meshlink.ui.theme.CyanNeon
import com.meshlink.ui.theme.EmeraldGreen
import com.meshlink.ui.theme.MeshBorder
import com.meshlink.ui.theme.MeshNavyDark
import com.meshlink.ui.theme.MeshSurfaceDark
import com.meshlink.ui.theme.MeshSurfaceElevated
import com.meshlink.ui.theme.PurpleNeon
import com.meshlink.ui.theme.TextPrimary
import com.meshlink.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showTtlSettings by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.peerAlias,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (state.peerId == "BROADCAST_ALL") "All Connected Nodes in Mesh" else "Node ID: ${state.peerId.take(16)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = CyanNeon,
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                    IconButton(onClick = { showTtlSettings = !showTtlSettings }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "TTL Settings",
                            tint = if (showTtlSettings) CyanNeon else TextSecondary
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
        ) {
            // Optional TTL Config Slider Banner
            if (showTtlSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MeshSurfaceDark)
                        .border(1.dp, MeshBorder)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Packet Time-To-Live (TTL):", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("${state.ttl} hops max", fontSize = 12.sp, color = CyanNeon, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = state.ttl.toFloat(),
                        onValueChange = { viewModel.onTtlChanged(it.toInt()) },
                        valueRange = 1f..15f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanNeon,
                            inactiveTrackColor = MeshSurfaceElevated
                        )
                    )
                }
            }

            // Messages Stream
            if (state.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet.\nType a message below to send across the mesh.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        ChatMessageBubble(message = message)
                    }
                }
            }

            // Input Bar (elevates smoothly with soft keyboard and safe navigation insets)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MeshSurfaceDark)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    placeholder = { Text("Write offline mesh message...", fontSize = 13.sp, color = TextSecondary) },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (state.inputText.isNotBlank() && !state.isSending) {
                                viewModel.sendMessage()
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = MeshBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = state.inputText.isNotBlank() && !state.isSending,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (state.inputText.isNotBlank()) CyanNeon else MeshSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (state.inputText.isNotBlank()) MeshNavyDark else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: MessageEntity) {
    val isOutgoing = message.isOutgoing

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        // Sender Alias if incoming
        if (!isOutgoing) {
            Text(
                text = message.senderAlias,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanNeon,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(min = 70.dp, max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isOutgoing) 14.dp else 2.dp,
                        bottomEnd = if (isOutgoing) 2.dp else 14.dp
                    )
                )
                .background(if (isOutgoing) ChatBubbleOutgoing else ChatBubbleIncoming)
                .border(
                    1.dp,
                    if (isOutgoing) CyanNeon.copy(alpha = 0.3f) else MeshBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = if (isOutgoing) Color.White.copy(alpha = 0.7f) else TextSecondary
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusChip(
                            status = message.status,
                            hopCount = message.hopCount
                        )
                    }
                }
            }
        }

        // Multi-Hop Route Visualization (if routed across multiple hops)
        if (message.routeHistoryJson.isNotBlank() && message.routeHistoryJson != "[]") {
            Spacer(modifier = Modifier.height(4.dp))
            RoutePathCard(
                routeHistoryJson = message.routeHistoryJson,
                hopCount = message.hopCount,
                modifier = Modifier.widthIn(max = 290.dp)
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

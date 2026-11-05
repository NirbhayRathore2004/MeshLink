package com.meshlink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.meshlink.util.LogEntry
import com.meshlink.util.LogLevel
import com.meshlink.util.MeshLogger

@Composable
fun LogViewer(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var tagFilterExpanded by remember { mutableStateOf(false) }

    val tags = listOf("All", MeshLogger.TAG_MESH, MeshLogger.TAG_TRANSPORT, MeshLogger.TAG_ROUTING, MeshLogger.TAG_FORWARDING, MeshLogger.TAG_DELIVERY, MeshLogger.TAG_ACK)

    val filteredLogs = remember(logs, searchQuery, selectedTag) {
        logs.filter { entry ->
            val matchesTag = selectedTag == null || selectedTag == "All" || entry.tag.equals(selectedTag, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || entry.message.contains(searchQuery, ignoreCase = true) || entry.tag.contains(searchQuery, ignoreCase = true)
            matchesTag && matchesSearch
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MeshSurfaceDark)
            .border(1.dp, MeshBorder, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        // Controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter logs...", fontSize = 12.sp, color = TextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box {
                IconButton(onClick = { tagFilterExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter by Tag",
                        tint = if (selectedTag != null && selectedTag != "All") CyanNeon else TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = tagFilterExpanded,
                    onDismissRequest = { tagFilterExpanded = false },
                    modifier = Modifier.background(MeshSurfaceElevated)
                ) {
                    tags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag, color = TextPrimary) },
                            onClick = {
                                selectedTag = if (tag == "All") null else tag
                                tagFilterExpanded = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = { MeshLogger.clear() }) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = "Clear Logs",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Log list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MeshNavyDark)
                .padding(6.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Text(
                        text = "No mesh log events yet. Start the mesh or send a message.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(filteredLogs, key = { it.id }) { entry ->
                    LogItemRow(entry)
                }
            }
        }
    }
}

@Composable
private fun LogItemRow(entry: LogEntry) {
    val tagColor = when (entry.tag) {
        MeshLogger.TAG_MESH -> CyanNeon
        MeshLogger.TAG_TRANSPORT -> EmeraldGreen
        MeshLogger.TAG_ROUTING -> PurpleNeon
        MeshLogger.TAG_FORWARDING -> AmberOrange
        MeshLogger.TAG_DELIVERY -> Color(0xFF00E676)
        MeshLogger.TAG_ACK -> Color(0xFF00B0FF)
        else -> TextSecondary
    }

    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> TextSecondary
        LogLevel.INFO -> Color.White
        LogLevel.WARN -> AmberOrange
        LogLevel.ERROR -> RoseRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "${entry.getFormattedTime()} ",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TextSecondary
        )
        Text(
            text = "[${entry.tag}] ",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = tagColor
        )
        Text(
            text = entry.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = levelColor
        )
    }
}

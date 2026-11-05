package com.meshlink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.ui.theme.CyanNeon
import com.meshlink.ui.theme.MeshBorder
import com.meshlink.ui.theme.MeshSurfaceElevated
import com.meshlink.ui.theme.PurpleNeon
import com.meshlink.ui.theme.TextSecondary
import kotlinx.serialization.json.Json

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutePathCard(
    routeHistoryJson: String,
    hopCount: Int,
    modifier: Modifier = Modifier
) {
    val routeNodes: List<String> = try {
        Json.decodeFromString<List<String>>(routeHistoryJson)
    } catch (e: Exception) {
        emptyList()
    }

    if (routeNodes.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MeshSurfaceElevated.copy(alpha = 0.7f))
            .border(1.dp, MeshBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                contentDescription = null,
                tint = PurpleNeon,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Multi-Hop Mesh Route (${routeNodes.size - 1} hops):",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = PurpleNeon,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            routeNodes.forEachIndexed { index, node ->
                Text(
                    text = node.take(8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (index == 0) CyanNeon else if (index == routeNodes.lastIndex) MaterialTheme.colorScheme.secondary else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                if (index < routeNodes.lastIndex) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "to",
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

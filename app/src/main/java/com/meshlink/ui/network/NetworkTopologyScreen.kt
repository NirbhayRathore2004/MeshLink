package com.meshlink.ui.network

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.data.database.entity.RouteEntity
import com.meshlink.mesh.model.MeshNode
import com.meshlink.ui.theme.CyanNeon
import com.meshlink.ui.theme.EmeraldGreen
import com.meshlink.ui.theme.MeshBorder
import com.meshlink.ui.theme.MeshNavyDark
import com.meshlink.ui.theme.MeshSurfaceDark
import com.meshlink.ui.theme.MeshSurfaceElevated
import com.meshlink.ui.theme.PurpleNeon
import com.meshlink.ui.theme.TextPrimary
import com.meshlink.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkTopologyScreen(
    viewModel: NetworkViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mesh Network Topology",
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
        containerColor = MeshNavyDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Topology Graph Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(1.dp, MeshBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                TopologyCanvas(
                    localAlias = state.localNodeAlias,
                    activeNodes = state.activeNodes,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Topology Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MeshSurfaceElevated)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = CyanNeon, label = "This Node")
                LegendItem(color = EmeraldGreen, label = "Direct (1 Hop)")
                LegendItem(color = PurpleNeon, label = "Multi-Hop (>1 Hop)")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ROUTING TABLE & REACHABLE NODES (${state.routes.size})",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            if (state.routes.isEmpty() && state.activeNodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active mesh routes yet.\nConnect to neighbor devices to build the topology graph.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.routes, key = { it.destinationNodeId }) { route ->
                        RouteTableRow(route = route)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopologyCanvas(
    localAlias: String,
    activeNodes: List<MeshNode>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 24f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val centerOffset = Offset(centerX, centerY)

            // Draw concentric range circles
            drawCircle(
                color = MeshBorder.copy(alpha = 0.5f),
                radius = size.width * 0.28f,
                center = centerOffset,
                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )
            drawCircle(
                color = MeshBorder.copy(alpha = 0.3f),
                radius = size.width * 0.42f,
                center = centerOffset,
                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )

            // Draw surrounding nodes
            val directPeers = activeNodes.filter { it.isDirectNeighbor }
            val multiHopPeers = activeNodes.filter { !it.isDirectNeighbor }

            // Draw direct peers
            directPeers.forEachIndexed { index, node ->
                val angle = (2 * Math.PI / directPeers.size.coerceAtLeast(1)) * index
                val distance = size.width * 0.28f
                val nodeX = centerX + (distance * cos(angle)).toFloat()
                val nodeY = centerY + (distance * sin(angle)).toFloat()
                val nodeOffset = Offset(nodeX, nodeY)

                // Solid link to center
                drawLine(
                    color = EmeraldGreen.copy(alpha = 0.8f),
                    start = centerOffset,
                    end = nodeOffset,
                    strokeWidth = 3f
                )

                // Node circle
                drawCircle(
                    color = EmeraldGreen,
                    radius = 16f,
                    center = nodeOffset
                )
            }

            // Draw multi-hop peers on outer ring
            multiHopPeers.forEachIndexed { index, node ->
                val angle = (2 * Math.PI / multiHopPeers.size.coerceAtLeast(1)) * index + 0.5
                val distance = size.width * 0.42f
                val nodeX = centerX + (distance * cos(angle)).toFloat()
                val nodeY = centerY + (distance * sin(angle)).toFloat()
                val nodeOffset = Offset(nodeX, nodeY)

                // Dotted multi-hop link
                drawLine(
                    color = PurpleNeon.copy(alpha = 0.7f),
                    start = centerOffset,
                    end = nodeOffset,
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Node circle
                drawCircle(
                    color = PurpleNeon,
                    radius = 14f,
                    center = nodeOffset
                )
            }

            // Draw local center node with pulsing glow
            drawCircle(
                color = CyanNeon.copy(alpha = 0.25f),
                radius = pulseRadius,
                center = centerOffset
            )
            drawCircle(
                color = CyanNeon,
                radius = 22f,
                center = centerOffset
            )
        }

        // Center Node Label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = localAlias.ifBlank { "You" },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MeshNavyDark
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, color = TextPrimary)
    }
}

@Composable
private fun RouteTableRow(route: RouteEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MeshBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                contentDescription = null,
                tint = PurpleNeon,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Destination: ${route.destinationNodeId}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Next Hop: ${route.nextHopNodeId} (via endpoint: ${route.nextHopEndpointId})",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            }
            Text(
                text = "${route.hopCount} Hops",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanNeon,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MeshSurfaceElevated)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

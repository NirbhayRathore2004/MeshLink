package com.meshlink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meshlink.data.model.MessageStatus
import com.meshlink.ui.theme.EmeraldGreen
import com.meshlink.ui.theme.RoseRed
import com.meshlink.ui.theme.StatusDelivered
import com.meshlink.ui.theme.StatusPending
import com.meshlink.ui.theme.StatusSent

@Composable
fun StatusChip(
    status: MessageStatus,
    hopCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val (label, bgColor, textColor) = when (status) {
        MessageStatus.DELIVERED -> {
            val hopText = if (hopCount > 1) "$hopCount hops" else if (hopCount == 1) "1 hop" else "Direct"
            Triple("✓✓ Delivered ($hopText)", StatusDelivered.copy(alpha = 0.15f), StatusDelivered)
        }
        MessageStatus.SENT_TO_PEER -> Triple("✓ Sent to Mesh", StatusSent.copy(alpha = 0.15f), StatusSent)
        MessageStatus.FORWARDED -> Triple("⇄ Forwarded", Color(0xFF7C4DFF).copy(alpha = 0.15f), Color(0xFFB388FF))
        MessageStatus.PENDING -> Triple("◷ Queued", StatusPending.copy(alpha = 0.15f), StatusPending)
        MessageStatus.FAILED -> Triple("✕ Failed", RoseRed.copy(alpha = 0.15f), RoseRed)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

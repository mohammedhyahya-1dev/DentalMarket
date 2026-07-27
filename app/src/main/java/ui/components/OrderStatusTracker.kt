package com.dentalmarket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dentalmarket.app.ui.theme.DeepTeal

// The order fulfillment pipeline, in order. CANCELLED is a separate terminal
// state handled outside this list rather than a step in it.
private val ORDER_TRACKER_STEPS = listOf(
    "PLACED" to "Placed",
    "PICKED_UP" to "Picked Up",
    "DELIVERED" to "Delivered",
    "PAID_TO_SELLER" to "Paid"
)

private val CancelledRed = Color(0xFFEF4444)

@Composable
fun OrderStatusTracker(
    status: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (status == "CANCELLED") {
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Cancel,
                contentDescription = null,
                tint = CancelledRed,
                modifier = Modifier.size(if (compact) 16.dp else 20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Order Cancelled",
                color = CancelledRed,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall
            )
        }
        return
    }

    val currentIndex = ORDER_TRACKER_STEPS.indexOfFirst { it.first == status }.coerceAtLeast(0)
    val dotSize = if (compact) 16.dp else 28.dp
    val lineHeight = if (compact) 2.dp else 3.dp
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ORDER_TRACKER_STEPS.forEachIndexed { index, _ ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(lineHeight)
                            .background(if (index <= currentIndex) DeepTeal else inactiveColor)
                    )
                }
                val isCompleted = index < currentIndex
                val isCurrent = index == currentIndex
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(if (isCompleted || isCurrent) DeepTeal else inactiveColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(dotSize * 0.65f)
                        )
                    } else if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .size(dotSize * 0.4f)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
        if (!compact) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ORDER_TRACKER_STEPS.forEachIndexed { index, (_, label) ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index <= currentIndex) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                ORDER_TRACKER_STEPS[currentIndex].second,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

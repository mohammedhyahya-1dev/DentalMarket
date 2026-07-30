package com.dentalmarket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dentalmarket.app.model.PaymentStatus

@Composable
fun PaymentStatusBadge(status: String, modifier: Modifier = Modifier) {
    val label = PaymentStatus.entries.find { it.name == status }?.label ?: status
    val color = when (status) {
        "AWAITING_PAYMENT" -> Color(0xFF64748B)
        "PENDING_VERIFICATION" -> Color(0xFFF59E0B)
        "VERIFIED" -> Color(0xFF10B981)
        "REJECTED" -> Color(0xFFEF4444)
        else -> Color.Gray
    }
    Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

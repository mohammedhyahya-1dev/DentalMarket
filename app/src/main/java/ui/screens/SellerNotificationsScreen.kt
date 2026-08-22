package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.model.SellerNotification
import com.dentalmarket.app.model.SellerNotificationType
import com.dentalmarket.app.viewmodel.NotificationViewModel
import java.text.DateFormat
import java.util.Date

// Deliberately just a flat list of handoff info, not the full seller-orders
// view — no status tracker, no actions. See the roadmap note in
// SellerNotification.kt.
//
// Tap-to-navigate was never wired up for ANY notification type before
// today (DELIVERY_INFO/DISPUTE_OPENED/DISPUTE_RESOLVED all carry a real
// orderId and went nowhere on tap) — fixed for all four types together
// rather than leaving the pre-existing three broken while only wiring up
// the new one.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerNotificationsScreen(
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    onVerificationSubmittedClick: (String) -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications = viewModel.notifications.value
    val isLoading = viewModel.isLoading.value

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            viewModel.markAllRead()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                notifications.isEmpty() -> Text(
                    "No notifications yet",
                    style = MaterialTheme.typography.titleMedium
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                // orderId means different things per type —
                                // a real order id for the three order-
                                // related types, but the submitter's uid
                                // for VERIFICATION_SUBMITTED (see
                                // SellerNotificationRepository's own
                                // comment on that reuse).
                                if (notification.type == SellerNotificationType.VERIFICATION_SUBMITTED) {
                                    onVerificationSubmittedClick(notification.orderId)
                                } else if (notification.orderId.isNotBlank()) {
                                    onOrderClick(notification.orderId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: SellerNotification, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Text(notification.listingName, style = MaterialTheme.typography.titleMedium)
            Text(
                DateFormat.getDateTimeInstance().format(Date(notification.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (notification.type) {
                SellerNotificationType.DISPUTE_OPENED -> Text(
                    "A dispute has been filed on this order. The payout is on hold until DentalMarket resolves it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                SellerNotificationType.DISPUTE_RESOLVED -> Text(
                    disputeResolvedMessage(notification.resolution),
                    style = MaterialTheme.typography.bodyMedium
                )
                SellerNotificationType.VERIFICATION_SUBMITTED -> Text(
                    "${notification.buyerName} submitted documents for identity verification review.",
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> {
                    Text("Buyer: ${notification.buyerName}", style = MaterialTheme.typography.bodyMedium)
                    Text("Deliver to: ${notification.deliveryAddress}", style = MaterialTheme.typography.bodyMedium)
                    Text("Contact: ${notification.deliveryContactPhone}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun disputeResolvedMessage(resolution: String): String = when (resolution) {
    "RESOLVED_SELLER" -> "The dispute on this order has been resolved in your favor. The payout will proceed as normal."
    "RESOLVED_BUYER" -> "The dispute on this order has been resolved in the buyer's favor. This order will not proceed to payout."
    else -> "The dispute on this order has been dismissed. The payout will proceed as normal."
}

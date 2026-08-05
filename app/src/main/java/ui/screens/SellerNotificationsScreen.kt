package com.dentalmarket.app.ui.screens

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
import com.dentalmarket.app.viewmodel.NotificationViewModel
import java.text.DateFormat
import java.util.Date

// Deliberately just a flat list of handoff info, not the full seller-orders
// view — no status tracker, no actions. See the roadmap note in
// SellerNotification.kt.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerNotificationsScreen(
    onBack: () -> Unit,
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
                        NotificationCard(notification)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: SellerNotification) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Text(notification.listingName, style = MaterialTheme.typography.titleMedium)
            Text(
                DateFormat.getDateTimeInstance().format(Date(notification.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Buyer: ${notification.buyerName}", style = MaterialTheme.typography.bodyMedium)
            Text("Deliver to: ${notification.deliveryAddress}", style = MaterialTheme.typography.bodyMedium)
            Text("Contact: ${notification.deliveryContactPhone}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.ui.components.OrderStatusTracker
import com.dentalmarket.app.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderViewModel = viewModel()
) {
    val order = viewModel.selectedOrder.value
    val isLoading = viewModel.isLoadingOrder.value

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val currentOrder = order
        if (isLoading || currentOrder == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) CircularProgressIndicator() else Text("Order not found")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentOrder.listingEmoji, fontSize = 40.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(currentOrder.listingName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Qty: ${currentOrder.quantity} • $" + "%.2f".format(currentOrder.price * currentOrder.quantity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("Status", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OrderStatusTracker(currentOrder.status, compact = false)

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Seller", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(currentOrder.sellerName, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Buyer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(currentOrder.buyerName, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Delivery", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(
                if (currentOrder.deliveryMethod == "DENTALMARKET_DELIVERS") {
                    "DentalMarket delivers"
                } else {
                    "Seller delivers"
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Safety Fee", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(
                "$" + "%.2f".format(currentOrder.safetyFee),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Delivery Address", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(currentOrder.deliveryAddress, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Delivery Contact Phone", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(currentOrder.deliveryContactPhone, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

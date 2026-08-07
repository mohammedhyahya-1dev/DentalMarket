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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.model.Order
import com.dentalmarket.app.ui.components.OrderStatusTracker
import com.dentalmarket.app.ui.components.PaymentStatusBadge
import com.dentalmarket.app.viewmodel.OrderViewModel
import com.dentalmarket.app.viewmodel.calculatePayout

// Read-only — every order mutation (status advance, payment verify/reject,
// delivery confirmation) is exclusively admin- or buyer-driven, unchanged by
// this screen. Deliberately never touches deliveryAddress/deliveryContactPhone
// — those live in orderDeliveryInfo/{orderId}, which sellers have no read
// access to at all (see firestore.rules); this screen doesn't even try.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersScreen(
    onBack: () -> Unit,
    viewModel: OrderViewModel = viewModel()
) {
    val orders = viewModel.orders.value
    val isLoading = viewModel.isLoading.value

    LaunchedEffect(Unit) {
        viewModel.loadOrdersForSeller()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Sales", style = MaterialTheme.typography.titleLarge) },
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
                orders.isEmpty() -> Text(
                    "No orders on your listings yet",
                    style = MaterialTheme.typography.titleMedium
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        SellerOrderCard(order)
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerOrderCard(order: Order) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.listingEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.listingName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$" + "%.2f".format(order.price * order.quantity) + " • Qty ${order.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Buyer: ${order.buyerName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            PaymentStatusBadge(order.paymentStatus)
            Spacer(modifier = Modifier.height(10.dp))
            OrderStatusTracker(order.status, compact = true)
            Spacer(modifier = Modifier.height(10.dp))
            PayoutRow(order)
        }
    }
}

// Locked-in actual figures (order.commissionPercentage/deliveryFee), not a
// live estimate — this is the seller's own money on an order that already
// happened, same calculatePayout() AdminOrdersScreen uses for its own
// reference display.
@Composable
private fun PayoutRow(order: Order) {
    val breakdown = calculatePayout(order.price * order.quantity, order.commissionPercentage, order.deliveryFee)
    Column {
        Text(
            "Item total: $" + "%.2f".format(breakdown.itemTotal) +
                "  •  Platform fee (${"%.1f".format(order.commissionPercentage)}%): $" +
                "%.2f".format(breakdown.commissionAmount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        if (breakdown.deliveryFee > 0) {
            Text(
                "Delivery fee: $" + "%.2f".format(breakdown.deliveryFee),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Text(
            "You receive: $" + "%.2f".format(breakdown.sellerReceives),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

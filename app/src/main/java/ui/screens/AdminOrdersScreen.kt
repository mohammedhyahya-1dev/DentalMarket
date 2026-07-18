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
import com.dentalmarket.app.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    onBack: () -> Unit,
    viewModel: OrderViewModel = viewModel()
) {
    val orders = viewModel.orders.value
    val isLoading = viewModel.isLoading.value

    LaunchedEffect(Unit) {
        viewModel.loadAllOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Orders (Admin)", style = MaterialTheme.typography.titleLarge) },
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
                orders.isEmpty() -> Text("No orders yet", style = MaterialTheme.typography.titleMedium)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        AdminOrderCard(order = order, onAdvance = { viewModel.advanceStatus(order) })
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(order: Order, onAdvance: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.listingEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.listingName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$" + "%.2f".format(order.price * order.quantity) + " \u2022 Qty ${order.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Buyer: ${order.buyerName}", style = MaterialTheme.typography.bodySmall)
            Text("Seller: ${order.sellerName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            StatusBadge(order.status)
            if (order.status != "PAID_TO_SELLER" && order.status != "CANCELLED") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) {
                    Text("Advance to Next Stage")
                }
            }
        }
    }
}
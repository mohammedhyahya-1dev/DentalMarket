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
import com.dentalmarket.app.ui.components.PaymentStatusBadge
import com.dentalmarket.app.viewmodel.OrderViewModel
import com.dentalmarket.app.viewmodel.calculatePayout
import com.dentalmarket.app.viewmodel.nextOrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    onBack: () -> Unit,
    viewModel: OrderViewModel = viewModel()
) {
    val orders = viewModel.orders.value
    val isLoading = viewModel.isLoading.value
    var orderToReject by remember { mutableStateOf<Order?>(null) }

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
                        AdminOrderCard(
                            order = order,
                            onAdvance = { viewModel.advanceStatus(order) },
                            onVerifyPayment = { viewModel.verifyPayment(order) },
                            onRejectPaymentClick = { orderToReject = order }
                        )
                    }
                }
            }
        }
    }

    orderToReject?.let { order ->
        RejectPaymentDialog(
            order = order,
            onDismiss = { orderToReject = null },
            onConfirm = { reason ->
                viewModel.rejectPayment(order, reason)
                orderToReject = null
            }
        )
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    onAdvance: () -> Unit,
    onVerifyPayment: (() -> Unit)? = null,
    onRejectPaymentClick: (() -> Unit)? = null
) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(order.status)
                PaymentStatusBadge(order.paymentStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))
            CommissionBreakdownRow(order)

            Spacer(modifier = Modifier.height(8.dp))
            DeliveryInfoRow(order)

            Spacer(modifier = Modifier.height(8.dp))
            SafetyFeeRow(order)

            if (order.paymentStatus == "PENDING_VERIFICATION") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Reference: ${order.paymentReference}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onVerifyPayment?.invoke() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Verify Payment")
                    }
                    OutlinedButton(
                        onClick = { onRejectPaymentClick?.invoke() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reject Payment")
                    }
                }
            } else if (order.paymentStatus == "REJECTED" && order.paymentRejectionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Rejected: ${order.paymentRejectionReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (nextOrderStatus(order.status, order.paymentStatus, order.deliveryMethod) != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) {
                    Text("Advance to Next Stage")
                }
            } else if (order.status == "PICKED_UP" && order.deliveryMethod == "SELLER_DELIVERS") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Waiting for buyer to confirm receipt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun DeliveryInfoRow(order: Order) {
    val methodLabel = if (order.deliveryMethod == "DENTALMARKET_DELIVERS") {
        "DentalMarket delivers (fee deducted from seller: $" + "%.2f".format(order.deliveryFee) + ")"
    } else {
        "Seller delivers"
    }
    Text(
        "Delivery: $methodLabel",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
}

// Reconciliation visibility only — this fee is buyer-paid and never reaches
// the seller, so it deliberately has no part in CommissionBreakdownRow or
// the seller-payout math above.
@Composable
private fun SafetyFeeRow(order: Order) {
    Text(
        "Safety fee collected: $" + "%.2f".format(order.safetyFee),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
}

// The manual-payout reference: this app has no automated payment API, so
// this is purely a calculated display telling the admin what to actually
// send the seller — nothing here transfers money. Both commission and (when
// DENTALMARKET_DELIVERS) the delivery fee come out of the seller's payout.
@Composable
private fun CommissionBreakdownRow(order: Order) {
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
            "Seller receives: $" + "%.2f".format(breakdown.sellerReceives),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RejectPaymentDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Payment") },
        text = {
            Column {
                Text(
                    "Reference: ${order.paymentReference}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(reason) }) {
                Text("Reject")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
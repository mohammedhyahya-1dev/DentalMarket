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
import com.dentalmarket.app.model.DisputeReason
import com.dentalmarket.app.model.DisputeStatus
import com.dentalmarket.app.ui.components.OrderStatusTracker
import com.dentalmarket.app.viewmodel.DisputeViewModel
import com.dentalmarket.app.viewmodel.OrderViewModel
import com.dentalmarket.app.model.formatPrice
import com.dentalmarket.app.model.formatPriceOrFree
import com.dentalmarket.app.model.roundPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderViewModel = viewModel(),
    disputeViewModel: DisputeViewModel = viewModel()
) {
    val order = viewModel.selectedOrder.value
    val isLoading = viewModel.isLoadingOrder.value
    val deliveryInfo = viewModel.selectedOrderDeliveryInfo.value
    val dispute by disputeViewModel.dispute.collectAsState()
    val disputeErrorMessage by disputeViewModel.errorMessage.collectAsState()
    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf(DisputeReason.NOT_DELIVERED) }
    var disputeDetails by remember { mutableStateOf("") }

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
        viewModel.loadOrderDeliveryInfo(orderId)
        disputeViewModel.loadDispute(orderId)
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
                        "Qty: ${currentOrder.quantity} • " +
                            formatPrice(roundPrice(currentOrder.price) * currentOrder.quantity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("Status", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OrderStatusTracker(currentOrder.status, compact = false)

            val currentDispute = dispute
            Spacer(modifier = Modifier.height(20.dp))
            if (currentDispute != null) {
                Text(
                    "Dispute status: " + (DisputeStatus.entries.find { it.name == currentDispute.status }?.label
                        ?: currentDispute.status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (currentOrder.paymentStatus == "VERIFIED") {
                OutlinedButton(
                    onClick = { showDisputeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Report a Problem")
                }
            }

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
                formatPriceOrFree(currentOrder.safetyFee),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Delivery Address", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(deliveryInfo?.deliveryAddress ?: "…", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Delivery Contact Phone", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(deliveryInfo?.deliveryContactPhone ?: "…", style = MaterialTheme.typography.bodyLarge)

            if (showDisputeDialog) {
                var reasonExpanded by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = { showDisputeDialog = false; disputeViewModel.clearError() },
                    title = { Text("Report a Problem") },
                    text = {
                        Column {
                            ExposedDropdownMenuBox(
                                expanded = reasonExpanded,
                                onExpandedChange = { reasonExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = disputeReason.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Reason") },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = reasonExpanded,
                                    onDismissRequest = { reasonExpanded = false }
                                ) {
                                    DisputeReason.entries.forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text(r.label) },
                                            onClick = {
                                                disputeReason = r
                                                reasonExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = disputeDetails,
                                onValueChange = { disputeDetails = it },
                                label = { Text("Details (optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            disputeErrorMessage?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                disputeViewModel.fileDispute(currentOrder, disputeReason, disputeDetails) {
                                    showDisputeDialog = false
                                    disputeDetails = ""
                                }
                            }
                        ) {
                            Text("Submit")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDisputeDialog = false; disputeViewModel.clearError() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

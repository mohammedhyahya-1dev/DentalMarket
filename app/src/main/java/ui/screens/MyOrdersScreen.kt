package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.data.PaymentConfigRepository
import com.dentalmarket.app.model.Order
import com.dentalmarket.app.model.OrderStatus
import com.dentalmarket.app.model.PaymentConfig
import com.dentalmarket.app.model.Rating
import com.dentalmarket.app.ui.components.OrderStatusTracker
import com.dentalmarket.app.ui.components.PaymentReferenceDialog
import com.dentalmarket.app.ui.components.PaymentStatusBadge
import com.dentalmarket.app.viewmodel.OrderViewModel
import com.dentalmarket.app.viewmodel.RatingViewModel
import com.dentalmarket.app.model.formatPrice
import com.dentalmarket.app.model.roundPrice

private val RATABLE_STATUSES = setOf("DELIVERED", "PAID_TO_SELLER")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    onBack: () -> Unit,
    onOrderClick: (Order) -> Unit = {},
    viewModel: OrderViewModel = viewModel(),
    ratingViewModel: RatingViewModel = viewModel()
) {
    val orders = viewModel.orders.value
    val isLoading = viewModel.isLoading.value
    val ratedOrderIds by ratingViewModel.ratedOrderIds.collectAsState()
    val isSubmittingRating by ratingViewModel.isLoading.collectAsState()
    val ratingErrorMessage by ratingViewModel.errorMessage.collectAsState()
    var orderToRate by remember { mutableStateOf<Order?>(null) }

    val paymentConfigRepository = remember { PaymentConfigRepository() }
    var paymentConfig by remember { mutableStateOf<PaymentConfig?>(null) }
    var paymentConfigLoadFailed by remember { mutableStateOf(false) }
    var orderForPayment by remember { mutableStateOf<Order?>(null) }
    val isSubmittingPayment = viewModel.isSubmittingPayment.value
    val paymentErrorMessage = viewModel.paymentErrorMessage.value

    LaunchedEffect(Unit) {
        viewModel.loadMyOrders()
        ratingViewModel.loadMyRatedOrders()
        paymentConfigRepository.getPaymentConfig()
            .onSuccess { paymentConfig = it }
            .onFailure { paymentConfigLoadFailed = true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders", style = MaterialTheme.typography.titleLarge) },
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
                    "You haven't placed any orders yet",
                    style = MaterialTheme.typography.titleMedium
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            canRate = order.status in RATABLE_STATUSES && order.id !in ratedOrderIds,
                            onRateClick = { orderToRate = order },
                            onClick = { onOrderClick(order) },
                            onEnterPaymentReferenceClick = { orderForPayment = order },
                            onConfirmDeliveryClick = { viewModel.confirmDelivery(order) }
                        )
                    }
                }
            }
        }
    }

    orderToRate?.let { order ->
        RateSellerDialog(
            order = order,
            isSubmitting = isSubmittingRating,
            errorMessage = ratingErrorMessage,
            onDismiss = { orderToRate = null },
            onSubmit = { stars, review ->
                ratingViewModel.submitRating(
                    Rating(
                        orderId = order.id,
                        listingId = order.listingId,
                        listingName = order.listingName,
                        sellerId = order.sellerId,
                        sellerName = order.sellerName,
                        buyerId = order.buyerId,
                        buyerName = order.buyerName,
                        stars = stars,
                        review = review
                    )
                ) {
                    orderToRate = null
                }
            }
        )
    }

    orderForPayment?.let { order ->
        PaymentReferenceDialog(
            order = order,
            paymentConfig = paymentConfig,
            paymentConfigLoadFailed = paymentConfigLoadFailed,
            isSubmitting = isSubmittingPayment,
            errorMessage = paymentErrorMessage,
            onDismiss = { orderForPayment = null },
            onSubmit = { reference ->
                viewModel.submitPaymentReference(order, reference) {
                    orderForPayment = null
                }
            }
        )
    }
}

@Composable
fun OrderCard(
    order: Order,
    canRate: Boolean = false,
    onRateClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onEnterPaymentReferenceClick: (() -> Unit)? = null,
    onConfirmDeliveryClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.listingEmoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.listingName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Qty: ${order.quantity} • " + formatPrice(roundPrice(order.price) * order.quantity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            PaymentStatusBadge(order.paymentStatus)
            if (order.paymentStatus == "REJECTED" && order.paymentRejectionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Reason: ${order.paymentRejectionReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Delivery: " + if (order.deliveryMethod == "DENTALMARKET_DELIVERS") {
                    "DentalMarket delivers"
                } else {
                    "Seller delivers"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (order.safetyFee > 0) {
                Text(
                    "Safety fee: " + formatPrice(order.safetyFee),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OrderStatusTracker(order.status, compact = true)
            val canEnterReference = order.paymentStatus == "AWAITING_PAYMENT" || order.paymentStatus == "REJECTED"
            if (canEnterReference && onEnterPaymentReferenceClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onEnterPaymentReferenceClick, modifier = Modifier.fillMaxWidth()) {
                    Text(if (order.paymentStatus == "REJECTED") "Resubmit Payment Reference" else "Enter Payment Reference")
                }
            }
            val canConfirmDelivery = order.status == "PICKED_UP" && order.deliveryMethod == "SELLER_DELIVERS"
            if (canConfirmDelivery && onConfirmDeliveryClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onConfirmDeliveryClick, modifier = Modifier.fillMaxWidth()) {
                    Text("I received this")
                }
            }
            if (canRate && onRateClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onRateClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Rate this Seller")
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val label = OrderStatus.entries.find { it.name == status }?.label ?: status
    val color = when (status) {
        "PLACED" -> Color(0xFF64748B)
        "PICKED_UP" -> Color(0xFFF59E0B)
        "DELIVERED" -> Color(0xFF3B82F6)
        "PAID_TO_SELLER" -> Color(0xFF10B981)
        "CANCELLED" -> Color(0xFFEF4444)
        else -> Color.Gray
    }
    Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateSellerDialog(
    order: Order,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (stars: Int, review: String) -> Unit
) {
    var stars by remember { mutableIntStateOf(5) }
    var review by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate ${order.sellerName}") },
        text = {
            Column {
                Text(order.listingName, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    for (i in 1..5) {
                        IconButton(onClick = { stars = i }) {
                            Icon(
                                if (i <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "$i star",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("Review (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(stars, review) },
                enabled = !isSubmitting
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

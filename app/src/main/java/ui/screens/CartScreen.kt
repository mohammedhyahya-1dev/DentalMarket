package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.PaymentConfigRepository
import com.dentalmarket.app.model.PaymentConfig
import com.dentalmarket.app.ui.components.CartItemRow
import com.dentalmarket.app.ui.components.GuestSignInPrompt
import com.dentalmarket.app.ui.components.PaymentReferenceDialog
import com.dentalmarket.app.ui.components.VerifyEmailPrompt
import com.dentalmarket.app.ui.theme.WarmAmber
import com.dentalmarket.app.viewmodel.CartViewModel
import com.dentalmarket.app.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    orderViewModel: OrderViewModel = viewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val isPlacingOrder = cartViewModel.isPlacingOrder.value
    val orderPlacedSuccess = cartViewModel.orderPlacedSuccess.value
    val orderErrorMessage = cartViewModel.orderErrorMessage.value
    val authRepository = remember { AuthRepository() }
    val isGuest = authRepository.isAnonymous
    var showGuestPrompt by remember { mutableStateOf(false) }
    val safetyFeeAmount = cartViewModel.safetyFeeAmount.value ?: 0.0
    val deliveryAddress = cartViewModel.deliveryAddress.value
    val deliveryContactPhone = cartViewModel.deliveryContactPhone.value

    val paymentConfigRepository = remember { PaymentConfigRepository() }
    var paymentConfig by remember { mutableStateOf<PaymentConfig?>(null) }
    var paymentConfigLoadFailed by remember { mutableStateOf(false) }
    val isSubmittingPayment = orderViewModel.isSubmittingPayment.value
    val paymentErrorMessage = orderViewModel.paymentErrorMessage.value

    val createdOrders = cartViewModel.createdOrders.value
    var paymentQueueIndex by remember { mutableStateOf(0) }
    LaunchedEffect(createdOrders) {
        paymentQueueIndex = 0
    }

    LaunchedEffect(Unit) {
        cartViewModel.loadSafetyFeeConfig()
        cartViewModel.loadBuyerDeliveryInfo()
        paymentConfigRepository.getPaymentConfig()
            .onSuccess { paymentConfig = it }
            .onFailure { paymentConfigLoadFailed = true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Pay via QiCard or ZainCash \u2014 you'll submit your transaction reference after placing your order.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { cartViewModel.updateDeliveryAddress(it) },
                        label = { Text("Delivery Address") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = deliveryContactPhone,
                        onValueChange = { cartViewModel.updateDeliveryContactPhone(it) },
                        label = { Text("Delivery Contact Phone") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    orderErrorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    val itemTotal = cartItems.sumOf { it.listing.price * it.quantity }
                    val safetyFeeTotal = cartItems
                        .filter { it.safetyFeeSelected }
                        .sumOf { safetyFeeAmount }
                    // Flat per listing, not multiplied by quantity — same
                    // treatment as safetyFeeTotal above.
                    val shippingTotal = cartItems
                        .filter { it.listing.deliveryMethod == "SELLER_DELIVERS" }
                        .sumOf { it.listing.shippingPrice }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total: $" + "%.2f".format(itemTotal + safetyFeeTotal + shippingTotal),
                            style = MaterialTheme.typography.titleLarge,
                            color = WarmAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (isGuest) showGuestPrompt = true else cartViewModel.checkout()
                        },
                        enabled = !isPlacingOrder && deliveryAddress.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isPlacingOrder) "Placing Order..." else "Place Order (QiCard/ZainCash)")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isPlacingOrder -> CircularProgressIndicator()
                orderPlacedSuccess && cartItems.isEmpty() -> {
                    Text(
                        "Order placed! \u2705\nWe'll be in touch to arrange delivery.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                cartItems.isEmpty() -> {
                    Text("Your cart is empty", style = MaterialTheme.typography.titleMedium)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(cartItems, key = { it.listing.id }) { item ->
                            CartItemRow(
                                item = item,
                                onIncrease = { cartViewModel.updateQuantity(item.listing.id, item.quantity + 1) },
                                onDecrease = { cartViewModel.updateQuantity(item.listing.id, item.quantity - 1) },
                                onRemove = { cartViewModel.removeFromCart(item.listing.id) },
                                onToggleSafetyFee = { cartViewModel.toggleSafetyFee(item.listing.id) },
                                safetyFeeAmount = safetyFeeAmount
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showGuestPrompt) {
        GuestSignInPrompt(
            onDismiss = { showGuestPrompt = false },
            onSignIn = {
                showGuestPrompt = false
                onRequireLogin()
            }
        )
    }

    if (cartViewModel.showVerifyEmailPrompt.value) {
        VerifyEmailPrompt(
            actionLabel = "checking out",
            isResending = cartViewModel.isResendingVerification.value,
            resendSuccess = cartViewModel.resendVerificationSuccess.value,
            resendError = cartViewModel.resendVerificationError.value,
            onDismiss = { cartViewModel.dismissVerifyEmailPrompt() },
            onResend = { cartViewModel.resendVerificationEmail() }
        )
    }

    // Prompt for a payment reference on each order just placed, one at a
    // time — the buyer no longer has to find each one separately in My
    // Orders. Dismissing/canceling one just moves to the next; that order
    // simply stays Awaiting Payment, same as it would without this prompt,
    // and "Enter Payment Reference" in My Orders remains there to finish it
    // later.
    //
    // Reaching the end of the queue clears CartViewModel.createdOrders
    // rather than just resetting the local index — CartViewModel outlives
    // this screen, so leaving that list populated would let a completed
    // checkout's orders leak into the next time CartScreen is entered and
    // immediately re-show a payment dialog for an already-finished checkout.
    fun advancePaymentQueue() {
        if (paymentQueueIndex + 1 >= createdOrders.size) {
            cartViewModel.clearCreatedOrders()
            paymentQueueIndex = 0
        } else {
            paymentQueueIndex += 1
        }
    }

    if (paymentQueueIndex < createdOrders.size) {
        val orderForPayment = createdOrders[paymentQueueIndex]
        PaymentReferenceDialog(
            order = orderForPayment,
            paymentConfig = paymentConfig,
            paymentConfigLoadFailed = paymentConfigLoadFailed,
            isSubmitting = isSubmittingPayment,
            errorMessage = paymentErrorMessage,
            onDismiss = { advancePaymentQueue() },
            onSubmit = { reference ->
                orderViewModel.submitPaymentReference(orderForPayment, reference) {
                    advancePaymentQueue()
                }
            }
        )
    }
}
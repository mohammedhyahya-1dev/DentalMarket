package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.OrderRepository
import com.dentalmarket.app.data.SellerNotificationRepository
import com.dentalmarket.app.model.Order
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    private val repository = OrderRepository()
    private val authRepository = AuthRepository()
    private val notificationRepository = SellerNotificationRepository()

    var orders = mutableStateOf<List<Order>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    var selectedOrder = mutableStateOf<Order?>(null)
    var isLoadingOrder = mutableStateOf(false)

    // Separate from isLoading/errorMessage above so submitting a payment
    // reference from a dialog doesn't trigger the full-screen list spinner.
    var isSubmittingPayment = mutableStateOf(false)
    var paymentErrorMessage = mutableStateOf<String?>(null)

    fun loadOrder(orderId: String) {
        isLoadingOrder.value = true
        viewModelScope.launch {
            val result = repository.getOrderById(orderId)
            isLoadingOrder.value = false
            result.onSuccess { selectedOrder.value = it }
            result.onFailure { errorMessage.value = it.message }
        }
    }

    fun loadMyOrders() {
        val buyerId = authRepository.currentUserId ?: return
        isLoading.value = true
        viewModelScope.launch {
            val result = repository.getOrdersForBuyer(buyerId)
            isLoading.value = false
            result.onSuccess { orders.value = it }
            result.onFailure { errorMessage.value = it.message }
        }
    }

    fun loadAllOrders() {
        isLoading.value = true
        viewModelScope.launch {
            val result = repository.getAllOrders()
            isLoading.value = false
            result.onSuccess { orders.value = it }
            result.onFailure { errorMessage.value = it.message }
        }
    }

    // Moves an order to the next step in the fulfillment pipeline. The
    // admin UI already hides this button when nextOrderStatus() would
    // return null, but the check is repeated here too, same defense-in-depth
    // as everywhere else in this app.
    fun advanceStatus(order: Order) {
        val next = nextOrderStatus(order.status, order.paymentStatus, order.deliveryMethod) ?: return
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, next)
            loadAllOrders()
        }
    }

    // The buyer's own delivery confirmation for SELLER_DELIVERS orders —
    // refreshes the buyer's list, mirroring submitPaymentReference() below.
    fun confirmDelivery(order: Order, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.confirmDelivery(order.id)
                .onSuccess {
                    loadMyOrders()
                    onSuccess()
                }
        }
    }

    fun submitPaymentReference(order: Order, reference: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSubmittingPayment.value = true
            paymentErrorMessage.value = null
            repository.submitPaymentReference(order.id, reference)
                .onSuccess {
                    loadMyOrders()
                    onSuccess()
                }
                .onFailure { paymentErrorMessage.value = it.message ?: "Failed to submit payment reference" }
            isSubmittingPayment.value = false
        }
    }

    fun verifyPayment(order: Order) {
        viewModelScope.launch {
            repository.verifyPayment(order.id)
                .onSuccess {
                    // SELLER_DELIVERS only — for DENTALMARKET_DELIVERS, admin
                    // already sees this info as the courier and doesn't need
                    // to relay it to the seller.
                    if (order.deliveryMethod == "SELLER_DELIVERS") {
                        notificationRepository.createNotification(order)
                    }
                }
            loadAllOrders()
        }
    }

    fun rejectPayment(order: Order, reason: String) {
        viewModelScope.launch {
            repository.rejectPayment(order.id, reason)
            loadAllOrders()
        }
    }
}

// Pure decision function for the fulfillment pipeline, kept free of any
// Firebase/ViewModel dependency so it's directly unit-testable. Returns null
// when there's no next step (terminal status) or the step is blocked
// (PLACED -> PICKED_UP requires a verified payment; PICKED_UP -> DELIVERED
// is blocked for the admin/advance-button path when the buyer chose
// SELLER_DELIVERS, since that transition can only come from the buyer's own
// confirmDelivery() action instead).
fun nextOrderStatus(currentStatus: String, paymentStatus: String, deliveryMethod: String): String? {
    return when (currentStatus) {
        "PLACED" -> if (paymentStatus == "VERIFIED") "PICKED_UP" else null
        "PICKED_UP" -> if (deliveryMethod == "DENTALMARKET_DELIVERS") "DELIVERED" else null
        "DELIVERED" -> "PAID_TO_SELLER"
        else -> null
    }
}

data class PayoutBreakdown(
    val itemTotal: Double,
    val commissionAmount: Double,
    val deliveryFee: Double,
    val sellerReceives: Double
)

// Pure calculation, free of any Firebase/ViewModel dependency, same reasoning
// as nextOrderStatus() above. Used both for the locked-in per-order figures
// (Order.commissionPercentage, Order.deliveryFee) and for live pre-sale
// estimates (Sell screen, listing detail) against the current config values.
// deliveryFee has no default — it's financial math, and a silently-assumed
// 0.0 at a call site that forgot to pass it would just look like an
// (incorrectly) higher payout rather than fail loudly.
fun calculatePayout(itemTotal: Double, commissionPercentage: Double, deliveryFee: Double): PayoutBreakdown {
    val commissionAmount = itemTotal * (commissionPercentage / 100.0)
    return PayoutBreakdown(
        itemTotal = itemTotal,
        commissionAmount = commissionAmount,
        deliveryFee = deliveryFee,
        sellerReceives = itemTotal - commissionAmount - deliveryFee
    )
}
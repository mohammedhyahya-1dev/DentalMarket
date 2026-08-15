package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.DisputeRepository
import com.dentalmarket.app.data.OrderDeliveryInfoRepository
import com.dentalmarket.app.data.OrderRepository
import com.dentalmarket.app.data.SellerNotificationRepository
import com.dentalmarket.app.model.Dispute
import com.dentalmarket.app.model.Order
import com.dentalmarket.app.model.OrderDeliveryInfo
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    private val repository = OrderRepository()
    private val authRepository = AuthRepository()
    private val notificationRepository = SellerNotificationRepository()
    private val deliveryInfoRepository = OrderDeliveryInfoRepository()
    private val disputeRepository = DisputeRepository()

    var orders = mutableStateOf<List<Order>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    var selectedOrder = mutableStateOf<Order?>(null)
    var isLoadingOrder = mutableStateOf(false)

    // AdminOrdersScreen's batched per-order delivery info, keyed by orderId
    // — loaded once alongside loadAllOrders() rather than per-card, so a
    // list of many orders doesn't fire one query per order.
    var deliveryInfoByOrderId = mutableStateOf<Map<String, OrderDeliveryInfo>>(emptyMap())

    // Same batching pattern, for the dispute banner/resolve buttons on
    // AdminOrderCard and for gating the DELIVERED -> PAID_TO_SELLER advance
    // button via disputeBlocksPayout() + nextOrderStatus()'s
    // disputeBlocksPayout parameter.
    var disputeByOrderId = mutableStateOf<Map<String, Dispute>>(emptyMap())

    // OrderDetailScreen's single-order delivery info (the buyer's own order).
    var selectedOrderDeliveryInfo = mutableStateOf<OrderDeliveryInfo?>(null)

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

    // Alongside loadOrder() for OrderDetailScreen — a separate call since
    // it's a different collection, but always paired with it in practice.
    fun loadOrderDeliveryInfo(orderId: String) {
        viewModelScope.launch {
            deliveryInfoRepository.getDeliveryInfo(orderId)
                .onSuccess { selectedOrderDeliveryInfo.value = it }
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

    // The seller's own "My Sales" view — orders placed against their
    // listings. Deliberately does not load delivery info at all; sellers
    // never get access to that collection (see firestore.rules).
    fun loadOrdersForSeller() {
        val sellerId = authRepository.currentUserId ?: return
        isLoading.value = true
        viewModelScope.launch {
            val result = repository.getOrdersForSeller(sellerId)
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
            result.onSuccess { loadedOrders ->
                orders.value = loadedOrders
                loadDeliveryInfoForOrders(loadedOrders.map { it.id })
                loadDisputesForOrders(loadedOrders.map { it.id })
            }
            result.onFailure { errorMessage.value = it.message }
        }
    }

    private fun loadDeliveryInfoForOrders(orderIds: List<String>) {
        viewModelScope.launch {
            deliveryInfoRepository.getDeliveryInfoForOrders(orderIds)
                .onSuccess { deliveryInfoByOrderId.value = it }
        }
    }

    private fun loadDisputesForOrders(orderIds: List<String>) {
        viewModelScope.launch {
            disputeRepository.getDisputesForOrders(orderIds)
                .onSuccess { disputeByOrderId.value = it }
        }
    }

    // Moves an order to the next step in the fulfillment pipeline. The
    // admin UI already hides this button when nextOrderStatus() would
    // return null, but the check is repeated here too, same defense-in-depth
    // as everywhere else in this app. firestore.rules is what actually
    // enforces the dispute block server-side — this is just so the button
    // doesn't even try.
    fun advanceStatus(order: Order) {
        val blocksPayout = disputeBlocksPayout(disputeByOrderId.value[order.id]?.status)
        val next = nextOrderStatus(order.status, order.paymentStatus, order.deliveryMethod, blocksPayout) ?: return
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, next)
            loadAllOrders()
        }
    }

    // Admin resolving a dispute — lifts the DELIVERED -> PAID_TO_SELLER
    // block (see disputeBlocksPayout above) unless resolved RESOLVED_BUYER,
    // which blocks it permanently, without touching the order itself; admin
    // still has to press "Advance to Next Stage" separately (when that's
    // even possible), same manual-nothing-automated approach as payment
    // verification.
    fun resolveDispute(order: Order, newStatus: String) {
        viewModelScope.launch {
            disputeRepository.resolveDispute(order.id, newStatus)
                .onSuccess {
                    notificationRepository.createDisputeResolvedNotification(order, newStatus)
                }
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
                    // to relay it to the seller. Delivery info now lives in
                    // its own document (admin-readable), fetched here rather
                    // than read off order directly.
                    if (order.deliveryMethod == "SELLER_DELIVERS") {
                        val deliveryInfo = deliveryInfoRepository.getDeliveryInfo(order.id).getOrNull()
                        notificationRepository.createNotification(
                            order = order,
                            deliveryAddress = deliveryInfo?.deliveryAddress ?: "",
                            deliveryContactPhone = deliveryInfo?.deliveryContactPhone ?: ""
                        )
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

// Single source of truth for which dispute outcomes block payout, mirrored
// exactly by firestore.rules' disputeBlocksPayout() — keep the two in sync
// if this ever changes. OPEN blocks pending resolution; RESOLVED_BUYER
// blocks permanently, since that outcome means this order should never pay
// out. RESOLVED_SELLER and DISMISSED (and no dispute at all) don't block.
fun disputeBlocksPayout(disputeStatus: String?): Boolean =
    disputeStatus == "OPEN" || disputeStatus == "RESOLVED_BUYER"

// Pure decision function for the fulfillment pipeline, kept free of any
// Firebase/ViewModel dependency so it's directly unit-testable. Returns null
// when there's no next step (terminal status) or the step is blocked
// (PLACED -> PICKED_UP requires a verified payment; PICKED_UP -> DELIVERED
// is blocked for the admin/advance-button path when the buyer chose
// SELLER_DELIVERS, since that transition can only come from the buyer's own
// confirmDelivery() action instead; DELIVERED -> PAID_TO_SELLER is blocked
// while disputeBlocksPayout, mirroring the same guard firestore.rules
// enforces server-side).
fun nextOrderStatus(
    currentStatus: String,
    paymentStatus: String,
    deliveryMethod: String,
    disputeBlocksPayout: Boolean = false
): String? {
    return when (currentStatus) {
        "PLACED" -> if (paymentStatus == "VERIFIED") "PICKED_UP" else null
        "PICKED_UP" -> if (deliveryMethod == "DENTALMARKET_DELIVERS") "DELIVERED" else null
        "DELIVERED" -> if (!disputeBlocksPayout) "PAID_TO_SELLER" else null
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
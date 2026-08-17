package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.CommissionConfigRepository
import com.dentalmarket.app.data.DeliveryConfigRepository
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.data.OrderRepository
import com.dentalmarket.app.data.SafetyFeeConfigRepository
import com.dentalmarket.app.model.CartItem
import com.dentalmarket.app.model.Listing
import com.dentalmarket.app.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    private val orderRepository = OrderRepository()
    private val listingRepository = ListingRepository()
    private val authRepository = AuthRepository()
    private val commissionConfigRepository = CommissionConfigRepository()
    private val deliveryConfigRepository = DeliveryConfigRepository()
    private val safetyFeeConfigRepository = SafetyFeeConfigRepository()

    var isPlacingOrder = mutableStateOf(false)
    var orderPlacedSuccess = mutableStateOf(false)
    var orderErrorMessage = mutableStateOf<String?>(null)

    // Shown instead of placing the order when checkout() finds the account
    // isn't verified — see the fresh check inside checkout() below.
    var showVerifyEmailPrompt = mutableStateOf(false)
    var isResendingVerification = mutableStateOf(false)
    var resendVerificationSuccess = mutableStateOf(false)
    var resendVerificationError = mutableStateOf<String?>(null)

    fun resendVerificationEmail() {
        viewModelScope.launch {
            isResendingVerification.value = true
            resendVerificationError.value = null
            authRepository.resendVerificationEmail()
                .onSuccess { resendVerificationSuccess.value = true }
                .onFailure { resendVerificationError.value = it.message ?: "Failed to resend verification email" }
            isResendingVerification.value = false
        }
    }

    fun dismissVerifyEmailPrompt() {
        showVerifyEmailPrompt.value = false
        resendVerificationSuccess.value = false
        resendVerificationError.value = null
    }

    // The orders just created by checkout(), each with its real Firestore
    // id — CartScreen uses this to queue up a payment reference prompt per
    // order immediately, instead of the buyer having to find each one
    // separately in My Orders.
    var createdOrders = mutableStateOf<List<Order>>(emptyList())

    // Live current flat fee, fetched once when the cart screen opens —
    // purely so each CartItemRow's checkbox label can show a real
    // "Add buyer safety fee (+$X)" number. The value actually locked onto
    // each Order is resolved fresh inside checkout() below, not read from
    // this cached copy.
    var safetyFeeAmount = mutableStateOf<Double?>(null)

    fun loadSafetyFeeConfig() {
        viewModelScope.launch {
            safetyFeeConfigRepository.getSafetyFeeConfig()
                .onSuccess { safetyFeeAmount.value = it.feeAmount }
        }
    }

    // Pre-filled from the buyer's saved profile default, editable in the
    // cart, and written back to that same default at checkout — see
    // checkout() below.
    var deliveryAddress = mutableStateOf("")
    var deliveryContactPhone = mutableStateOf("")

    fun loadBuyerDeliveryInfo() {
        viewModelScope.launch {
            authRepository.getCurrentUserProfile()
                .onSuccess { profile ->
                    deliveryAddress.value = profile?.deliveryAddress ?: ""
                    deliveryContactPhone.value = profile?.deliveryContactPhone?.takeIf { it.isNotBlank() }
                        ?: profile?.mobile ?: ""
                }
        }
    }

    fun updateDeliveryAddress(value: String) {
        deliveryAddress.value = value
    }

    fun updateDeliveryContactPhone(value: String) {
        deliveryContactPhone.value = value
    }

    fun addToCart(listing: Listing, quantity: Int = 1) {
        _cartItems.update { current ->
            val existing = current.find { it.listing.id == listing.id }
            if (existing != null) {
                current.map {
                    if (it.listing.id == listing.id) it.copy(quantity = it.quantity + quantity) else it
                }
            } else {
                current + CartItem(listing, quantity)
            }
        }
    }

    fun updateQuantity(listingId: String, quantity: Int) {
        _cartItems.update { current ->
            if (quantity <= 0) {
                current.filterNot { it.listing.id == listingId }
            } else {
                current.map {
                    if (it.listing.id == listingId) it.copy(quantity = quantity) else it
                }
            }
        }
    }

    fun removeFromCart(listingId: String) {
        _cartItems.update { current -> current.filterNot { it.listing.id == listingId } }
    }

    fun toggleSafetyFee(listingId: String) {
        _cartItems.update { current ->
            current.map {
                if (it.listing.id == listingId) it.copy(safetyFeeSelected = !it.safetyFeeSelected) else it
            }
        }
    }

    // CartViewModel outlives any single signed-in identity (it's created once
    // for the app's whole session), so whoever calls sign-out/sign-in/guest
    // upgrade must call this too — otherwise one account's cart carries over
    // into the next session that reuses this same ViewModel instance.
    // createdOrders has the same leak risk (same session-long-lived
    // instance), so it's cleared here too.
    fun clearCart() {
        _cartItems.value = emptyList()
        clearCreatedOrders()
    }

    // Called once CartScreen has walked through the whole payment-reference
    // queue for a completed checkout, so that list can't leak into the next
    // time CartScreen is entered and immediately (and wrongly) pop up a
    // payment dialog for an already-finished checkout.
    fun clearCreatedOrders() {
        createdOrders.value = emptyList()
    }

    // Places one Order per cart item (cash-on-delivery), marks each listing
    // as sold so it disappears from the marketplace, then empties the cart.
    // Delivery method is the seller's own choice, set on the listing at
    // posting time — read per item here, not decided by the buyer at
    // checkout. deliveryFee is still resolved and locked onto the Order the
    // same way, but it's now deducted from the seller's payout rather than
    // charged to the buyer — see calculatePayout(). safetyFee is a flat,
    // per-item opt-in (item.safetyFeeSelected), not multiplied by quantity.
    // deliveryAddress/deliveryContactPhone are a single value for the whole
    // batch, stamped onto every Order, and also saved back to the buyer's
    // profile so this checkout's edits become next time's pre-filled default.
    fun checkout() {
        val buyerId = authRepository.currentUserId
        if (buyerId == null) {
            orderErrorMessage.value = "You must be logged in."
            return
        }
        val items = _cartItems.value
        if (items.isEmpty()) return
        if (deliveryAddress.value.isBlank()) {
            orderErrorMessage.value = "Please enter a delivery address."
            return
        }

        isPlacingOrder.value = true
        orderErrorMessage.value = null

        viewModelScope.launch {
            val verifiedResult = authRepository.isEmailVerifiedFresh()
            if (verifiedResult.getOrNull() != true) {
                isPlacingOrder.value = false
                if (verifiedResult.isFailure) {
                    orderErrorMessage.value = "Couldn't verify your account — check your connection and try again."
                } else {
                    showVerifyEmailPrompt.value = true
                }
                return@launch
            }

            val profileResult = authRepository.getCurrentUserProfile()
            val buyerName = profileResult.getOrNull()?.name ?: "Unknown Buyer"
            val commissionPercentage = commissionConfigRepository.getCommissionConfig()
                .getOrNull()?.percentage ?: 0.0
            val configuredDeliveryFee = deliveryConfigRepository.getDeliveryConfig()
                .getOrNull()?.feeAmount ?: 0.0
            val configuredSafetyFee = safetyFeeConfigRepository.getSafetyFeeConfig()
                .getOrNull()?.feeAmount ?: 0.0
            val address = deliveryAddress.value
            val contactPhone = deliveryContactPhone.value

            var allSucceeded = true
            val placedOrders = mutableListOf<Order>()
            for (item in items) {
                val deliveryFee = if (item.listing.deliveryMethod == "DENTALMARKET_DELIVERS") {
                    configuredDeliveryFee
                } else {
                    0.0
                }
                val order = Order(
                    listingId = item.listing.id,
                    listingName = item.listing.name,
                    listingEmoji = item.listing.emoji,
                    price = item.listing.price,
                    quantity = item.quantity,
                    buyerId = buyerId,
                    buyerName = buyerName,
                    sellerId = item.listing.sellerId,
                    sellerName = item.listing.sellerName,
                    commissionPercentage = commissionPercentage,
                    deliveryMethod = item.listing.deliveryMethod,
                    deliveryFee = deliveryFee,
                    safetyFee = if (item.safetyFeeSelected) configuredSafetyFee else 0.0
                )
                val orderResult = orderRepository.placeOrder(order, address, contactPhone)
                val placedOrder = orderResult.getOrNull()
                if (placedOrder == null) {
                    allSucceeded = false
                    continue
                }
                placedOrders.add(placedOrder)
                listingRepository.markAsSold(item.listing.id)
            }

            if (allSucceeded) {
                authRepository.updateDeliveryInfo(address, contactPhone)
            }

            isPlacingOrder.value = false
            if (allSucceeded) {
                _cartItems.value = emptyList()
                createdOrders.value = placedOrders
                orderPlacedSuccess.value = true
            } else {
                orderErrorMessage.value = "Some items could not be ordered. Please try again."
            }
        }
    }
}
package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.CommissionConfigRepository
import com.dentalmarket.app.data.DeliveryConfigRepository
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.data.OrderRepository
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

    var isPlacingOrder = mutableStateOf(false)
    var orderPlacedSuccess = mutableStateOf(false)
    var orderErrorMessage = mutableStateOf<String?>(null)

    // Live current fee, fetched once when the cart screen opens — purely so
    // the "DentalMarket delivers (+$X)" choice label shows a real number.
    // The value actually locked onto each Order is resolved fresh inside
    // checkout() below, not read from this cached copy.
    var deliveryFeeAmount = mutableStateOf<Double?>(null)

    fun loadDeliveryConfig() {
        viewModelScope.launch {
            deliveryConfigRepository.getDeliveryConfig()
                .onSuccess { deliveryFeeAmount.value = it.feeAmount }
        }
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

    // CartViewModel outlives any single signed-in identity (it's created once
    // for the app's whole session), so whoever calls sign-out/sign-in/guest
    // upgrade must call this too — otherwise one account's cart carries over
    // into the next session that reuses this same ViewModel instance.
    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // Places one Order per cart item (cash-on-delivery), marks each listing
    // as sold so it disappears from the marketplace, then empties the cart.
    // Delivery method is the seller's own choice, set on the listing at
    // posting time — read per item here, not decided by the buyer at
    // checkout. The fee amount itself is still the single platform-wide
    // config/delivery figure, resolved once per checkout and applied to
    // whichever items need it.
    fun checkout() {
        val buyerId = authRepository.currentUserId
        if (buyerId == null) {
            orderErrorMessage.value = "You must be logged in."
            return
        }
        val items = _cartItems.value
        if (items.isEmpty()) return

        isPlacingOrder.value = true
        orderErrorMessage.value = null

        viewModelScope.launch {
            val profileResult = authRepository.getCurrentUserProfile()
            val buyerName = profileResult.getOrNull()?.name ?: "Unknown Buyer"
            val commissionPercentage = commissionConfigRepository.getCommissionConfig()
                .getOrNull()?.percentage ?: 0.0
            val configuredDeliveryFee = deliveryConfigRepository.getDeliveryConfig()
                .getOrNull()?.feeAmount ?: 0.0

            var allSucceeded = true
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
                    deliveryFee = deliveryFee
                )
                val orderResult = orderRepository.placeOrder(order)
                if (orderResult.isFailure) {
                    allSucceeded = false
                    continue
                }
                listingRepository.markAsSold(item.listing.id)
            }

            isPlacingOrder.value = false
            if (allSucceeded) {
                _cartItems.value = emptyList()
                orderPlacedSuccess.value = true
            } else {
                orderErrorMessage.value = "Some items could not be ordered. Please try again."
            }
        }
    }
}
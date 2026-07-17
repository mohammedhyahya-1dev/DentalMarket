package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
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

    var isPlacingOrder = mutableStateOf(false)
    var orderPlacedSuccess = mutableStateOf(false)
    var orderErrorMessage = mutableStateOf<String?>(null)

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

    // Places one Order per cart item (cash-on-delivery), marks each listing
    // as sold so it disappears from the marketplace, then empties the cart.
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

            var allSucceeded = true
            for (item in items) {
                val order = Order(
                    listingId = item.listing.id,
                    listingName = item.listing.name,
                    listingEmoji = item.listing.emoji,
                    price = item.listing.price,
                    quantity = item.quantity,
                    buyerId = buyerId,
                    buyerName = buyerName,
                    sellerId = item.listing.sellerId,
                    sellerName = item.listing.sellerName
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
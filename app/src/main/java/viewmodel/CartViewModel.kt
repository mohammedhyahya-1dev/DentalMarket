package com.dentalmarket.app.viewmodel

import androidx.lifecycle.ViewModel
import com.dentalmarket.app.model.CartItem
import com.dentalmarket.app.model.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class CartViewModel : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

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
}
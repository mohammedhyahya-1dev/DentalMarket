package com.dentalmarket.app.viewmodel

import androidx.lifecycle.ViewModel
import com.dentalmarket.app.model.CartItem
import com.dentalmarket.app.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// Holds the cart in memory for now. In a later phase this could sync to a
// server, but for testing the flow, in-memory state is all we need.
class CartViewModel : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    fun addToCart(product: Product, quantity: Int = 1) {
        _cartItems.update { current ->
            val existing = current.find { it.product.id == product.id }
            if (existing != null) {
                current.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + quantity) else it
                }
            } else {
                current + CartItem(product, quantity)
            }
        }
    }

    fun updateQuantity(productId: Int, quantity: Int) {
        _cartItems.update { current ->
            if (quantity <= 0) {
                current.filterNot { it.product.id == productId }
            } else {
                current.map {
                    if (it.product.id == productId) it.copy(quantity = quantity) else it
                }
            }
        }
    }

    fun removeFromCart(productId: Int) {
        _cartItems.update { current -> current.filterNot { it.product.id == productId } }
    }
}

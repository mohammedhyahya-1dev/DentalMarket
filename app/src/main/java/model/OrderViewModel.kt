package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.OrderRepository
import com.dentalmarket.app.model.Order
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    private val repository = OrderRepository()
    private val authRepository = AuthRepository()

    var orders = mutableStateOf<List<Order>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

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

    // Moves an order to the next step in the fulfillment pipeline.
    fun advanceStatus(order: Order) {
        val next = when (order.status) {
            "PLACED" -> "PICKED_UP"
            "PICKED_UP" -> "DELIVERED"
            "DELIVERED" -> "PAID_TO_SELLER"
            else -> return
        }
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, next)
            loadAllOrders()
        }
    }
}
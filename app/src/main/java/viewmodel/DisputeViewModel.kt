package com.dentalmarket.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.DisputeRepository
import com.dentalmarket.app.data.SellerNotificationRepository
import com.dentalmarket.app.model.Dispute
import com.dentalmarket.app.model.DisputeReason
import com.dentalmarket.app.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DisputeViewModel : ViewModel() {
    private val repository = DisputeRepository()
    private val notificationRepository = SellerNotificationRepository()

    private val _dispute = MutableStateFlow<Dispute?>(null)
    val dispute: StateFlow<Dispute?> = _dispute

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadDispute(orderId: String) {
        viewModelScope.launch {
            repository.getDispute(orderId).onSuccess { _dispute.value = it }
        }
    }

    fun fileDispute(order: Order, reason: DisputeReason, details: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            val dispute = Dispute(
                orderId = order.id,
                listingId = order.listingId,
                listingName = order.listingName,
                buyerId = order.buyerId,
                sellerId = order.sellerId,
                reason = reason.name,
                details = details
            )
            repository.fileDispute(dispute)
                .onSuccess {
                    _dispute.value = dispute
                    notificationRepository.createDisputeOpenedNotification(order)
                    onSuccess()
                }
                .onFailure { _errorMessage.value = it.message ?: "Failed to file dispute" }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

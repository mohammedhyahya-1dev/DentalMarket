package com.dentalmarket.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.RatingRepository
import com.dentalmarket.app.model.Rating
import com.dentalmarket.app.model.SellerRatingSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RatingViewModel : ViewModel() {

    private val repository = RatingRepository()
    private val authRepository = AuthRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _ratedOrderIds = MutableStateFlow<Set<String>>(emptySet())
    val ratedOrderIds: StateFlow<Set<String>> = _ratedOrderIds

    private val _sellerSummaries = MutableStateFlow<Map<String, SellerRatingSummary>>(emptyMap())
    val sellerSummaries: StateFlow<Map<String, SellerRatingSummary>> = _sellerSummaries

    fun submitRating(rating: Rating, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.submitRating(rating)
                .onSuccess {
                    _ratedOrderIds.update { it + rating.orderId }
                    loadSellerSummaries(listOf(rating.sellerId))
                    onSuccess()
                }
                .onFailure { _errorMessage.value = it.message ?: "Failed to submit rating" }
            _isLoading.value = false
        }
    }

    fun loadMyRatedOrders() {
        val buyerId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            repository.getRatedOrderIdsForBuyer(buyerId)
                .onSuccess { _ratedOrderIds.value = it }
                .onFailure { _errorMessage.value = it.message ?: "Failed to load ratings" }
        }
    }

    fun loadSellerSummaries(sellerIds: List<String>) {
        val idsToFetch = sellerIds.distinct().filter { it.isNotBlank() }
        if (idsToFetch.isEmpty()) return
        viewModelScope.launch {
            repository.getRatingSummariesForSellers(idsToFetch)
                .onSuccess { fetched -> _sellerSummaries.update { it + fetched } }
        }
    }
}

package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.CommissionConfigRepository
import com.dentalmarket.app.data.DeliveryConfigRepository
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.model.Listing
import kotlinx.coroutines.launch

class MyListingsViewModel : ViewModel() {
    private val repository = ListingRepository()
    private val authRepository = AuthRepository()
    private val commissionConfigRepository = CommissionConfigRepository()
    private val deliveryConfigRepository = DeliveryConfigRepository()

    var listings = mutableStateOf<List<Listing>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    // Mirrors OrderViewModel's selectedOrder/loadOrder pattern, for the
    // listing detail screen reached by tapping a card in My Listings.
    var selectedListing = mutableStateOf<Listing?>(null)
    var isLoadingListing = mutableStateOf(false)

    // Live current rate for the detail screen's pre-sale commission estimate
    // — same non-blocking, best-effort fetch as ListingViewModel's Sell screen.
    var commissionPercentage = mutableStateOf<Double?>(null)

    fun loadMyListings() {
        val sellerId = authRepository.currentUserId ?: return
        isLoading.value = true
        viewModelScope.launch {
            val result = repository.getListingsBySeller(sellerId)
            isLoading.value = false
            result.onSuccess { listings.value = it }
            result.onFailure { errorMessage.value = it.message }
        }
    }

    fun loadListing(listingId: String) {
        isLoadingListing.value = true
        viewModelScope.launch {
            val result = repository.getListingById(listingId)
            isLoadingListing.value = false
            result.onSuccess { selectedListing.value = it }
        }
    }

    fun loadCommissionConfig() {
        viewModelScope.launch {
            commissionConfigRepository.getCommissionConfig()
                .onSuccess { commissionPercentage.value = it.percentage }
        }
    }

    // Live current fee, for showing a real number next to a listing's
    // DentalMarket-delivers choice on both the card list and the detail
    // screen. The listing itself only stores the seller's yes/no.
    var deliveryFeeAmount = mutableStateOf<Double?>(null)

    fun loadDeliveryConfig() {
        viewModelScope.launch {
            deliveryConfigRepository.getDeliveryConfig()
                .onSuccess { deliveryFeeAmount.value = it.feeAmount }
        }
    }

    fun deleteListing(listingId: String) {
        viewModelScope.launch {
            repository.deleteListing(listingId)
            loadMyListings()
        }
    }
}
package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.model.Listing
import kotlinx.coroutines.launch

class MyListingsViewModel : ViewModel() {
    private val repository = ListingRepository()
    private val authRepository = AuthRepository()

    var listings = mutableStateOf<List<Listing>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

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

    fun deleteListing(listingId: String) {
        viewModelScope.launch {
            repository.deleteListing(listingId)
            loadMyListings()
        }
    }
}
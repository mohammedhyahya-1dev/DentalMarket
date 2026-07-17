package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.model.Listing
import kotlinx.coroutines.launch

class MarketplaceViewModel : ViewModel() {
    private val repository = ListingRepository()

    var listings = mutableStateOf<List<Listing>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    init {
        loadListings()
    }

    fun loadListings() {
        isLoading.value = true
        viewModelScope.launch {
            val result = repository.getAllListings()
            isLoading.value = false
            result.onSuccess { listings.value = it }
            result.onFailure { errorMessage.value = it.message }
        }
    }
}
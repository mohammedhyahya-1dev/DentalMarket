package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.model.Listing
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ListingViewModel : ViewModel() {
    private val repository = ListingRepository()
    private val authRepository = AuthRepository()

    var name = mutableStateOf("")
    var category = mutableStateOf("")
    var condition = mutableStateOf("GOOD")
    var price = mutableStateOf("")
    var description = mutableStateOf("")
    var emoji = mutableStateOf("🦷")

    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)
    var postSuccess = mutableStateOf(false)

    fun postListing() {
        val sellerId = FirebaseAuth.getInstance().currentUser?.uid
        if (sellerId == null) {
            errorMessage.value = "You must be logged in."
            return
        }
        val priceValue = price.value.toDoubleOrNull()
        if (name.value.isBlank() || priceValue == null) {
            errorMessage.value = "Please fill in name and a valid price."
            return
        }

        isLoading.value = true
        errorMessage.value = null

        viewModelScope.launch {
            val profileResult = authRepository.getCurrentUserProfile()
            val sellerName = profileResult.getOrNull()?.name ?: "Unknown Seller"

            val listing = Listing(
                sellerId = sellerId,
                sellerName = sellerName,
                name = name.value,
                category = category.value,
                condition = condition.value,
                price = priceValue,
                description = description.value,
                emoji = emoji.value
            )
            val result = repository.addListing(listing)
            isLoading.value = false
            result.onSuccess { postSuccess.value = true }
            result.onFailure { errorMessage.value = it.message }
        }
    }
}
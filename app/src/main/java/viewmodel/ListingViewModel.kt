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

    // When editing, these hold the parts of the original listing the form
    // doesn't show, so saving changes doesn't wipe them out.
    private var editingListingId: String? = null
    private var editingSellerId: String = ""
    private var editingSellerName: String = ""
    private var editingStatus: String = "AVAILABLE"

    fun loadListingForEdit(listingId: String) {
        editingListingId = listingId
        isLoading.value = true
        viewModelScope.launch {
            val result = repository.getListingById(listingId)
            isLoading.value = false
            result.onSuccess { listing ->
                if (listing != null) {
                    name.value = listing.name
                    category.value = listing.category
                    condition.value = listing.condition
                    price.value = listing.price.toString()
                    description.value = listing.description
                    emoji.value = listing.emoji
                    editingSellerId = listing.sellerId
                    editingSellerName = listing.sellerName
                    editingStatus = listing.status
                }
            }
            result.onFailure { errorMessage.value = it.message }
        }
    }

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
        val editingId = editingListingId

        viewModelScope.launch {
            if (editingId != null) {
                val listing = Listing(
                    id = editingId,
                    sellerId = editingSellerId,
                    sellerName = editingSellerName,
                    name = name.value,
                    category = category.value,
                    condition = condition.value,
                    price = priceValue,
                    description = description.value,
                    emoji = emoji.value,
                    status = editingStatus
                )
                val result = repository.updateListing(editingId, listing)
                isLoading.value = false
                result.onSuccess { postSuccess.value = true }
                result.onFailure { errorMessage.value = it.message }
            } else {
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
}
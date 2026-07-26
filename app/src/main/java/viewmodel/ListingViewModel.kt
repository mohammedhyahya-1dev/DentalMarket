package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.model.Listing
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

data class SpecRow(
    var key: androidx.compose.runtime.MutableState<String>,
    var value: androidx.compose.runtime.MutableState<String>
)

class ListingViewModel : ViewModel() {
    private val repository = ListingRepository()
    private val authRepository = AuthRepository()

    var name = mutableStateOf("")
    var category = mutableStateOf("")
    var condition = mutableStateOf("GOOD")
    var price = mutableStateOf("")
    var description = mutableStateOf("")
    var emoji = mutableStateOf("🦷")

    // Dynamic key/value spec rows the seller builds (e.g. "Brand" -> "Sirona").
    // Kept as a mutable list of individually-observable rows so typing in one
    // field doesn't force the whole list to redraw.
    var specifics = mutableStateListOf<SpecRow>()

    fun addSpecRow() {
        specifics.add(SpecRow(mutableStateOf(""), mutableStateOf("")))
    }

    fun removeSpecRow(row: SpecRow) {
        specifics.remove(row)
    }

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
                    specifics.clear()
                    listing.specifics.forEach { (k, v) ->
                        specifics.add(SpecRow(mutableStateOf(k), mutableStateOf(v)))
                    }
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
                    status = editingStatus,
                    specifics = specifics.associate { it.key.value.trim() to it.value.value.trim() }.filterKeys { it.isNotBlank() }
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
                    emoji = emoji.value,
                    specifics = specifics.associate { it.key.value.trim() to it.value.value.trim() }.filterKeys { it.isNotBlank() }
                )
                val result = repository.addListing(listing)
                isLoading.value = false
                result.onSuccess { postSuccess.value = true }
                result.onFailure { errorMessage.value = it.message }
            }
        }
    }
}
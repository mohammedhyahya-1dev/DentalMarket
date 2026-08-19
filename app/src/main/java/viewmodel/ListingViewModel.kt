package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.CommissionConfigRepository
import com.dentalmarket.app.data.DeliveryConfigRepository
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
    private val commissionConfigRepository = CommissionConfigRepository()
    private val deliveryConfigRepository = DeliveryConfigRepository()

    // Live current rate, fetched once when the screen opens — purely for the
    // pre-sale "you'll receive approximately..." estimate below the price
    // field. Unrelated to Order.commissionPercentage, which locks in the
    // rate at checkout time instead.
    var commissionPercentage = mutableStateOf<Double?>(null)

    fun loadCommissionConfig() {
        viewModelScope.launch {
            commissionConfigRepository.getCommissionConfig()
                .onSuccess { commissionPercentage.value = it.percentage }
        }
    }

    // Live current fee, fetched once when the screen opens — purely so the
    // "DentalMarket delivers" choice label shows a real number. The listing
    // itself only stores the yes/no (deliveryMethod below), not this amount.
    var deliveryFeeAmount = mutableStateOf<Double?>(null)

    fun loadDeliveryConfig() {
        viewModelScope.launch {
            deliveryConfigRepository.getDeliveryConfig()
                .onSuccess { deliveryFeeAmount.value = it.feeAmount }
        }
    }

    var name = mutableStateOf("")
    var category = mutableStateOf("")
    var subcategory = mutableStateOf("")
    var condition = mutableStateOf("GOOD")
    var price = mutableStateOf("")
    var description = mutableStateOf("")
    var emoji = mutableStateOf("🦷")
    var deliveryMethod = mutableStateOf("SELLER_DELIVERS")
    var shippingPrice = mutableStateOf("")
    var brand = mutableStateOf("")

    // Dynamic key/value spec rows the seller builds (e.g. "Brand" -> "Sirona").
    // Kept as a mutable list of individually-observable rows so typing in one
    // field doesn't force the whole list to redraw.
    var specifics = mutableStateListOf<SpecRow>()

    // Picking a new top-level category invalidates whatever subcategory was
    // chosen under the previous one (each category has its own distinct
    // subcategory list) — always route category changes through here rather
    // than setting category.value directly.
    fun setCategory(newLabel: String) {
        category.value = newLabel
        subcategory.value = ""
    }

    fun addSpecRow() {
        specifics.add(SpecRow(mutableStateOf(""), mutableStateOf("")))
    }

    fun removeSpecRow(row: SpecRow) {
        specifics.remove(row)
    }

    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)
    var postSuccess = mutableStateOf(false)

    // Shown instead of posting/saving when postListing() finds the account
    // isn't verified — see the fresh check inside postListing() below.
    var showVerifyEmailPrompt = mutableStateOf(false)
    var isResendingVerification = mutableStateOf(false)
    var resendVerificationSuccess = mutableStateOf(false)
    var resendVerificationError = mutableStateOf<String?>(null)

    fun resendVerificationEmail() {
        viewModelScope.launch {
            isResendingVerification.value = true
            resendVerificationError.value = null
            authRepository.resendVerificationEmail()
                .onSuccess { resendVerificationSuccess.value = true }
                .onFailure { resendVerificationError.value = it.message ?: "Failed to resend verification email" }
            isResendingVerification.value = false
        }
    }

    fun dismissVerifyEmailPrompt() {
        showVerifyEmailPrompt.value = false
        resendVerificationSuccess.value = false
        resendVerificationError.value = null
    }

    // When editing, these hold the parts of the original listing the form
    // doesn't show, so saving changes doesn't wipe them out.
    private var editingListingId: String? = null
    private var editingSellerId: String = ""
    private var editingSellerName: String = ""
    private var editingSellerProvince: String = ""
    private var editingStatus: String = "AVAILABLE"
    private var editingCreatedAt: Long = 0

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
                    subcategory.value = listing.subcategory
                    condition.value = listing.condition
                    price.value = listing.price.toString()
                    description.value = listing.description
                    emoji.value = listing.emoji
                    deliveryMethod.value = listing.deliveryMethod
                    shippingPrice.value = if (listing.shippingPrice > 0) listing.shippingPrice.toString() else ""
                    brand.value = listing.brand
                    editingSellerId = listing.sellerId
                    editingSellerName = listing.sellerName
                    editingSellerProvince = listing.sellerProvince
                    editingStatus = listing.status
                    editingCreatedAt = listing.createdAt
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
            val verifiedResult = authRepository.isEmailVerifiedFresh()
            if (verifiedResult.getOrNull() != true) {
                isLoading.value = false
                if (verifiedResult.isFailure) {
                    errorMessage.value = "Couldn't verify your account — check your connection and try again."
                } else {
                    showVerifyEmailPrompt.value = true
                }
                return@launch
            }

            val shippingPriceValue = shippingPrice.value.toDoubleOrNull() ?: 0.0

            if (editingId != null) {
                val listing = Listing(
                    id = editingId,
                    sellerId = editingSellerId,
                    sellerName = editingSellerName,
                    name = name.value,
                    category = category.value,
                    subcategory = subcategory.value,
                    condition = condition.value,
                    price = priceValue,
                    description = description.value,
                    emoji = emoji.value,
                    status = editingStatus,
                    specifics = specifics.associate { it.key.value.trim() to it.value.value.trim() }.filterKeys { it.isNotBlank() },
                    deliveryMethod = deliveryMethod.value,
                    shippingPrice = shippingPriceValue,
                    brand = brand.value.trim(),
                    sellerProvince = editingSellerProvince,
                    createdAt = editingCreatedAt
                )
                val result = repository.updateListing(editingId, listing)
                isLoading.value = false
                result.onSuccess { postSuccess.value = true }
                result.onFailure { errorMessage.value = it.message }
            } else {
                val profileResult = authRepository.getCurrentUserProfile()
                val sellerName = profileResult.getOrNull()?.name ?: "Unknown Seller"
                val sellerProvince = profileResult.getOrNull()?.province ?: ""

                val listing = Listing(
                    sellerId = sellerId,
                    sellerName = sellerName,
                    name = name.value,
                    category = category.value,
                    subcategory = subcategory.value,
                    condition = condition.value,
                    price = priceValue,
                    description = description.value,
                    emoji = emoji.value,
                    specifics = specifics.associate { it.key.value.trim() to it.value.value.trim() }.filterKeys { it.isNotBlank() },
                    deliveryMethod = deliveryMethod.value,
                    shippingPrice = shippingPriceValue,
                    brand = brand.value.trim(),
                    sellerProvince = sellerProvince
                )
                val result = repository.addListing(listing)
                isLoading.value = false
                result.onSuccess { postSuccess.value = true }
                result.onFailure { errorMessage.value = it.message }
            }
        }
    }
}
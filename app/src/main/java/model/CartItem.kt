package com.dentalmarket.app.model

data class CartItem(
    val listing: Listing,
    val quantity: Int,
    // Buyer's own opt-in for this specific item, checked per row in the
    // cart — not a whole-cart toggle.
    val safetyFeeSelected: Boolean = false
)
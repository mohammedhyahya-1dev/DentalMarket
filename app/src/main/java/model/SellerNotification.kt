package com.dentalmarket.app.model

// Created once per order, only when deliveryMethod is SELLER_DELIVERS, the
// moment admin verifies that order's payment reference — see
// OrderViewModel.verifyPayment(). This is deliberately a lightweight
// stand-in for the still-open full seller-orders-view roadmap item, not
// that feature itself.
data class SellerNotification(
    val id: String = "",
    val recipientId: String = "",
    val orderId: String = "",
    val listingName: String = "",
    val buyerName: String = "",
    val deliveryAddress: String = "",
    val deliveryContactPhone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

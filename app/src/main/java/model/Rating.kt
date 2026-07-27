package com.dentalmarket.app.model

data class Rating(
    val id: String = "",
    val orderId: String = "",
    val listingId: String = "",
    val listingName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val stars: Int = 5,
    val review: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SellerRatingSummary(
    val average: Double = 0.0,
    val count: Int = 0
)

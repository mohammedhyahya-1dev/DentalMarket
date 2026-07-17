package com.dentalmarket.app.model

data class Order(
    val id: String = "",
    val listingId: String = "",
    val listingName: String = "",
    val listingEmoji: String = "🦷",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val status: String = "PLACED",
    val createdAt: Long = System.currentTimeMillis()
)

enum class OrderStatus(val label: String) {
    PLACED("Placed"),
    PICKED_UP("Picked Up from Seller"),
    DELIVERED("Delivered to Buyer"),
    PAID_TO_SELLER("Paid to Seller"),
    CANCELLED("Cancelled")
}
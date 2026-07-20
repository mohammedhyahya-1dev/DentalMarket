package com.dentalmarket.app.model

data class Inquiry(
    val id: String = "",
    val listingId: String = "",
    val listingName: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerName: String = "",
    val question: String = "",
    val answer: String = "",
    val status: String = "PENDING", // PENDING or ANSWERED
    val timestamp: Long = System.currentTimeMillis()
)
package com.dentalmarket.app.model

data class Listing(
    val id: String = "",
    val sellerId: String = "",
    val name: String = "",
    val category: String = "",
    val condition: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val emoji: String = "🦷"
)
package com.dentalmarket.app.model

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val condition: Condition,
    val price: Double,
    val description: String,
    val emoji: String
)

enum class Condition(val label: String) {
    LIKE_NEW("Like New"),
    GOOD("Good"),
    FAIR("Fair")
}

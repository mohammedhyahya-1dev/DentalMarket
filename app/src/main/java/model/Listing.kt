package com.dentalmarket.app.model

data class Listing(
    val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val name: String = "",
    val category: String = "",
    val condition: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val emoji: String = "🦷",
    val status: String = "AVAILABLE",
    val specifics: Map<String, String> = emptyMap(),
    // Seller's own choice of who delivers — "SELLER_DELIVERS" or
    // "DENTALMARKET_DELIVERS". The fee amount itself stays a single
    // platform-wide knob (config/delivery), not stored here; this field is
    // only the seller's yes/no. Read at checkout time and locked onto each
    // Order independently, same as everything else in that pipeline.
    val deliveryMethod: String = "SELLER_DELIVERS",
    // Denormalized watchlist count, nudged by ±1 in WatchlistRepository
    // whenever a buyer adds/removes this listing — not queried live from
    // the watchlist collection, since Firestore can't rules-gate a count()
    // aggregation query any differently than the equivalent full-document
    // list query, and watchlist docs are buyer-private (see
    // firestore.rules). This is the only way to show "N people watching"
    // without exposing who.
    val watchCount: Int = 0
)
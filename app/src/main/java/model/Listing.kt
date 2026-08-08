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
    // Page-view counter, bumped by +1 in ListingRepository every time
    // ProductDetailScreen opens for this listing — no dedup, the same
    // person reopening it counts again each time. Unrelated to the
    // watchlist (heart icon) now; that's tracked separately in the
    // watchlist collection.
    val viewCount: Int = 0
)
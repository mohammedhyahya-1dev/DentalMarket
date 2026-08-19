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
    // Only meaningful when deliveryMethod == SELLER_DELIVERS — the seller's
    // own flat shipping charge, 0.0 meaning free shipping. Buyer pays this
    // in full at checkout as its own line item (see CartViewModel.checkout,
    // Order.shippingFee) and the seller keeps all of it — unlike price, it's
    // never reduced by platform commission.
    val shippingPrice: Double = 0.0,
    // First-class field rather than a "specifics" freeform key, since brand
    // needs to be reliably filterable (see SearchViewModel.kt) — freeform
    // keys sellers type there have no guaranteed casing or spelling.
    val brand: String = "",
    // Denormalized copy of the seller's DentalUser.province at posting
    // time, same pattern as sellerName above — lets the search/filter
    // screen filter by seller location without a per-listing profile
    // lookup (which firestore.rules restricts to self/admin anyway). Like
    // sellerName, this is a snapshot: it does not update if the seller's
    // profile province changes after posting.
    val sellerProvince: String = "",
    // Set once at posting time, preserved as-is on every later edit — powers
    // the search screen's "Newest First" sort. Listings written before this
    // field existed will read back with this constructor default (i.e. the
    // time of that particular read, not of posting), so their relative
    // "newest" order isn't meaningful until they're next edited.
    val createdAt: Long = System.currentTimeMillis(),
    // Page-view counter, bumped by +1 in ListingRepository every time
    // ProductDetailScreen opens for this listing — no dedup, the same
    // person reopening it counts again each time. Unrelated to the
    // watchlist (heart icon) now; that's tracked separately in the
    // watchlist collection.
    val viewCount: Int = 0
)
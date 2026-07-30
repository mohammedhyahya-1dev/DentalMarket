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
    val createdAt: Long = System.currentTimeMillis(),
    // Payment verification is orthogonal to the fulfillment status above —
    // it tracks whether the buyer's external QiCard/ZainCash transfer has
    // been confirmed, not where the physical item is. Kept as its own field
    // rather than a pipeline stage so the manual verification step here can
    // later be replaced by an automatic gateway confirmation without
    // touching the fulfillment pipeline, tracker UI, or rating logic.
    val paymentStatus: String = "AWAITING_PAYMENT",
    val paymentReference: String = "",
    val paymentRejectionReason: String = "",
    val paymentSubmittedAt: Long = 0,
    val paymentVerifiedAt: Long = 0
)

enum class OrderStatus(val label: String) {
    PLACED("Placed"),
    PICKED_UP("Picked Up from Seller"),
    DELIVERED("Delivered to Buyer"),
    PAID_TO_SELLER("Paid to Seller"),
    CANCELLED("Cancelled")
}

enum class PaymentStatus(val label: String) {
    AWAITING_PAYMENT("Awaiting Payment"),
    PENDING_VERIFICATION("Verification Pending"),
    VERIFIED("Payment Verified"),
    REJECTED("Payment Rejected")
}
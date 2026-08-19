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
    val paymentVerifiedAt: Long = 0,
    // The platform commission rate in effect when this order was placed,
    // captured once at checkout so a later change to config/commission
    // never retroactively changes what an already-placed (or already paid
    // out) order shows was owed to the seller.
    val commissionPercentage: Double = 0.0,
    // "SELLER_DELIVERS" or "DENTALMARKET_DELIVERS", chosen by the buyer at
    // checkout. Determines who is allowed to confirm the PICKED_UP ->
    // DELIVERED step: the buyer themselves for SELLER_DELIVERS (nextOrderStatus
    // blocks the admin path for that transition), or admin-as-courier for
    // DENTALMARKET_DELIVERS, same as every other stage.
    val deliveryMethod: String = "SELLER_DELIVERS",
    // Locked in at checkout from config/delivery, same reasoning as
    // commissionPercentage above — 0 for SELLER_DELIVERS. Deducted from the
    // seller's payout (same treatment as commission), never charged to the
    // buyer.
    val deliveryFee: Double = 0.0,
    // Set when the buyer taps "I received this". Unused today beyond being
    // a record, but it's the hook a future auto-release-after-waiting-period
    // job would read from without another schema change.
    val deliveryConfirmedAt: Long = 0,
    // Locked in at checkout from config/safetyFee, same reasoning as
    // commissionPercentage/deliveryFee above — a later change to the
    // configured fee shouldn't retroactively change what an already-placed
    // order shows the buyer was charged. Flat amount, only present if the
    // buyer opted in for this specific item at checkout (not multiplied by
    // quantity). Paid by the buyer, never reaches the seller.
    val safetyFee: Double = 0.0,
    // Locked in at checkout from item.listing.shippingPrice, only when
    // deliveryMethod == SELLER_DELIVERS (0 for DENTALMARKET_DELIVERS —
    // that's already free to the buyer via deliveryFee above). Flat per
    // order, not multiplied by quantity, same treatment as safetyFee. Paid
    // by the buyer, but unlike price, kept 100% by the seller — never
    // reduced by commissionPercentage.
    val shippingFee: Double = 0.0
    // Delivery address/contact phone deliberately live in a separate
    // orderDeliveryInfo/{orderId} document (see model/OrderDeliveryInfo.kt),
    // not here — sellers get read access to their own orders
    // (getOrdersForSeller), and Firestore has no way to grant access to a
    // document while hiding specific fields from it. Splitting them out is
    // what makes "buyer/admin only" a rule-enforced guarantee. Written
    // atomically alongside this Order in OrderRepository.placeOrder().
)

enum class OrderStatus(val label: String) {
    PLACED("Placed"),
    PICKED_UP("Picked Up from Seller"),
    DELIVERED("Delivered to Buyer"),
    PAID_TO_SELLER("Paid to Seller"),
    CANCELLED("Cancelled")
}

enum class DeliveryMethod(val label: String) {
    SELLER_DELIVERS("Seller delivers"),
    DENTALMARKET_DELIVERS("DentalMarket delivers")
}

enum class PaymentStatus(val label: String) {
    AWAITING_PAYMENT("Awaiting Payment"),
    PENDING_VERIFICATION("Verification Pending"),
    VERIFIED("Payment Verified"),
    REJECTED("Payment Rejected")
}
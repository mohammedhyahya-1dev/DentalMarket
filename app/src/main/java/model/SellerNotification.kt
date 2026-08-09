package com.dentalmarket.app.model

// type discriminates what this notification is about; SellerNotificationsScreen
// branches on it to decide what to render. Defaults to DELIVERY_INFO so
// documents written before this field existed still deserialize and display
// exactly as before, no migration needed (same trick as Listing.viewCount).
// Deliberately no free-text message field — the actual sentence lives in
// SellerNotificationsScreen's Kotlin, not in Firestore, since the
// DISPUTE_OPENED variant is written by the buyer's own client (see
// firestore.rules) and a stored message field would let it inject arbitrary
// text into a seller's notification feed.
object SellerNotificationType {
    // Created once per order, only when deliveryMethod is SELLER_DELIVERS,
    // the moment admin verifies that order's payment reference — see
    // OrderViewModel.verifyPayment().
    const val DELIVERY_INFO = "DELIVERY_INFO"
    // Created by the buyer's own client when they file a dispute — see
    // DisputeViewModel.fileDispute().
    const val DISPUTE_OPENED = "DISPUTE_OPENED"
    // Created by admin when they resolve a dispute — see
    // OrderViewModel.resolveDispute(). resolution carries the outcome.
    const val DISPUTE_RESOLVED = "DISPUTE_RESOLVED"
}

data class SellerNotification(
    val id: String = "",
    val recipientId: String = "",
    val orderId: String = "",
    val listingName: String = "",
    val buyerName: String = "",
    val type: String = SellerNotificationType.DELIVERY_INFO,
    val deliveryAddress: String = "",
    val deliveryContactPhone: String = "",
    // Only set for DISPUTE_RESOLVED: RESOLVED_BUYER, RESOLVED_SELLER, or
    // DISMISSED (see Dispute.status) — determines which of the three
    // resolved-notification sentences SellerNotificationsScreen shows.
    val resolution: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

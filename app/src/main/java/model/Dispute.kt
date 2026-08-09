package com.dentalmarket.app.model

// One per order, keyed by orderId itself (see DisputeRepository.fileDispute)
// — a buyer can't file a second dispute on the same order. Filing this
// blocks that order's DELIVERED -> PAID_TO_SELLER transition in
// firestore.rules until status moves off OPEN; see nextOrderStatus() and
// the orders/{orderId} update rule.
data class Dispute(
    val id: String = "", // == orderId
    val orderId: String = "",
    val listingId: String = "",
    val listingName: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "OPEN", // OPEN, RESOLVED_BUYER, RESOLVED_SELLER, DISMISSED
    val timestamp: Long = System.currentTimeMillis(),
    val resolvedAt: Long = 0
)

enum class DisputeReason(val label: String) {
    NOT_DELIVERED("Item wasn't delivered"),
    NOT_AS_DESCRIBED("Item wasn't as described"),
    OTHER("Other")
}

enum class DisputeStatus(val label: String) {
    OPEN("Open"),
    RESOLVED_BUYER("Resolved — Buyer"),
    RESOLVED_SELLER("Resolved — Seller"),
    DISMISSED("Dismissed")
}

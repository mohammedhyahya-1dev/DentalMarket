package com.dentalmarket.app.model

// Split out of Order itself so granting sellers read access to their own
// orders (see OrderRepository.getOrdersForSeller / firestore.rules) can
// never expose this — Firestore has no field-level redaction, so this is
// the only way "buyer/admin only" is a rule-enforced guarantee rather than
// a UI convention. Doc ID matches the order's own id, same trick
// ratings/{orderId} already uses. Written once at checkout (see
// OrderRepository.placeOrder's batch write), never updated after.
data class OrderDeliveryInfo(
    val orderId: String = "",
    val buyerId: String = "",
    val deliveryAddress: String = "",
    val deliveryContactPhone: String = ""
)

package com.dentalmarket.app.model

// identityVerifications/{uid} — one doc per account (uid-keyed, same
// convention as ratings/{orderId}/disputes/{orderId}), reviewed by the
// single admin account via AdminIdentityVerificationsScreen. A resubmit
// overwrites this doc's image paths and flips status back to PENDING — no
// history is kept across rejected attempts. See firestore.rules for the
// full owner/admin write split this mirrors.
data class IdentityVerification(
    val uid: String = "",
    val status: String = "PENDING",
    val submittedAt: Long = 0L,
    val reviewedAt: Long = 0L,
    val rejectionReason: String = "",
    // Storage PATHS, never a getDownloadUrl() string — see
    // IdentityVerificationRepository's own comment on why that distinction
    // is what actually keeps this collection's images private.
    val nationalIdFrontPath: String = "",
    val nationalIdBackPath: String = "",
    val dentalAssocIdFrontPath: String = "",
    val dentalAssocIdBackPath: String = ""
)

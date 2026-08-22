package com.dentalmarket.app.model

// The subset of a seller's profile visible to OTHER users, read from
// publicProfiles/{uid} — see AuthRepository.getPublicProfile and that
// collection's rule in firestore.rules. Every field here is denormalized
// (name/username from the profile form, createdAt/emailVerified from
// Firebase Auth, identityVerified from an approved identityVerifications/{uid}
// submission) so SellerProfileScreen never needs more than this one read.
data class PublicSellerProfile(
    val name: String = "",
    val username: String = "",
    val createdAt: Long = 0L,
    val emailVerified: Boolean = false,
    // Admin-approved real identity check — see IdentityVerificationRepository.
    // Only ever set true by AuthRepository.markIdentityVerified() (admin-only
    // per firestore.rules' publicProfiles carve-out); the owner can never set
    // this on themselves.
    val identityVerified: Boolean = false
)

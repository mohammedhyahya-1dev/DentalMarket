package com.dentalmarket.app.model

// One doc per follower/seller pair, keyed by "followerId_followingId" — same
// trick as watchlist/{uid}_{listingId} and reports/{reporterId}_{listingId}
// (see FollowRepository.docId). followingId is always a seller's uid, never
// a listing id — this is a person-to-person relationship, not a saved item.
data class Follow(
    val followerId: String = "",
    val followingId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

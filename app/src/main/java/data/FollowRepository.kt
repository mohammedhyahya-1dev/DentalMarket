package com.dentalmarket.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore

class FollowRepository {
    private val auth = FirebaseAuth.getInstance()
    private val followsCollection = FirebaseFirestore.getInstance().collection("follows")

    // One doc per follower+seller pair, so "am I following" and "follow/
    // unfollow" are all simple document lookups — same trick
    // WatchlistRepository.docId uses for buyer+listing.
    private fun docId(sellerId: String): String {
        val uid = auth.currentUser?.uid ?: ""
        return "${uid}_$sellerId"
    }

    suspend fun follow(sellerId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not signed in")
            val data = mapOf(
                "followerId" to uid,
                "followingId" to sellerId,
                "createdAt" to System.currentTimeMillis()
            )
            followsCollection.document(docId(sellerId)).set(data).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollow(sellerId: String): Result<Unit> {
        return try {
            followsCollection.document(docId(sellerId)).delete().awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(sellerId: String): Result<Boolean> {
        return try {
            val doc = followsCollection.document(docId(sellerId)).get().awaitResult()
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Server-side count() — no documents actually transferred, so this stays
    // cheap regardless of how many followers a seller has. Same reasoning
    // RatingRepository.getRatingSummariesForSellers gives for batching by
    // seller rather than reading every rating doc client-side.
    suspend fun getFollowerCount(sellerId: String): Result<Int> {
        return try {
            val snapshot = followsCollection
                .whereEqualTo("followingId", sellerId)
                .count()
                .get(AggregateSource.SERVER)
                .awaitResult()
            Result.success(snapshot.count.toInt())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowingCount(uid: String): Result<Int> {
        return try {
            val snapshot = followsCollection
                .whereEqualTo("followerId", uid)
                .count()
                .get(AggregateSource.SERVER)
                .awaitResult()
            Result.success(snapshot.count.toInt())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

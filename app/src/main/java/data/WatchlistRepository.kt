package com.dentalmarket.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WatchlistRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // One doc per buyer+listing pair, so "is this watched" and "add/remove"
    // are all simple, cheap document lookups instead of querying a list.
    private fun docId(listingId: String): String {
        val uid = auth.currentUser?.uid ?: ""
        return "${uid}_$listingId"
    }

    suspend fun addToWatchlist(listingId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not signed in")
            val data = mapOf(
                "buyerId" to uid,
                "listingId" to listingId,
                "addedAt" to System.currentTimeMillis()
            )
            firestore.collection("watchlist").document(docId(listingId)).set(data).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWatchlist(listingId: String): Result<Unit> {
        return try {
            firestore.collection("watchlist").document(docId(listingId)).delete().awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWatchlistedListingIds(): Result<List<String>> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not signed in")
            val snapshot = firestore.collection("watchlist")
                .whereEqualTo("buyerId", uid)
                .get()
                .awaitResult()
            Result.success(snapshot.documents.mapNotNull { it.getString("listingId") })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
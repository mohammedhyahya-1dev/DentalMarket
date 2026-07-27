package com.dentalmarket.app.data

import com.dentalmarket.app.model.Rating
import com.dentalmarket.app.model.SellerRatingSummary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RatingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val ratingsCollection = db.collection("ratings")

    // One rating doc per order, keyed by the orderId itself, so a second
    // submit for the same order overwrites the buyer's own doc instead of
    // creating a duplicate — the same one-doc-per-pair trick WatchlistRepository uses.
    suspend fun submitRating(rating: Rating): Result<Unit> {
        return try {
            ratingsCollection.document(rating.orderId).set(rating).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Which of this buyer's orders already have a rating on file —
    // powers the "Rate this Seller" prompt gating on My Orders.
    suspend fun getRatedOrderIdsForBuyer(buyerId: String): Result<Set<String>> {
        return try {
            val snapshot = ratingsCollection.whereEqualTo("buyerId", buyerId).get().await()
            Result.success(snapshot.documents.mapNotNull { it.getString("orderId") }.toSet())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Every rating left for one seller — for a future "all reviews" list.
    suspend fun getRatingsForSeller(sellerId: String): Result<List<Rating>> {
        return try {
            val snapshot = ratingsCollection.whereEqualTo("sellerId", sellerId).get().await()
            val ratings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Rating::class.java)?.copy(id = doc.id)
            }
            Result.success(ratings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Average + count per seller, batched in groups of 10 (Firestore's
    // whereIn limit) so a marketplace grid full of listings doesn't fire
    // one query per card.
    suspend fun getRatingSummariesForSellers(sellerIds: List<String>): Result<Map<String, SellerRatingSummary>> {
        return try {
            val starsBySeller = mutableMapOf<String, MutableList<Int>>()
            for (chunk in sellerIds.distinct().chunked(10)) {
                val snapshot = ratingsCollection.whereIn("sellerId", chunk).get().await()
                for (doc in snapshot.documents) {
                    val sellerId = doc.getString("sellerId") ?: continue
                    val stars = doc.getLong("stars")?.toInt() ?: continue
                    starsBySeller.getOrPut(sellerId) { mutableListOf() }.add(stars)
                }
            }
            val summaries = starsBySeller.mapValues { (_, stars) ->
                SellerRatingSummary(average = stars.average(), count = stars.size)
            }
            Result.success(summaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

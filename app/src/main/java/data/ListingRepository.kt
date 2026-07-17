package com.dentalmarket.app.data

import com.dentalmarket.app.model.Listing
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ListingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val listingsCollection = db.collection("listings")

    suspend fun addListing(listing: Listing): Result<Unit> {
        return try {
            listingsCollection.add(listing).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllListings(): Result<List<Listing>> {
        return try {
            val snapshot = listingsCollection.get().await()
            val listings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListingById(id: String): Result<Listing?> {
        return try {
            val doc = listingsCollection.document(id).get().await()
            val listing = doc.toObject(Listing::class.java)?.copy(id = doc.id)
            Result.success(listing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
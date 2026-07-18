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
            Result.success(listings.filter { it.status != "SOLD" })
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

    // Every listing one specific dentist has posted, sold or not \u2014
    // powers the "My Listings" management screen.
    suspend fun getListingsBySeller(sellerId: String): Result<List<Listing>> {
        return try {
            val snapshot = listingsCollection.whereEqualTo("sellerId", sellerId).get().await()
            val listings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.copy(id = doc.id)
            }
            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsSold(listingId: String): Result<Unit> {
        return try {
            listingsCollection.document(listingId).update("status", "SOLD").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateListing(listingId: String, listing: Listing): Result<Unit> {
        return try {
            listingsCollection.document(listingId).set(listing).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        return try {
            listingsCollection.document(listingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
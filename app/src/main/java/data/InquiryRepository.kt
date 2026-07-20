package com.dentalmarket.app.data

import com.dentalmarket.app.model.Inquiry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InquiryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val inquiriesCollection = db.collection("inquiries")

    suspend fun addInquiry(inquiry: Inquiry): Result<Unit> {
        return try {
            inquiriesCollection.add(inquiry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Every question one specific dentist has asked \u2014 powers "My Questions".
    suspend fun getInquiriesForBuyer(buyerId: String): Result<List<Inquiry>> {
        return try {
            val snapshot = inquiriesCollection.whereEqualTo("buyerId", buyerId).get().await()
            val inquiries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Inquiry::class.java)?.copy(id = doc.id)
            }
            Result.success(inquiries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Every question from every buyer \u2014 powers the admin inbox.
    suspend fun getAllInquiries(): Result<List<Inquiry>> {
        return try {
            val snapshot = inquiriesCollection.get().await()
            val inquiries = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Inquiry::class.java)?.copy(id = doc.id)
            }
            Result.success(inquiries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun answerInquiry(inquiryId: String, answer: String): Result<Unit> {
        return try {
            inquiriesCollection.document(inquiryId)
                .update(mapOf("answer" to answer, "status" to "ANSWERED"))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package com.dentalmarket.app.data

import com.dentalmarket.app.model.Dispute
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

class DisputeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val disputesCollection = db.collection("disputes")

    // Doc id is the orderId itself, same trick as ratings/{orderId} — a
    // second filing on the same order lands on this doc, which
    // firestore.rules denies as an update (only admin can update, to flip
    // status), enforcing one-dispute-per-order without a query.
    suspend fun fileDispute(dispute: Dispute): Result<Unit> {
        return try {
            disputesCollection.document(dispute.orderId).set(dispute).await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Result.failure(Exception("You've already filed a dispute for this order"))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDispute(orderId: String): Result<Dispute?> {
        return try {
            val doc = disputesCollection.document(orderId).get().await()
            Result.success(doc.toObject(Dispute::class.java)?.copy(id = doc.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Batched by document id in groups of 10, same chunking pattern as
    // OrderDeliveryInfoRepository.getDeliveryInfoForOrders — AdminOrdersScreen
    // needs every order's dispute (if any) without one query per order.
    suspend fun getDisputesForOrders(orderIds: List<String>): Result<Map<String, Dispute>> {
        return try {
            val result = mutableMapOf<String, Dispute>()
            for (chunk in orderIds.distinct().chunked(10)) {
                val snapshot = disputesCollection
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                for (doc in snapshot.documents) {
                    doc.toObject(Dispute::class.java)?.let { result[doc.id] = it.copy(id = doc.id) }
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveDispute(orderId: String, newStatus: String): Result<Unit> {
        return try {
            disputesCollection.document(orderId).update(
                mapOf(
                    "status" to newStatus,
                    "resolvedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

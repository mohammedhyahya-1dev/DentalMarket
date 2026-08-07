package com.dentalmarket.app.data

import com.dentalmarket.app.model.OrderDeliveryInfo
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OrderDeliveryInfoRepository {
    private val db = FirebaseFirestore.getInstance()
    private val deliveryInfoCollection = db.collection("orderDeliveryInfo")

    suspend fun getDeliveryInfo(orderId: String): Result<OrderDeliveryInfo?> {
        return try {
            val doc = deliveryInfoCollection.document(orderId).get().await()
            Result.success(doc.toObject(OrderDeliveryInfo::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Batched by document id in groups of 10 (Firestore's whereIn limit) so
    // a list of many orders doesn't fire one query per order — same
    // chunking pattern RatingRepository.getRatingSummariesForSellers() uses.
    suspend fun getDeliveryInfoForOrders(orderIds: List<String>): Result<Map<String, OrderDeliveryInfo>> {
        return try {
            val result = mutableMapOf<String, OrderDeliveryInfo>()
            for (chunk in orderIds.distinct().chunked(10)) {
                val snapshot = deliveryInfoCollection
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                for (doc in snapshot.documents) {
                    doc.toObject(OrderDeliveryInfo::class.java)?.let { result[doc.id] = it }
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

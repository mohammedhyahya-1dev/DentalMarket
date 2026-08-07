package com.dentalmarket.app.data

import com.dentalmarket.app.model.Order
import com.dentalmarket.app.model.SellerNotification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SellerNotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("sellerNotifications")

    suspend fun createNotification(
        order: Order,
        deliveryAddress: String,
        deliveryContactPhone: String
    ): Result<Unit> {
        return try {
            val notification = SellerNotification(
                recipientId = order.sellerId,
                orderId = order.id,
                listingName = order.listingName,
                buyerName = order.buyerName,
                deliveryAddress = deliveryAddress,
                deliveryContactPhone = deliveryContactPhone
            )
            notificationsCollection.add(notification).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotificationsForSeller(sellerId: String): Result<List<SellerNotification>> {
        return try {
            val snapshot = notificationsCollection.whereEqualTo("recipientId", sellerId).get().await()
            val notifications = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SellerNotification::class.java)?.copy(id = doc.id)
            }
            Result.success(notifications.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllRead(notifications: List<SellerNotification>): Result<Unit> {
        return try {
            notifications.filterNot { it.read }.forEach { notification ->
                notificationsCollection.document(notification.id).update("read", true).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.dentalmarket.app.data

import com.dentalmarket.app.model.Order
import com.dentalmarket.app.model.SellerNotification
import com.dentalmarket.app.model.SellerNotificationType
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

    // Buyer-initiated — see firestore.rules' sellerNotifications create rule
    // for the DISPUTE_OPENED-specific path that allows this non-admin write.
    suspend fun createDisputeOpenedNotification(order: Order): Result<Unit> {
        return try {
            val notification = SellerNotification(
                recipientId = order.sellerId,
                orderId = order.id,
                listingName = order.listingName,
                buyerName = order.buyerName,
                type = SellerNotificationType.DISPUTE_OPENED
            )
            notificationsCollection.add(notification).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Admin-initiated, alongside OrderViewModel.resolveDispute().
    suspend fun createDisputeResolvedNotification(order: Order, resolution: String): Result<Unit> {
        return try {
            val notification = SellerNotification(
                recipientId = order.sellerId,
                orderId = order.id,
                listingName = order.listingName,
                buyerName = order.buyerName,
                type = SellerNotificationType.DISPUTE_RESOLVED,
                resolution = resolution
            )
            notificationsCollection.add(notification).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Self-initiated — see firestore.rules' sellerNotifications create rule
    // for the VERIFICATION_SUBMITTED-specific path that lets a non-admin
    // user write into the admin's own notification feed (recipientId must
    // equal the hardcoded ADMIN_UID, same as AuthRepository's).
    suspend fun createVerificationSubmittedNotification(recipientId: String, submitterLabel: String): Result<Unit> {
        return try {
            val notification = SellerNotification(
                recipientId = recipientId,
                listingName = "Identity Verification Submitted",
                buyerName = submitterLabel,
                type = SellerNotificationType.VERIFICATION_SUBMITTED
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

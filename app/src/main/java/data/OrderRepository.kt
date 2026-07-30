package com.dentalmarket.app.data

import com.dentalmarket.app.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OrderRepository {
    private val db = FirebaseFirestore.getInstance()
    private val ordersCollection = db.collection("orders")

    suspend fun placeOrder(order: Order): Result<Unit> {
        return try {
            ordersCollection.add(order).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderById(orderId: String): Result<Order?> {
        return try {
            val doc = ordersCollection.document(orderId).get().await()
            val order = doc.toObject(Order::class.java)?.copy(id = doc.id)
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrdersForBuyer(buyerId: String): Result<List<Order>> {
        return try {
            val snapshot = ordersCollection.whereEqualTo("buyerId", buyerId).get().await()
            val orders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }
            Result.success(orders.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllOrders(): Result<List<Order>> {
        return try {
            val snapshot = ordersCollection.get().await()
            val orders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }
            Result.success(orders.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // These three are the swappable seam: today a buyer types a reference
    // number and an admin manually checks it, but nothing here presumes a
    // human is on either end — a future ZainCash/QiCard API integration can
    // call the same two verification methods from a webhook instead of a
    // button tap, with no change to the Order model or the rest of the app.

    suspend fun submitPaymentReference(orderId: String, reference: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(
                mapOf(
                    "paymentReference" to reference,
                    "paymentSubmittedAt" to System.currentTimeMillis(),
                    "paymentStatus" to "PENDING_VERIFICATION"
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPayment(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(
                mapOf(
                    "paymentStatus" to "VERIFIED",
                    "paymentVerifiedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectPayment(orderId: String, reason: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(
                mapOf(
                    "paymentStatus" to "REJECTED",
                    "paymentRejectionReason" to reason,
                    "paymentVerifiedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
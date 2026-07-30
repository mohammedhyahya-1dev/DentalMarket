package com.dentalmarket.app.data

import com.dentalmarket.app.model.PaymentConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PaymentConfigRepository {
    private val db = FirebaseFirestore.getInstance()

    // A single well-known document, edited directly in the Firebase console
    // so the QiCard/ZainCash account numbers can change without a new
    // app release.
    suspend fun getPaymentConfig(): Result<PaymentConfig> {
        return try {
            val doc = db.collection("config").document("payment").get().await()
            Result.success(doc.toObject(PaymentConfig::class.java) ?: PaymentConfig())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.dentalmarket.app.data

import com.dentalmarket.app.model.DeliveryConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DeliveryConfigRepository {
    private val db = FirebaseFirestore.getInstance()

    // A single well-known document, edited directly in the Firebase console
    // so the DentalMarket-delivers fee can change without a new app release.
    suspend fun getDeliveryConfig(): Result<DeliveryConfig> {
        return try {
            val doc = db.collection("config").document("delivery").get().await()
            Result.success(doc.toObject(DeliveryConfig::class.java) ?: DeliveryConfig())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

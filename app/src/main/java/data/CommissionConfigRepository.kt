package com.dentalmarket.app.data

import com.dentalmarket.app.model.CommissionConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CommissionConfigRepository {
    private val db = FirebaseFirestore.getInstance()

    // A single well-known document, edited directly in the Firebase console
    // so the commission percentage can change without a new app release.
    suspend fun getCommissionConfig(): Result<CommissionConfig> {
        return try {
            val doc = db.collection("config").document("commission").get().await()
            Result.success(doc.toObject(CommissionConfig::class.java) ?: CommissionConfig())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

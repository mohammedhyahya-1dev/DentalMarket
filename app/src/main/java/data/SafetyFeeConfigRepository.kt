package com.dentalmarket.app.data

import com.dentalmarket.app.model.SafetyFeeConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SafetyFeeConfigRepository {
    private val db = FirebaseFirestore.getInstance()

    // A single well-known document, edited directly in the Firebase console
    // so the buyer safety fee can change without a new app release.
    suspend fun getSafetyFeeConfig(): Result<SafetyFeeConfig> {
        return try {
            val doc = db.collection("config").document("safetyFee").get().await()
            Result.success(doc.toObject(SafetyFeeConfig::class.java) ?: SafetyFeeConfig())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

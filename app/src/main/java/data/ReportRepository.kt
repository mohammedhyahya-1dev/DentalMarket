package com.dentalmarket.app.data

import com.dentalmarket.app.model.Report
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

class ReportRepository {
    private val auth = FirebaseAuth.getInstance()
    private val reportsCollection = FirebaseFirestore.getInstance().collection("reports")

    // One doc per reporter+listing pair — a second report for the same pair
    // lands on the same doc id, which firestore.rules denies as an update
    // (create-only), enforcing the one-report-per-listing rule server-side
    // rather than needing a query to check first. That denial surfaces here
    // as PERMISSION_DENIED, which is translated to a friendly message since
    // it's the only realistic way this write hits that code — guests are
    // already blocked from reaching this call by ProductDetailScreen's
    // requireLogin gate.
    suspend fun addReport(report: Report): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not signed in")
            val docId = "${uid}_${report.listingId}"
            reportsCollection.document(docId).set(report.copy(reporterId = uid)).await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Result.failure(Exception("You've already reported this listing"))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

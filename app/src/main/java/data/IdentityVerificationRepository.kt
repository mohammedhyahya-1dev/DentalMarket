package com.dentalmarket.app.data

import com.dentalmarket.app.model.IdentityVerification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata

private val IMAGE_SLOTS = listOf(
    "nationalIdFront", "nationalIdBack", "dentalAssocIdFront", "dentalAssocIdBack"
)

// identityVerifications/{uid} + the matching Storage paths underneath it —
// the one collection in this app where privacy is the actual point (see
// storage.rules). Deliberately never hands back a getDownloadUrl() string
// to store anywhere: that URL embeds a long-lived access token, so anyone
// holding the string could view the image forever regardless of what
// storage.rules says. Only the PATH is ever persisted (in Firestore); a
// fresh download URL is minted at render time, by whoever is actually
// authorized to see it right now.
class IdentityVerificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val collection = firestore.collection("identityVerifications")
    private val notificationRepository = SellerNotificationRepository()

    // Deterministic path per uid+slot — a resubmit overwrites the previous
    // rejected image at the same path instead of accumulating orphaned
    // files across rejection cycles.
    private fun storagePath(uid: String, slot: String) = "identityVerifications/$uid/$slot.jpg"

    // slot must be one of IMAGE_SLOTS — enforced by IdentityVerificationScreen
    // only ever calling this with its own fixed 4 slot names, not user input.
    suspend fun uploadImage(uid: String, slot: String, jpegBytes: ByteArray): Result<String> {
        return try {
            val path = storagePath(uid, slot)
            // storage.rules requires contentType.matches('image/.*') — putBytes()
            // with no metadata defaults to application/octet-stream, which that
            // check (correctly) rejects. Found this the hard way: the very
            // first live upload attempt failed with "User does not have
            // permission to access this object." until this metadata was added.
            val metadata = storageMetadata { contentType = "image/jpeg" }
            storage.reference.child(path).putBytes(jpegBytes, metadata).awaitResult()
            Result.success(path)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Minted fresh every call — never cached/stored (see class comment).
    suspend fun getDownloadUrl(path: String): Result<String> {
        return try {
            val url = storage.reference.child(path).downloadUrl.awaitResult()
            Result.success(url.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMySubmission(uid: String): Result<IdentityVerification?> {
        return try {
            val doc = collection.document(uid).get().awaitResult()
            Result.success(if (doc.exists()) doc.toObject(IdentityVerification::class.java) else null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // First-ever submission — matches firestore.rules' create branch exactly,
    // which requires the uid field to be present and self-owned.
    suspend fun submitNew(uid: String, paths: Map<String, String>): Result<Unit> {
        return try {
            collection.document(uid).set(
                mapOf(
                    "uid" to uid,
                    "status" to "PENDING",
                    "submittedAt" to System.currentTimeMillis()
                ) + paths
            ).awaitResult()
            notifyAdminOfSubmission(uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // A REJECTED -> PENDING resubmit — matches firestore.rules' owner-update
    // branch exactly, which restricts the diff to status/submittedAt/the 4
    // paths and deliberately does NOT include uid (that branch never checks
    // it, so including it here would fail the rule's hasOnly() diff check).
    suspend fun resubmit(uid: String, paths: Map<String, String>): Result<Unit> {
        return try {
            collection.document(uid).set(
                mapOf(
                    "status" to "PENDING",
                    "submittedAt" to System.currentTimeMillis()
                ) + paths,
                SetOptions.merge()
            ).awaitResult()
            notifyAdminOfSubmission(uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Best-effort — a failure here shouldn't fail the submission itself
    // (same posture as OrderViewModel.verifyPayment()'s notification call).
    // Reuses the EXISTING sellerNotifications bell/badge system rather than
    // any new infrastructure; see firestore.rules for the matching
    // VERIFICATION_SUBMITTED create rule this write has to satisfy.
    private suspend fun notifyAdminOfSubmission(uid: String) {
        try {
            val userDoc = firestore.collection("users").document(uid).get().awaitResult()
            val name = userDoc.getString("name")?.takeIf { it.isNotBlank() } ?: "A user"
            val username = userDoc.getString("username")?.takeIf { it.isNotBlank() }
            val label = if (username != null) "$name (@$username)" else name
            notificationRepository.createVerificationSubmittedNotification(ADMIN_UID, label)
        } catch (e: Exception) {
            // Ignored — see comment above.
        }
    }

    // Admin-only per firestore.rules (unenforced here — the rule is the
    // actual boundary, same posture as every other admin action in this app).
    suspend fun getPendingSubmissions(): Result<List<IdentityVerification>> {
        return try {
            val snapshot = collection.whereEqualTo("status", "PENDING").get().awaitResult()
            Result.success(snapshot.toObjects(IdentityVerification::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approve(uid: String): Result<Unit> {
        return try {
            collection.document(uid).update(
                mapOf("status" to "APPROVED", "reviewedAt" to System.currentTimeMillis())
            ).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reject(uid: String, reason: String): Result<Unit> {
        return try {
            collection.document(uid).update(
                mapOf(
                    "status" to "REJECTED",
                    "reviewedAt" to System.currentTimeMillis(),
                    "rejectionReason" to reason
                )
            ).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// The 4 fixed upload-slot names, shared by the submission screen (which
// picks/compresses/uploads one per slot) and the repository (which turns
// each into a deterministic Storage path) — kept as one source of truth so
// the two can't drift apart.
fun identityVerificationImageSlots(): List<String> = IMAGE_SLOTS

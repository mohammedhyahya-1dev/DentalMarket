package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.IdentityVerificationRepository
import com.dentalmarket.app.model.IdentityVerification
import kotlinx.coroutines.launch

// Backs both IdentityVerificationScreen (self-submission) and
// AdminIdentityVerificationsScreen (review queue) — same combined-viewmodel
// shape as OrderViewModel, which already mixes a buyer's own order actions
// with the admin-only verify/reject-payment ones.
class IdentityVerificationViewModel : ViewModel() {
    private val repository = IdentityVerificationRepository()
    private val authRepository = AuthRepository()

    // --- Self-submission ---
    var mySubmission = mutableStateOf<IdentityVerification?>(null)
    var isLoadingMySubmission = mutableStateOf(true)
    var isSubmitting = mutableStateOf(false)
    var submitError = mutableStateOf<String?>(null)

    fun loadMySubmission() {
        val uid = authRepository.currentUserId ?: return
        isLoadingMySubmission.value = true
        viewModelScope.launch {
            repository.getMySubmission(uid).onSuccess { mySubmission.value = it }
            isLoadingMySubmission.value = false
        }
    }

    // images: slot name (see identityVerificationImageSlots()) -> already-
    // compressed JPEG bytes for that slot. Uploads all 4 to Storage first,
    // then writes the Firestore submission doc referencing the resulting
    // paths — matches storage.rules' expectation that the Firestore doc
    // never claims a path before that file actually exists.
    fun submit(images: Map<String, ByteArray>, onSuccess: () -> Unit) {
        val uid = authRepository.currentUserId ?: return
        isSubmitting.value = true
        submitError.value = null
        viewModelScope.launch {
            val paths = mutableMapOf<String, String>()
            var uploadFailure: String? = null
            for ((slot, bytes) in images) {
                repository.uploadImage(uid, slot, bytes)
                    .onSuccess { path -> paths[fieldNameForSlot(slot)] = path }
                    .onFailure { uploadFailure = it.message ?: "Failed to upload $slot" }
                if (uploadFailure != null) break
            }

            if (uploadFailure != null) {
                submitError.value = uploadFailure
                isSubmitting.value = false
                return@launch
            }

            val isResubmit = mySubmission.value?.status == "REJECTED"
            val result = if (isResubmit) repository.resubmit(uid, paths) else repository.submitNew(uid, paths)
            result
                .onSuccess {
                    loadMySubmission()
                    onSuccess()
                }
                .onFailure { submitError.value = it.message ?: "Failed to submit for review" }
            isSubmitting.value = false
        }
    }

    // --- Admin review ---
    var pendingSubmissions = mutableStateOf<List<IdentityVerification>>(emptyList())
    var isLoadingPending = mutableStateOf(false)
    // uid -> best-effort display label ("Name (@username)"), fetched once
    // per load so AdminIdentityVerificationsScreen's cards don't each need
    // their own read.
    var submitterLabels = mutableStateOf<Map<String, String>>(emptyMap())
    // path -> a freshly-minted download URL — never persisted anywhere,
    // just held in memory for as long as this screen is open (see
    // IdentityVerificationRepository's own comment on why a stored URL
    // would defeat the point of storage.rules).
    var imageUrls = mutableStateOf<Map<String, String>>(emptyMap())
    // uid currently mid-approve/reject, so its card's buttons can disable
    // rather than allow a double-tap double-submit.
    var processingUid = mutableStateOf<String?>(null)

    fun loadPendingSubmissions() {
        isLoadingPending.value = true
        viewModelScope.launch {
            repository.getPendingSubmissions().onSuccess { submissions ->
                pendingSubmissions.value = submissions

                val labels = mutableMapOf<String, String>()
                val urls = mutableMapOf<String, String>()
                for (submission in submissions) {
                    authRepository.getPublicProfile(submission.uid).onSuccess { profile ->
                        val name = profile.name.takeIf { it.isNotBlank() } ?: "Unknown"
                        labels[submission.uid] = if (profile.username.isNotBlank()) {
                            "$name (@${profile.username})"
                        } else name
                    }
                    for (path in listOf(
                        submission.nationalIdFrontPath, submission.nationalIdBackPath,
                        submission.dentalAssocIdFrontPath, submission.dentalAssocIdBackPath
                    )) {
                        repository.getDownloadUrl(path).onSuccess { urls[path] = it }
                    }
                }
                submitterLabels.value = labels
                imageUrls.value = urls
            }
            isLoadingPending.value = false
        }
    }

    fun approve(uid: String) {
        processingUid.value = uid
        viewModelScope.launch {
            repository.approve(uid).onSuccess {
                authRepository.markIdentityVerified(uid)
            }
            processingUid.value = null
            loadPendingSubmissions()
        }
    }

    fun reject(uid: String, reason: String) {
        processingUid.value = uid
        viewModelScope.launch {
            repository.reject(uid, reason)
            processingUid.value = null
            loadPendingSubmissions()
        }
    }
}

// nationalIdFront -> nationalIdFrontPath, etc. — the Firestore field name
// for a given upload slot.
private fun fieldNameForSlot(slot: String): String = "${slot}Path"

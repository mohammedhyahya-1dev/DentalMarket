package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.IdentityVerificationRepository
import com.dentalmarket.app.model.DentalUser
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val identityVerificationRepository = IdentityVerificationRepository()

    var profile = mutableStateOf<DentalUser?>(null)
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    var isEmailVerified = mutableStateOf(authRepository.isEmailVerified)
    var resendSuccess = mutableStateOf(false)

    // Status of the current user's OWN identityVerifications/{uid} submission
    // — null means none exists yet. Lives here rather than on DentalUser,
    // same posture as isEmailVerified above: this is never something the
    // owner can freely set on themselves (see firestore.rules), so it isn't
    // a plain field on a self-writable document either.
    var verificationStatus = mutableStateOf<String?>(null)

    val isAdmin: Boolean
        get() = authRepository.isAdmin

    fun loadProfile() {
        isLoading.value = true
        viewModelScope.launch {
            authRepository.reloadUser()
            isEmailVerified.value = authRepository.isEmailVerified

            val result = authRepository.getCurrentUserProfile()
            isLoading.value = false
            result.onSuccess { profile.value = it }
            result.onFailure { errorMessage.value = it.message }

            authRepository.currentUserId?.let { uid ->
                identityVerificationRepository.getMySubmission(uid)
                    .onSuccess { verificationStatus.value = it?.status }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            val result = authRepository.resendVerificationEmail()
            result.onSuccess { resendSuccess.value = true }
            result.onFailure { errorMessage.value = it.message }
        }
    }

    var isChangingUsername = mutableStateOf(false)
    var usernameErrorMessage = mutableStateOf<String?>(null)

    fun changeUsername(newUsername: String, onSuccess: () -> Unit) {
        isChangingUsername.value = true
        usernameErrorMessage.value = null
        viewModelScope.launch {
            val result = authRepository.changeUsername(newUsername)
            result.onSuccess {
                profile.value = profile.value?.copy(username = newUsername.trim())
                onSuccess()
            }
            result.onFailure { usernameErrorMessage.value = it.message ?: "Failed to change username" }
            isChangingUsername.value = false
        }
    }
}
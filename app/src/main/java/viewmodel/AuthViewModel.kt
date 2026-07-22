package com.dentalmarket.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val isLoggedIn: Boolean
        get() = repository.isLoggedIn

    val currentUserId: String?
        get() = repository.currentUserId

    fun signUp(name: String, email: String, password: String, onSuccess: () -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.signUp(name, email, password)
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message ?: "Sign up failed" }
            _isLoading.value = false
        }
    }

    fun logIn(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.logIn(email, password)
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message ?: "Login failed" }
            _isLoading.value = false
        }
    }
    fun logInOrSignUp(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }
        if (!email.trim().lowercase().endsWith("@gmail.com")) {
            _errorMessage.value = "Please use a Gmail address (@gmail.com)"
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.logInOrSignUp(email, password)
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message ?: "Sign in failed" }
            _isLoading.value = false
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.signInWithGoogleIdToken(idToken)
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message ?: "Google sign-in failed" }
            _isLoading.value = false
        }
    }
    fun checkProfileComplete(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.getCurrentUserProfile()
            onResult(result.getOrNull()?.profileComplete ?: false)
        }
    }
    fun sendPasswordReset(email: String, onSuccess: () -> Unit) {
        if (email.isBlank()) {
            _errorMessage.value = "Please enter your email"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.sendPasswordReset(email)
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message ?: "Failed to send reset email" }
            _isLoading.value = false
        }
    }
    fun signOut() {
        repository.signOut()
    }
    fun completeProfile(
        title: String,
        firstName: String,
        lastName: String,
        specialty: String,
        province: String,
        mobile: String,
        extraMobile: String,
        onSuccess: () -> Unit
    ) {
        if (title.isBlank() || firstName.isBlank() || lastName.isBlank() ||
            specialty.isBlank() || province.isBlank() || mobile.isBlank()
        ) {
            _errorMessage.value = "Please fill in all required fields"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.completeProfile(title, firstName, lastName, specialty, province, mobile, extraMobile)
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message ?: "Failed to save profile" }
            _isLoading.value = false
        }
    }
}
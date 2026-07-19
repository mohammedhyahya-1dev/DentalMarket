package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.model.DentalUser
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    var profile = mutableStateOf<DentalUser?>(null)
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    val isAdmin: Boolean
        get() = authRepository.isAdmin

    fun loadProfile() {
        isLoading.value = true
        viewModelScope.launch {
            val result = authRepository.getCurrentUserProfile()
            isLoading.value = false
            result.onSuccess { profile.value = it }
            result.onFailure { errorMessage.value = it.message }
        }
    }
}
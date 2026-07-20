package com.dentalmarket.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.InquiryRepository
import com.dentalmarket.app.model.Inquiry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InquiryViewModel : ViewModel() {

    private val repository = InquiryRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _inquiries = MutableStateFlow<List<Inquiry>>(emptyList())
    val inquiries: StateFlow<List<Inquiry>> = _inquiries

    fun submitInquiry(inquiry: Inquiry, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.addInquiry(inquiry)
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message ?: "Failed to send question" }
            _isLoading.value = false
        }
    }

    fun loadMyQuestions(buyerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getInquiriesForBuyer(buyerId)
                .onSuccess { _inquiries.value = it }
                .onFailure { _errorMessage.value = it.message ?: "Failed to load questions" }
            _isLoading.value = false
        }
    }

    fun loadAllInquiries() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getAllInquiries()
                .onSuccess { _inquiries.value = it }
                .onFailure { _errorMessage.value = it.message ?: "Failed to load questions" }
            _isLoading.value = false
        }
    }

    fun answerInquiry(inquiryId: String, answer: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.answerInquiry(inquiryId, answer)
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message ?: "Failed to send answer" }
            _isLoading.value = false
        }
    }
}
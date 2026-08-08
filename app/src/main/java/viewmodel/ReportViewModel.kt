package com.dentalmarket.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.ReportRepository
import com.dentalmarket.app.model.Report
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {
    private val repository = ReportRepository()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun submitReport(report: Report, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            repository.addReport(report)
                .onSuccess { onSuccess() }
                .onFailure { _errorMessage.value = it.message ?: "Failed to submit report" }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

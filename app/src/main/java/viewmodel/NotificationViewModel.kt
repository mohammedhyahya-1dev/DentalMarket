package com.dentalmarket.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.SellerNotificationRepository
import com.dentalmarket.app.model.SellerNotification
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = SellerNotificationRepository()
    private val authRepository = AuthRepository()

    var notifications = mutableStateOf<List<SellerNotification>>(emptyList())
    var isLoading = mutableStateOf(false)

    val unreadCount: Int
        get() = notifications.value.count { !it.read }

    fun loadNotifications() {
        val sellerId = authRepository.currentUserId ?: return
        isLoading.value = true
        viewModelScope.launch {
            repository.getNotificationsForSeller(sellerId)
                .onSuccess { notifications.value = it }
            isLoading.value = false
        }
    }

    // Marks everything read as soon as the list screen is opened — this is a
    // lightweight glance-and-go list, not something with a per-item detail
    // view to tap into.
    fun markAllRead() {
        val unread = notifications.value.filterNot { it.read }
        if (unread.isEmpty()) return
        viewModelScope.launch {
            repository.markAllRead(unread)
                .onSuccess {
                    notifications.value = notifications.value.map { it.copy(read = true) }
                }
        }
    }
}

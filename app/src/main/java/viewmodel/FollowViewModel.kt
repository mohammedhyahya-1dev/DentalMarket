package com.dentalmarket.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.FollowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Scoped to a single Seller Profile screen at a time (unlike WatchlistViewModel,
// which tracks every watched id at once) — there's only ever one seller's
// follow state and counts on screen, so no need for a Set/Map here.
class FollowViewModel : ViewModel() {
    private val repository = FollowRepository()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing

    private val _followerCount = MutableStateFlow(0)
    val followerCount: StateFlow<Int> = _followerCount

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount

    fun load(sellerId: String) {
        viewModelScope.launch {
            repository.isFollowing(sellerId).onSuccess { _isFollowing.value = it }
        }
        refreshCounts(sellerId)
    }

    private fun refreshCounts(sellerId: String) {
        viewModelScope.launch {
            repository.getFollowerCount(sellerId).onSuccess { _followerCount.value = it }
        }
        viewModelScope.launch {
            repository.getFollowingCount(sellerId).onSuccess { _followingCount.value = it }
        }
    }

    fun toggleFollow(sellerId: String) {
        val wasFollowing = _isFollowing.value
        // Update instantly so the button feels responsive, same optimistic-
        // then-roll-back-on-failure pattern as WatchlistViewModel.toggleWatch.
        _isFollowing.value = !wasFollowing
        _followerCount.value = if (wasFollowing) _followerCount.value - 1 else _followerCount.value + 1
        viewModelScope.launch {
            val result = if (wasFollowing) {
                repository.unfollow(sellerId)
            } else {
                repository.follow(sellerId)
            }
            result.onFailure {
                _isFollowing.value = wasFollowing
                _followerCount.value = if (wasFollowing) _followerCount.value + 1 else _followerCount.value - 1
            }
        }
    }
}

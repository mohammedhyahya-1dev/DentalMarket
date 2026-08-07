package com.dentalmarket.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dentalmarket.app.data.WatchlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WatchlistViewModel : ViewModel() {
    private val repository = WatchlistRepository()

    private val _watchedIds = MutableStateFlow<Set<String>>(emptySet())
    val watchedIds: StateFlow<Set<String>> = _watchedIds

    private var loaded = false

    fun loadWatchlistOnce() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            repository.getWatchlistedListingIds()
                .onSuccess { ids -> _watchedIds.value = ids.toSet() }
        }
    }

    fun isWatched(listingId: String): Boolean = _watchedIds.value.contains(listingId)

    // onResult lets a caller (e.g. ProductDetailScreen) mirror its own
    // optimistic-then-rollback treatment for a locally-displayed watchCount,
    // since this ViewModel only tracks watched ids, not listing data.
    fun toggleWatch(listingId: String, onResult: (success: Boolean) -> Unit = {}) {
        val currentlyWatched = isWatched(listingId)
        // Update instantly so the heart icon feels responsive, then sync to
        // Firestore in the background \u2014 and roll back only if that fails.
        _watchedIds.value = if (currentlyWatched) {
            _watchedIds.value - listingId
        } else {
            _watchedIds.value + listingId
        }
        viewModelScope.launch {
            val result = if (currentlyWatched) {
                repository.removeFromWatchlist(listingId)
            } else {
                repository.addToWatchlist(listingId)
            }
            result.onFailure {
                // Roll back the optimistic update if the save actually failed.
                _watchedIds.value = if (currentlyWatched) {
                    _watchedIds.value + listingId
                } else {
                    _watchedIds.value - listingId
                }
            }
            onResult(result.isSuccess)
        }
    }
}
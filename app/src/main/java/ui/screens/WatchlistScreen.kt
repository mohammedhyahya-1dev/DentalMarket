package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.model.Listing
import com.dentalmarket.app.ui.components.ProductCard
import com.dentalmarket.app.viewmodel.WatchlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onProductClick: (String) -> Unit,
    onBack: () -> Unit,
    watchlistViewModel: WatchlistViewModel = viewModel()
) {
    val watchedIds by watchlistViewModel.watchedIds.collectAsState()
    val repository = remember { ListingRepository() }
    var watchedListings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(watchedIds) {
        isLoading = true
        // Fetch each watched listing individually \u2014 simplest approach given
        // Firestore has no "get many by ID list" query, and watchlists are
        // typically small (a handful of items), so this stays fast.
        watchedListings = watchedIds.mapNotNull { id ->
            repository.getListingById(id).getOrNull()
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        watchlistViewModel.loadWatchlistOnce()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Watchlist", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                watchedListings.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No saved items yet \u2014 tap the heart on any listing to save it here",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(watchedListings, key = { it.id }) { listing ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductCard(
                                listing = listing,
                                onClick = { onProductClick(listing.id) },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { watchlistViewModel.toggleWatch(listing.id) }) {
                                Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = "Remove from watchlist",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
package com.dentalmarket.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.ui.components.GuestSignInPrompt
import com.dentalmarket.app.ui.components.ProductCard
import com.dentalmarket.app.ui.components.RatingBadge
import com.dentalmarket.app.model.Listing
import com.dentalmarket.app.viewmodel.FollowViewModel
import com.dentalmarket.app.viewmodel.RatingViewModel
import com.dentalmarket.app.viewmodel.SearchViewModel
import com.dentalmarket.app.viewmodel.filterAndSortListings

// A seller's own shop page — reached by tapping "Sold by X" on Product
// Detail. Unlike SearchResultsScreen this has a fixed base set (one seller's
// active listings, fetched once) rather than a marketplace-wide list plus a
// keyword search box: same Category/Brand/Condition/Price/Delivery-method/
// Free-shipping FilterSheet and filterAndSortListings() pure function,
// just scoped down and with no query field or CategoriesRow entry point.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileScreen(
    sellerId: String,
    sellerName: String,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onRequireLogin: () -> Unit,
    searchViewModel: SearchViewModel = viewModel(),
    ratingViewModel: RatingViewModel = viewModel(),
    followViewModel: FollowViewModel = viewModel()
) {
    val context = LocalContext.current
    val listingRepository = remember { ListingRepository() }
    val authRepository = remember { AuthRepository() }
    val isGuest = authRepository.isAnonymous
    var showGuestPrompt by remember { mutableStateOf(false) }

    fun requireLogin(action: () -> Unit) {
        if (isGuest) showGuestPrompt = true else action()
    }

    var allListings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // The one cross-user profile read in the app — see
    // AuthRepository.getPublicUsername and publicProfiles/{uid} in
    // firestore.rules. Null while loading or if this seller has none yet.
    var username by remember { mutableStateOf<String?>(null) }
    // sellerName arrives blank only when this screen was opened via a
    // shared deep link (MainActivity's path-based handler has no name to
    // pass, unlike ProductDetailScreen's "Sold by X" tap) — falls back to
    // publicProfiles/{uid}.name in that case only; the normal in-app path
    // never touches this and keeps showing sellerName exactly as before.
    var displayName by remember { mutableStateOf(sellerName) }

    LaunchedEffect(sellerId) {
        if (sellerName.isBlank()) {
            authRepository.getPublicName(sellerId).onSuccess { name ->
                if (!name.isNullOrBlank()) displayName = name
            }
        }
        listingRepository.getListingsBySeller(sellerId)
            .onSuccess { allListings = it }
        isLoading = false
        ratingViewModel.loadSellerSummaries(listOf(sellerId))
        followViewModel.load(sellerId)
        authRepository.getPublicUsername(sellerId).onSuccess { username = it }
    }

    // Same status filter MarketplaceScreen/SearchResultsScreen apply via
    // ListingRepository.getAllListings() — a shop page shows what's for
    // sale, sold items are represented by the Sold count only.
    val activeListings = remember(allListings) { allListings.filter { it.status != "SOLD" } }
    val soldCount = remember(allListings) { allListings.count { it.status == "SOLD" } }

    val availableBrands = remember(activeListings) {
        activeListings.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableLocations = remember(activeListings) {
        activeListings.map { it.sellerProvince }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val results = remember(
        activeListings,
        searchViewModel.sortOption.value,
        searchViewModel.minPrice.value,
        searchViewModel.maxPrice.value,
        searchViewModel.selectedCategories.value,
        searchViewModel.selectedSubcategories.value,
        searchViewModel.selectedConditions.value,
        searchViewModel.selectedDeliveryMethods.value,
        searchViewModel.freeShippingOnly.value,
        searchViewModel.selectedBrands.value,
        searchViewModel.selectedLocations.value
    ) {
        filterAndSortListings(
            listings = activeListings,
            query = "",
            sortOption = searchViewModel.sortOption.value,
            minPrice = searchViewModel.minPrice.value.toDoubleOrNull(),
            maxPrice = searchViewModel.maxPrice.value.toDoubleOrNull(),
            categories = searchViewModel.selectedCategories.value,
            subcategories = searchViewModel.selectedSubcategories.value,
            conditions = searchViewModel.selectedConditions.value,
            deliveryMethods = searchViewModel.selectedDeliveryMethods.value,
            freeShippingOnly = searchViewModel.freeShippingOnly.value,
            brands = searchViewModel.selectedBrands.value,
            locations = searchViewModel.selectedLocations.value
        )
    }

    var showFilters by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sellerSummaries by ratingViewModel.sellerSummaries.collectAsState()
    val isFollowing by followViewModel.isFollowing.collectAsState()
    val followerCount by followViewModel.followerCount.collectAsState()
    val followingCount by followViewModel.followingCount.collectAsState()
    // A seller can't follow themselves — firestore.rules rejects the write
    // too, this just keeps the button from ever appearing on your own shop.
    val isOwnProfile = !isGuest && authRepository.currentUserId == sellerId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Shown on every profile, including your own (share your
                    // own shop) — unlike Follow, there's no reason to hide
                    // this on isOwnProfile. Straight to the system
                    // Sharesheet on tap — no intermediate screen.
                    IconButton(onClick = {
                        val shareText = "Check out ${displayName.ifBlank { "this seller" }}'s shop on DentalMarket: " +
                            sellerShareLink(sellerId)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share this shop"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share this seller")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // No photo field exists on DentalUser or Listing — same
                // placeholder avatar ProfileScreen uses for "my own" profile.
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(60.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(displayName, style = MaterialTheme.typography.headlineSmall)

                username?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        "@$it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                sellerSummaries[sellerId]?.let { summary ->
                    Spacer(modifier = Modifier.height(4.dp))
                    RatingBadge(summary)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatColumn(followerCount.toString(), "Followers")
                    StatColumn(followingCount.toString(), "Following")
                    StatColumn(soldCount.toString(), "Sold")
                }

                if (!isOwnProfile) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = { requireLogin { followViewModel.toggleFollow(sellerId) } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Following")
                        }
                    } else {
                        Button(
                            onClick = { requireLogin { followViewModel.toggleFollow(sellerId) } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Follow")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    OutlinedButton(onClick = { showSortMenu = true }) {
                        Text(searchViewModel.sortOption.value.label)
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        com.dentalmarket.app.viewmodel.SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    searchViewModel.sortOption.value = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedButton(onClick = { showFilters = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filters")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                results.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (activeListings.isEmpty()) "No active listings" else "No listings match these filters",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(results, key = { it.id }) { listing ->
                        ProductCard(
                            listing = listing,
                            onClick = { onProductClick(listing.id) }
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            searchViewModel = searchViewModel,
            availableBrands = availableBrands,
            availableLocations = availableLocations,
            onDismiss = { showFilters = false }
        )
    }

    if (showGuestPrompt) {
        GuestSignInPrompt(
            onDismiss = { showGuestPrompt = false },
            onSignIn = {
                showGuestPrompt = false
                onRequireLogin()
            }
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

// https://dentalmarket-abdf6.firebaseapp.com/seller/{uid} — same verified
// host as the existing password-reset App Link (see AndroidManifest.xml),
// new pathPrefix. Opens straight into this seller's SellerProfileScreen via
// MainActivity's deep-link handling if the app is installed; otherwise
// Firebase Hosting serves public/seller.html (see firebase.json's rewrite).
// Keyed by uid, not @username — matches the seller/{sellerId} route exactly,
// and can never break if the seller renames their handle later.
private fun sellerShareLink(sellerId: String): String =
    "https://dentalmarket-abdf6.firebaseapp.com/seller/$sellerId"

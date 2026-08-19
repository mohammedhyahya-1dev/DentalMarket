package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.model.Condition
import com.dentalmarket.app.model.DeliveryMethod
import com.dentalmarket.app.ui.components.CategoriesRow
import com.dentalmarket.app.ui.components.GuestSignInPrompt
import com.dentalmarket.app.ui.components.ProductCard
import com.dentalmarket.app.viewmodel.CartViewModel
import com.dentalmarket.app.viewmodel.MarketplaceViewModel
import com.dentalmarket.app.viewmodel.RatingViewModel
import com.dentalmarket.app.viewmodel.SearchViewModel
import com.dentalmarket.app.viewmodel.SortOption
import com.dentalmarket.app.viewmodel.WatchlistViewModel
import com.dentalmarket.app.viewmodel.filterAndSortListings

// A separate, additive destination from MarketplaceScreen's own lightweight
// inline search/category filter (which is left exactly as it is) — reached
// only by submitting the Home search field. Loads listings independently
// via its own MarketplaceViewModel instance (same "every screen gets its
// own ViewModel" pattern already used across the app), then filters/sorts
// them client-side with the pure filterAndSortListings() function, mirroring
// how Home's own category chips already filter the same in-memory list.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    initialQuery: String,
    cartViewModel: CartViewModel,
    onProductClick: (String) -> Unit,
    onBack: () -> Unit,
    onCategoriesClick: () -> Unit,
    onRequireLogin: () -> Unit,
    marketplaceViewModel: MarketplaceViewModel = viewModel(),
    watchlistViewModel: WatchlistViewModel = viewModel(),
    ratingViewModel: RatingViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    val watchedIds by watchlistViewModel.watchedIds.collectAsState()
    val sellerSummaries by ratingViewModel.sellerSummaries.collectAsState()
    val listings = marketplaceViewModel.listings.value
    val isLoading = marketplaceViewModel.isLoading.value

    LaunchedEffect(Unit) {
        marketplaceViewModel.loadListings()
        watchlistViewModel.loadWatchlistOnce()
        if (searchViewModel.query.value.isBlank()) {
            searchViewModel.query.value = initialQuery
        }
    }

    LaunchedEffect(listings) {
        if (listings.isNotEmpty()) {
            ratingViewModel.loadSellerSummaries(listings.map { it.sellerId })
        }
    }

    val authRepository = remember { AuthRepository() }
    val isGuest = authRepository.isAnonymous
    var showGuestPrompt by remember { mutableStateOf(false) }

    fun requireLogin(action: () -> Unit) {
        if (isGuest) showGuestPrompt = true else action()
    }

    var showFilters by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val availableCategories = remember(listings) {
        listings.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableBrands = remember(listings) {
        listings.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableLocations = remember(listings) {
        listings.map { it.sellerProvince }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val results = remember(
        listings,
        searchViewModel.query.value,
        searchViewModel.sortOption.value,
        searchViewModel.minPrice.value,
        searchViewModel.maxPrice.value,
        searchViewModel.selectedCategories.value,
        searchViewModel.selectedConditions.value,
        searchViewModel.selectedDeliveryMethods.value,
        searchViewModel.freeShippingOnly.value,
        searchViewModel.selectedBrands.value,
        searchViewModel.selectedLocations.value
    ) {
        filterAndSortListings(
            listings = listings,
            query = searchViewModel.query.value,
            sortOption = searchViewModel.sortOption.value,
            minPrice = searchViewModel.minPrice.value.toDoubleOrNull(),
            maxPrice = searchViewModel.maxPrice.value.toDoubleOrNull(),
            categories = searchViewModel.selectedCategories.value,
            conditions = searchViewModel.selectedConditions.value,
            deliveryMethods = searchViewModel.selectedDeliveryMethods.value,
            freeShippingOnly = searchViewModel.freeShippingOnly.value,
            brands = searchViewModel.selectedBrands.value,
            locations = searchViewModel.selectedLocations.value
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchViewModel.query.value,
                onValueChange = { searchViewModel.query.value = it },
                label = { Text("Search devices") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            CategoriesRow(onClick = onCategoriesClick)

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
                        SortOption.entries.forEach { option ->
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

            Text(
                "${results.size} result" + if (results.size == 1) "" else "s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                results.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No devices match your search", style = MaterialTheme.typography.titleMedium)
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
                            onClick = { onProductClick(listing.id) },
                            onAddToCart = { requireLogin { cartViewModel.addToCart(listing) } },
                            isWatched = watchedIds.contains(listing.id),
                            onToggleWatch = { requireLogin { watchlistViewModel.toggleWatch(listing.id) } },
                            sellerRating = sellerSummaries[listing.sellerId]
                        )
                    }
                }
            }
        }
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

    if (showFilters) {
        FilterSheet(
            searchViewModel = searchViewModel,
            availableCategories = availableCategories,
            availableBrands = availableBrands,
            availableLocations = availableLocations,
            onDismiss = { showFilters = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    searchViewModel: SearchViewModel,
    availableCategories: List<String>,
    availableBrands: List<String>,
    availableLocations: List<String>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filters", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { searchViewModel.clearFilters() }) {
                    Text("Clear all")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Price range", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchViewModel.minPrice.value,
                    onValueChange = { searchViewModel.minPrice.value = it },
                    label = { Text("Min $") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = searchViewModel.maxPrice.value,
                    onValueChange = { searchViewModel.maxPrice.value = it },
                    label = { Text("Max $") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (availableCategories.isNotEmpty()) {
                Text("Category", style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                ) {
                    items(availableCategories) { category ->
                        FilterChip(
                            selected = searchViewModel.selectedCategories.value.contains(category),
                            onClick = { searchViewModel.toggleCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }
            }

            Text("Condition", style = MaterialTheme.typography.titleMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                items(Condition.entries) { condition ->
                    FilterChip(
                        selected = searchViewModel.selectedConditions.value.contains(condition.name),
                        onClick = { searchViewModel.toggleCondition(condition.name) },
                        label = { Text(condition.label) }
                    )
                }
            }

            Text("Delivery method", style = MaterialTheme.typography.titleMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            ) {
                items(DeliveryMethod.entries) { method ->
                    FilterChip(
                        selected = searchViewModel.selectedDeliveryMethods.value.contains(method.name),
                        onClick = { searchViewModel.toggleDeliveryMethod(method.name) },
                        label = { Text(method.label) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Checkbox(
                    checked = searchViewModel.freeShippingOnly.value,
                    onCheckedChange = { searchViewModel.freeShippingOnly.value = it }
                )
                Text("Free shipping only", style = MaterialTheme.typography.bodyMedium)
            }

            if (availableBrands.isNotEmpty()) {
                Text("Brand", style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                ) {
                    items(availableBrands) { brand ->
                        FilterChip(
                            selected = searchViewModel.selectedBrands.value.contains(brand),
                            onClick = { searchViewModel.toggleBrand(brand) },
                            label = { Text(brand) }
                        )
                    }
                }
            }

            if (availableLocations.isNotEmpty()) {
                Text("Seller location", style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                ) {
                    items(availableLocations) { location ->
                        FilterChip(
                            selected = searchViewModel.selectedLocations.value.contains(location),
                            onClick = { searchViewModel.toggleLocation(location) },
                            label = { Text(location) }
                        )
                    }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Show results")
            }
        }
    }
}

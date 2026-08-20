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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.model.Condition
import com.dentalmarket.app.model.DeliveryMethod
import com.dentalmarket.app.model.DeviceCategory
import com.dentalmarket.app.ui.components.CategoriesRow
import com.dentalmarket.app.ui.components.ProductCard
import com.dentalmarket.app.viewmodel.MarketplaceViewModel
import com.dentalmarket.app.viewmodel.SearchViewModel
import com.dentalmarket.app.viewmodel.SortOption
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
    initialCategory: String = "",
    initialSubcategory: String = "",
    onProductClick: (String) -> Unit,
    onBack: () -> Unit,
    onCategoriesClick: () -> Unit,
    marketplaceViewModel: MarketplaceViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    val listings = marketplaceViewModel.listings.value
    val isLoading = marketplaceViewModel.isLoading.value

    LaunchedEffect(Unit) {
        marketplaceViewModel.loadListings()
        if (searchViewModel.query.value.isBlank() && initialQuery.isNotBlank()) {
            searchViewModel.query.value = initialQuery
        }
        if (searchViewModel.selectedCategories.value.isEmpty() && initialCategory.isNotBlank()) {
            searchViewModel.selectedCategories.value = setOf(initialCategory)
        }
        if (searchViewModel.selectedSubcategories.value.isEmpty() && initialSubcategory.isNotBlank()) {
            searchViewModel.selectedSubcategories.value = setOf(initialSubcategory)
        }
    }

    var showFilters by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    var isSearchFocused by remember { mutableStateOf(false) }
    // Tapping a suggestion fills the field but shouldn't immediately reopen
    // the same dropdown underneath it; typing anything afterward clears
    // this so suggestions can resume.
    var suggestionsDismissed by remember { mutableStateOf(false) }

    val suggestions = remember(listings, searchViewModel.query.value) {
        val q = searchViewModel.query.value
        if (q.isBlank()) {
            emptyList()
        } else {
            listings
                .map { it.name }
                .filter { it.isNotBlank() && it.contains(q, ignoreCase = true) }
                .distinct()
                .take(6)
        }
    }

    val availableBrands = remember(listings) {
        listings.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableLocations = remember(listings) {
        listings.map { it.sellerProvince }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Query text and category drill-down are the two ways to arrive at a
    // result set; the other FilterSheet facets are refinements applied on
    // top of one of those, never a standalone entry point on their own.
    val hasActiveSearch = searchViewModel.query.value.isNotBlank() ||
        searchViewModel.selectedCategories.value.isNotEmpty() ||
        searchViewModel.selectedSubcategories.value.isNotEmpty()

    val results = remember(
        listings,
        searchViewModel.query.value,
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
            listings = listings,
            query = searchViewModel.query.value,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchViewModel.query.value,
                    onValueChange = {
                        searchViewModel.query.value = it
                        suggestionsDismissed = false
                    },
                    label = { Text("Search devices") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isSearchFocused = it.isFocused }
                )
                DropdownMenu(
                    expanded = isSearchFocused && !suggestionsDismissed && suggestions.isNotEmpty(),
                    onDismissRequest = {},
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    suggestions.forEach { title ->
                        DropdownMenuItem(
                            text = { Text(title) },
                            onClick = {
                                searchViewModel.query.value = title
                                suggestionsDismissed = true
                            }
                        )
                    }
                }
            }

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

            if (hasActiveSearch) {
                Text(
                    "${results.size} result" + if (results.size == 1) "" else "s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            when {
                !hasActiveSearch -> EmptySearchState()
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
}

// Shown before the buyer has typed anything — filterAndSortListings() is
// never even asked to run against a blank query (see the `when` branch
// order above), so this is a distinct "nothing searched yet" state, not a
// "0 results" state.
@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("What are you looking for?", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Search by device name, category, or brand.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    searchViewModel: SearchViewModel,
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

            Text("Category", style = MaterialTheme.typography.titleMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            ) {
                items(DeviceCategory.entries) { cat ->
                    FilterChip(
                        selected = searchViewModel.selectedCategories.value.contains(cat.label),
                        onClick = { searchViewModel.toggleCategory(cat.label) },
                        label = { Text("${cat.emoji} ${cat.label}") }
                    )
                }
            }

            // Only meaningful (and only shown) once exactly one category is
            // selected — each category has its own distinct subcategory
            // list, and SearchViewModel.toggleCategory() already clears any
            // stale subcategory selection whenever this set changes.
            val soleSelectedCategory = searchViewModel.selectedCategories.value
                .singleOrNull()
                ?.let { label -> DeviceCategory.entries.find { it.label == label } }
            if (soleSelectedCategory != null && soleSelectedCategory.subcategories.isNotEmpty()) {
                Text("Subcategory", style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                ) {
                    items(soleSelectedCategory.subcategories) { sub ->
                        FilterChip(
                            selected = searchViewModel.selectedSubcategories.value.contains(sub),
                            onClick = { searchViewModel.toggleSubcategory(sub) },
                            label = { Text(sub) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
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

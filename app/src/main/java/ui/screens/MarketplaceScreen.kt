package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.ui.components.BottomNavBar
import com.dentalmarket.app.ui.components.BottomNavTab
import com.dentalmarket.app.ui.components.GuestSignInPrompt
import com.dentalmarket.app.ui.components.ProductCard
import com.dentalmarket.app.viewmodel.CartViewModel
import com.dentalmarket.app.viewmodel.MarketplaceViewModel
import com.dentalmarket.app.viewmodel.NotificationViewModel
import com.dentalmarket.app.viewmodel.RatingViewModel
import com.dentalmarket.app.viewmodel.WatchlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    cartViewModel: CartViewModel,
    onProductClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSellClick: () -> Unit,
    onMyOrdersClick: () -> Unit,
    onAdminOrdersClick: () -> Unit,
    onMyListingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSellerOrdersClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onRequireLogin: () -> Unit,
    preselectedCategory: String? = null,
    marketplaceViewModel: MarketplaceViewModel = viewModel(),
    watchlistViewModel: WatchlistViewModel = viewModel(),
    ratingViewModel: RatingViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val watchedIds by watchlistViewModel.watchedIds.collectAsState()
    val sellerSummaries by ratingViewModel.sellerSummaries.collectAsState()
    val unreadNotifications = notificationViewModel.unreadCount
    LaunchedEffect(Unit) {
        marketplaceViewModel.loadListings()
        watchlistViewModel.loadWatchlistOnce()
        notificationViewModel.loadNotifications()
    }
    val itemCount = cartItems.sumOf { it.quantity }
    val listings = marketplaceViewModel.listings.value
    val isLoading = marketplaceViewModel.isLoading.value

    LaunchedEffect(listings) {
        if (listings.isNotEmpty()) {
            ratingViewModel.loadSellerSummaries(listings.map { it.sellerId })
        }
    }
    val authRepository = remember { AuthRepository() }
    val isAdmin = authRepository.isAdmin
    val isGuest = authRepository.isAnonymous
    var showGuestPrompt by remember { mutableStateOf(false) }

    fun requireLogin(action: () -> Unit) {
        if (isGuest) showGuestPrompt = true else action()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(preselectedCategory) }

    val categories = remember(listings) {
        listings.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredListings = remember(listings, searchQuery, selectedCategory) {
        listings.filter { listing ->
            val matchesSearch = searchQuery.isBlank() ||
                    listing.name.contains(searchQuery, ignoreCase = true) ||
                    listing.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null ||
                    listing.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DentalMarket", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onAdminOrdersClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "Admin orders")
                        }
                    }
                    IconButton(onClick = { requireLogin(onNotificationsClick) }, modifier = Modifier.padding(end = 8.dp)) {
                        BadgedBox(badge = {
                            if (unreadNotifications > 0) {
                                Badge { Text("$unreadNotifications") }
                            }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = { requireLogin(onSellerOrdersClick) }, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Filled.Storefront, contentDescription = "My sales")
                    }
                    IconButton(onClick = { requireLogin(onMyListingsClick) }, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "My listings")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = BottomNavTab.HOME,
                onHomeClick = {},
                onCategoriesClick = onCategoriesClick,
                onCartClick = onCartClick,
                onMyOrdersClick = { requireLogin(onMyOrdersClick) },
                onProfileClick = { requireLogin(onProfileClick) },
                cartItemCount = itemCount
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { requireLogin(onSellClick) }) {
                Icon(Icons.Filled.Add, contentDescription = "Sell a device")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search devices") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (categories.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    listings.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No listings yet \u2014 be the first to sell a device!")
                    }
                    filteredListings.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No devices match your search", style = MaterialTheme.typography.titleMedium)
                    }
                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(filteredListings, key = { it.id }) { listing ->
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
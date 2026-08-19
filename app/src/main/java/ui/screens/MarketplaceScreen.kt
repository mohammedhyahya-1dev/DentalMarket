package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.ui.components.BottomNavBar
import com.dentalmarket.app.ui.components.BottomNavTab
import com.dentalmarket.app.ui.components.GuestSignInPrompt
import com.dentalmarket.app.ui.components.NotificationPermissionDialog
import com.dentalmarket.app.ui.components.ProductCard
import com.dentalmarket.app.ui.components.markNotificationPromptShown
import com.dentalmarket.app.ui.components.shouldShowNotificationPrompt
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
    onWatchlistClick: () -> Unit,
    onSearchSubmit: (String) -> Unit,
    onRequireLogin: () -> Unit,
    marketplaceViewModel: MarketplaceViewModel = viewModel(),
    watchlistViewModel: WatchlistViewModel = viewModel(),
    ratingViewModel: RatingViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val watchedIds by watchlistViewModel.watchedIds.collectAsState()
    val sellerSummaries by ratingViewModel.sellerSummaries.collectAsState()
    val unreadNotifications = notificationViewModel.unreadCount

    val context = LocalContext.current
    var showNotificationPrompt by remember { mutableStateOf(false) }
    // shouldShowNotificationPrompt() is once-ever-per-install, backed by a
    // SharedPreferences flag — re-checking on every fresh composition of
    // this screen (e.g. Categories -> Home) is harmless, since it returns
    // false again the moment that flag is set. Used to be a gating nav
    // destination reached from authGate/CompleteProfileScreen before ever
    // landing here; now this screen owns the decision itself.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        markNotificationPromptShown(context)
        showNotificationPrompt = false
    }
    LaunchedEffect(Unit) {
        marketplaceViewModel.loadListings()
        watchlistViewModel.loadWatchlistOnce()
        notificationViewModel.loadNotifications()
        if (shouldShowNotificationPrompt(context)) {
            showNotificationPrompt = true
        }
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
                    IconButton(onClick = onCartClick) {
                        BadgedBox(badge = {
                            if (itemCount > 0) {
                                Badge { Text("$itemCount") }
                            }
                        }) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = BottomNavTab.HOME,
                onHomeClick = {},
                onFavoritesClick = { requireLogin(onWatchlistClick) },
                onSellClick = { requireLogin(onSellClick) },
                onMyOrdersClick = { requireLogin(onMyOrdersClick) },
                onProfileClick = { requireLogin(onProfileClick) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Non-editable — this is a fake entry point you tap, not a real
            // input. The overlay Box below intercepts the tap before it ever
            // reaches the OutlinedTextField, so the field never requests
            // focus and no keyboard flickers open before navigating away.
            // The real, editable, auto-focused search field lives on
            // SearchResultsScreen itself.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Search devices") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = { onSearchSubmit("") })
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    listings.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No listings yet \u2014 be the first to sell a device!")
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
                        items(listings, key = { it.id }) { listing ->
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

    if (showNotificationPrompt) {
        NotificationPermissionDialog(
            onAllow = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    markNotificationPromptShown(context)
                    showNotificationPrompt = false
                }
            },
            onDismiss = {
                markNotificationPromptShown(context)
                showNotificationPrompt = false
            }
        )
    }
}
package com.dentalmarket.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dentalmarket.app.ui.screens.AdminInquiriesScreen
import com.dentalmarket.app.ui.screens.AdminOrdersScreen
import com.dentalmarket.app.ui.screens.CartScreen
import com.dentalmarket.app.ui.screens.CompleteProfileScreen
import com.dentalmarket.app.ui.screens.ListingDetailScreen
import com.dentalmarket.app.ui.screens.LoginScreen
import com.dentalmarket.app.ui.screens.MarketplaceScreen
import com.dentalmarket.app.ui.screens.MyListingsScreen
import com.dentalmarket.app.ui.screens.MyOrdersScreen
import com.dentalmarket.app.ui.screens.OrderDetailScreen
import com.dentalmarket.app.ui.screens.SellerNotificationsScreen
import com.dentalmarket.app.ui.screens.SellerOrdersScreen
import com.dentalmarket.app.ui.screens.MyQuestionsScreen
import com.dentalmarket.app.ui.screens.ProductDetailScreen
import com.dentalmarket.app.ui.screens.ProfileScreen
import com.dentalmarket.app.ui.screens.ResetPasswordScreen
import com.dentalmarket.app.ui.screens.SellScreen
import com.dentalmarket.app.ui.theme.DentalMarketTheme
import com.dentalmarket.app.viewmodel.AuthViewModel
import com.dentalmarket.app.viewmodel.CartViewModel
import com.dentalmarket.app.viewmodel.InquiryViewModel
import com.dentalmarket.app.ui.screens.CategoriesScreen
import com.dentalmarket.app.ui.screens.SearchResultsScreen
import com.dentalmarket.app.ui.screens.WatchlistScreen

class MainActivity : ComponentActivity() {

    private var pendingDeepLinkUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDeepLinkUri = intent?.data
        setContent {
            DentalMarketTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DentalMarketApp(deepLinkUri = pendingDeepLinkUri)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkUri = intent.data
    }
}

@Composable
fun DentalMarketApp(deepLinkUri: Uri? = null) {
    val navController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()
    val inquiryViewModel: InquiryViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    // Always starts at authGate now — a brand-new install has no Firebase
    // session at all, and authGate's own LaunchedEffect below silently
    // creates an anonymous one instead of routing to "login". "login"
    // itself is still reachable, just never as the start destination — see
    // GuestSignInPrompt's onRequireLogin wiring across every gated screen.
    val startDestination = "authGate"

    LaunchedEffect(deepLinkUri) {
        val code = deepLinkUri?.getQueryParameter("oobCode")
        if (!code.isNullOrBlank()) {
            navController.navigate("resetPassword/$code")
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("authGate") {
            LaunchedEffect(Unit) {
                fun proceedAsGuest() {
                    // Guests have no profile doc to check — skip straight to
                    // marketplace. MarketplaceScreen itself now decides
                    // whether to show the notification-permission dialog.
                    navController.navigate("marketplace") {
                        popUpTo("authGate") { inclusive = true }
                    }
                }
                when {
                    !authViewModel.isLoggedIn -> {
                        // No Firebase session at all — brand-new install, or
                        // right after an explicit sign-out. Silently create an
                        // anonymous one instead of showing a login wall.
                        authViewModel.continueAsGuest(
                            onSuccess = { proceedAsGuest() },
                            onFailure = {
                                // No network, or Firebase rejected it — don't
                                // strand the user on an infinite spinner; give
                                // them a manual retry surface instead.
                                navController.navigate("login") {
                                    popUpTo("authGate") { inclusive = true }
                                }
                            }
                        )
                    }
                    authViewModel.isAnonymous -> proceedAsGuest()
                    else -> {
                        authViewModel.checkProfileComplete { complete ->
                            val destination = if (!complete) "completeProfile" else "marketplace"
                            navController.navigate(destination) {
                                popUpTo("authGate") { inclusive = true }
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { sameAccount ->
                    // Only a genuine account switch (fresh login/signup, or a
                    // collision fallback into a different existing account)
                    // should drop the cart — a guest-upgrade link keeps the
                    // same uid, and "browse then sign up to buy" is the
                    // normal flow now, so wiping the cart right at that
                    // moment would cost a sale.
                    if (!sameAccount) {
                        cartViewModel.clearCart()
                    }
                    navController.navigate("authGate") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable(
            "resetPassword/{code}",
            arguments = listOf(navArgument("code") { type = NavType.StringType })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: ""
            ResetPasswordScreen(
                authViewModel = authViewModel,
                code = code,
                onDone = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("completeProfile") {
            CompleteProfileScreen(
                authViewModel = authViewModel,
                onComplete = {
                    // MarketplaceScreen itself now decides whether to show
                    // the notification-permission dialog.
                    navController.navigate("marketplace") {
                        popUpTo("completeProfile") { inclusive = true }
                    }
                },
                onSignOut = {
                    authViewModel.signOut()
                    cartViewModel.clearCart()
                    // authGate silently re-guests (see its LaunchedEffect)
                    // instead of dropping the user on a login wall.
                    navController.navigate("authGate") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("marketplace") {
            MarketplaceScreen(
                cartViewModel = cartViewModel,
                onProductClick = { id -> navController.navigate("product/$id") },
                onCartClick = { navController.navigate("cart") },
                onProfileClick = { navController.navigate("profile") },
                onSellClick = { navController.navigate("sell") },
                onMyOrdersClick = { navController.navigate("myOrders") },
                onAdminOrdersClick = { navController.navigate("adminOrders") },
                onMyListingsClick = { navController.navigate("myListings") },
                onNotificationsClick = { navController.navigate("sellerNotifications") },
                onSellerOrdersClick = { navController.navigate("sellerOrders") },
                onWatchlistClick = { navController.navigate("watchlist") },
                onSearchSubmit = { query -> navController.navigate("searchResults?query=${Uri.encode(query)}") },
                onRequireLogin = { navController.navigate("login") }
            )
        }
        composable(
            "searchResults?query={query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = "" })
        ) { backStackEntry ->
            val initialQuery = backStackEntry.arguments?.getString("query") ?: ""
            SearchResultsScreen(
                initialQuery = initialQuery,
                cartViewModel = cartViewModel,
                onProductClick = { id -> navController.navigate("product/$id") },
                onBack = { navController.popBackStack() },
                onCategoriesClick = { navController.navigate("categories") },
                onRequireLogin = { navController.navigate("login") }
            )
        }
        composable("categories") {
            CategoriesScreen(
                // Home no longer has any way to act on a preselected
                // category (its inline quick-filter chips were removed —
                // that filtering now lives only in SearchResultsScreen's
                // filter sheet), so this just goes back to an unfiltered
                // Home, same as onHomeClick below.
                onCategoryClick = {
                    navController.navigate("marketplace") {
                        popUpTo("marketplace") { inclusive = true }
                    }
                },
                onHomeClick = {
                    navController.navigate("marketplace") {
                        popUpTo("marketplace") { inclusive = true }
                    }
                },
                onFavoritesClick = { navController.navigate("watchlist") },
                onSellClick = { navController.navigate("sell") },
                onMyOrdersClick = { navController.navigate("myOrders") },
                onProfileClick = { navController.navigate("profile") },
                onRequireLogin = { navController.navigate("login") }
            )
        }
        composable("profile") {
            // The bottom-nav Profile tab prompts guests to sign in before
            // ever calling this, but guard the destination itself too —
            // same defense-in-depth as the admin routes below.
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSignOut = {
                        authViewModel.signOut()
                        cartViewModel.clearCart()
                        // authGate silently re-guests (see its LaunchedEffect)
                        // instead of dropping the user on a login wall.
                        navController.navigate("authGate") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onMyQuestionsClick = { navController.navigate("myQuestions") },
                    onAdminInquiriesClick = { navController.navigate("adminInquiries") },
                    onWatchlistClick = { navController.navigate("watchlist") }
                )
            }
        }
        composable("watchlist") {
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                WatchlistScreen(
                    cartViewModel = cartViewModel,
                    onProductClick = { id -> navController.navigate("product/$id") },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("sell") {
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                SellScreen(
                    onPosted = { navController.popBackStack() }
                )
            }
        }
        composable(
            "editListing/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                SellScreen(
                    onPosted = { navController.popBackStack() },
                    listingId = listingId
                )
            }
        }
        composable(
            "product/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                listingId = productId,
                cartViewModel = cartViewModel,
                inquiryViewModel = inquiryViewModel,
                buyerId = authViewModel.currentUserId ?: "",
                buyerName = "",
                onBack = { navController.popBackStack() },
                onRequireLogin = { navController.navigate("login") }
            )
        }
        composable("cart") {
            CartScreen(
                cartViewModel = cartViewModel,
                onBack = { navController.popBackStack() },
                onRequireLogin = { navController.navigate("login") }
            )
        }
        composable("myOrders") {
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                MyOrdersScreen(
                    onBack = { navController.popBackStack() },
                    onOrderClick = { order -> navController.navigate("orderDetail/${order.id}") }
                )
            }
        }
        composable(
            "orderDetail/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailScreen(
                    orderId = orderId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("adminOrders") {
            // The Profile/Marketplace links only show for admins, but that's
            // just UI — guard the destination itself so this screen (and the
            // all-orders Firestore query behind it) can't be reached by
            // navigating here directly, e.g. via a restored back stack.
            if (authViewModel.isAdmin) {
                AdminOrdersScreen(
                    onBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
        composable("myListings") {
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                MyListingsScreen(
                    onBack = { navController.popBackStack() },
                    onEditListing = { id -> navController.navigate("editListing/$id") },
                    onListingClick = { id -> navController.navigate("listingDetail/$id") }
                )
            }
        }
        composable("sellerNotifications") {
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                SellerNotificationsScreen(onBack = { navController.popBackStack() })
            }
        }
        composable("sellerOrders") {
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                SellerOrdersScreen(onBack = { navController.popBackStack() })
            }
        }
        composable(
            "listingDetail/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                ListingDetailScreen(
                    listingId = listingId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("myQuestions") {
            MyQuestionsScreen(
                inquiryViewModel = inquiryViewModel,
                buyerId = authViewModel.currentUserId ?: "",
                onBack = { navController.popBackStack() }
            )
        }
        composable("adminInquiries") {
            if (authViewModel.isAdmin) {
                AdminInquiriesScreen(
                    inquiryViewModel = inquiryViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
    }
}
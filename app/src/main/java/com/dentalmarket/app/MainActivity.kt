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
import com.dentalmarket.app.ui.screens.AdminIdentityVerificationsScreen
import com.dentalmarket.app.ui.screens.AdminInquiriesScreen
import com.dentalmarket.app.ui.screens.AdminOrdersScreen
import com.dentalmarket.app.ui.screens.CartScreen
import com.dentalmarket.app.ui.screens.CompleteProfileScreen
import com.dentalmarket.app.ui.screens.IdentityVerificationScreen
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
import com.dentalmarket.app.ui.screens.SellerProfileScreen
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
                    DentalMarketApp(
                        deepLinkUri = pendingDeepLinkUri,
                        onDeepLinkConsumed = { pendingDeepLinkUri = null }
                    )
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
fun DentalMarketApp(deepLinkUri: Uri? = null, onDeepLinkConsumed: () -> Unit = {}) {
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

    fun resetPasswordRouteFor(uri: Uri): String? {
        // Password-reset links carry oobCode as a query param regardless of
        // path (Firebase's own /__/auth/action link shape) — unrelated to
        // the seller route below, which is path-based instead, since a
        // seller-profile link has no query params of its own to key off.
        val code = uri.getQueryParameter("oobCode")
        return if (!code.isNullOrBlank()) "resetPassword/$code" else null
    }

    fun sellerRouteFor(uri: Uri): String? {
        // Seller-profile share links: https://.../seller/{uid}, same host as
        // the reset-password intent-filter (see AndroidManifest.xml), routed
        // here by MainActivity.onCreate/onNewIntent the same way. sellerName
        // is left blank — this is the one path into SellerProfileScreen that
        // doesn't already have it on hand (unlike ProductDetailScreen's
        // "Sold by" link, which passes it from the listing); the screen's
        // own getPublicUsername-style live read isn't a name lookup today,
        // so the TopAppBar title is blank until the seller's own
        // listings/rating data loads in — a known, minor rough edge.
        val segments = uri.pathSegments.orEmpty()
        val sellerIndex = segments.indexOf("seller")
        val sellerId = segments.getOrNull(sellerIndex + 1)
        return if (sellerIndex >= 0 && !sellerId.isNullOrBlank()) "seller/$sellerId" else null
    }

    // Handles a deep link that arrives once the app is already past authGate
    // (a live app on marketplace/product/etc. receiving a new intent via
    // onNewIntent). The cold-start deep link is instead resolved by authGate
    // itself below — if we acted on it here too, this effect's plain
    // navigate() and authGate's navigate(...){ popUpTo("authGate", inclusive
    // = true) } would race, with authGate's popUpTo capable of popping
    // whatever this effect just pushed. Skipping while authGate is still the
    // current destination avoids that; the rare case of a second deep link
    // arriving in the sub-second window while authGate is still resolving
    // the first one is a known, minor edge we don't handle.
    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri == null || navController.currentDestination?.route == "authGate") return@LaunchedEffect
        val target = resetPasswordRouteFor(deepLinkUri) ?: sellerRouteFor(deepLinkUri) ?: return@LaunchedEffect
        onDeepLinkConsumed()
        navController.navigate(target)
    }

    // Single definition of "show me this category", shared by CategoriesScreen's
    // leaf taps and ProductDetailScreen's category links so both land on the
    // exact same pre-filtered SearchResultsScreen.
    fun navigateToCategory(categoryLabel: String, subcategory: String?) {
        val subcategoryArg = subcategory?.takeIf { it.isNotBlank() }
            ?.let { "&subcategory=${Uri.encode(it)}" } ?: ""
        navController.navigate("searchResults?category=${Uri.encode(categoryLabel)}$subcategoryArg")
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("authGate") {
            LaunchedEffect(Unit) {
                // authGate is the sole owner of the cold-start deep link: it
                // reads deepLinkUri exactly once here and immediately marks it
                // consumed, so a later re-entry into authGate (sign-out,
                // login-switch, both of which popUpTo(0) back here) never
                // replays a stale link. Resolving the target now — before
                // touching auth at all — also means the eventual
                // navigate(...) { popUpTo("authGate", inclusive = true) }
                // below is the ONLY call that can leave authGate, so it can
                // no longer race the deep link out from under itself.
                val resetPasswordRoute = deepLinkUri?.let(::resetPasswordRouteFor)
                val sellerRoute = deepLinkUri?.let(::sellerRouteFor)
                onDeepLinkConsumed()

                // Password reset doesn't need a resolved session, so it
                // short-circuits ahead of the guest sign-in/profile-complete
                // checks below rather than waiting on a network round trip it
                // doesn't need.
                if (resetPasswordRoute != null) {
                    navController.navigate(resetPasswordRoute) {
                        popUpTo("authGate") { inclusive = true }
                    }
                    return@LaunchedEffect
                }

                fun proceedAsGuest() {
                    // Guests have no profile doc to check — skip straight to
                    // the seller deep link if there is one, else marketplace.
                    // MarketplaceScreen itself now decides whether to show
                    // the notification-permission dialog.
                    navController.navigate(sellerRoute ?: "marketplace") {
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
                        // Fire-and-forget — backfills a username for any
                        // pre-existing account that doesn't have one yet,
                        // without delaying the navigation below.
                        authViewModel.ensureUsername()
                        authViewModel.checkProfileComplete { complete ->
                            // An incomplete profile forces onboarding first —
                            // the seller deep link is dropped in that case
                            // rather than resumed after completeProfile, same
                            // as it was already dropped in the old, racy code.
                            val destination = if (!complete) "completeProfile" else (sellerRoute ?: "marketplace")
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
                    // One optional stop before marketplace now — see
                    // "identityVerification?fromOnboarding={fromOnboarding}"
                    // below, which is what actually navigates to
                    // marketplace once Skip or Submit is pressed there.
                    navController.navigate("identityVerification?fromOnboarding=true") {
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
        composable(
            "identityVerification?fromOnboarding={fromOnboarding}",
            arguments = listOf(
                navArgument("fromOnboarding") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val fromOnboarding = backStackEntry.arguments?.getBoolean("fromOnboarding") ?: false
                IdentityVerificationScreen(
                    showSkip = fromOnboarding,
                    onDone = {
                        if (fromOnboarding) {
                            // Same "start completely fresh" idiom onSignOut
                            // uses elsewhere in this file — the whole
                            // onboarding stack (authGate, completeProfile,
                            // this screen) should never be back-navigable
                            // into once onboarding is done.
                            navController.navigate("marketplace") {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
        composable(
            "adminIdentityVerifications?highlightUid={highlightUid}",
            arguments = listOf(
                navArgument("highlightUid") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            if (authViewModel.isAdmin) {
                AdminIdentityVerificationsScreen(
                    onBack = { navController.popBackStack() },
                    highlightUid = backStackEntry.arguments?.getString("highlightUid")
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
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
            "searchResults?query={query}&category={category}&subcategory={subcategory}",
            arguments = listOf(
                navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("subcategory") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) { backStackEntry ->
            val initialQuery = backStackEntry.arguments?.getString("query") ?: ""
            val initialCategory = backStackEntry.arguments?.getString("category") ?: ""
            val initialSubcategory = backStackEntry.arguments?.getString("subcategory") ?: ""
            SearchResultsScreen(
                initialQuery = initialQuery,
                initialCategory = initialCategory,
                initialSubcategory = initialSubcategory,
                onProductClick = { id -> navController.navigate("product/$id") },
                onBack = { navController.popBackStack() },
                onCategoriesClick = { navController.navigate("categories") }
            )
        }
        composable("categories") {
            CategoriesScreen(
                onLeafSelected = { category, subcategory ->
                    navigateToCategory(category.label, subcategory)
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
                    onWatchlistClick = { navController.navigate("watchlist") },
                    onVerifyIdentityClick = { navController.navigate("identityVerification") },
                    onAdminIdentityVerificationsClick = { navController.navigate("adminIdentityVerifications") }
                )
            }
        }
        composable("watchlist") {
            if (authViewModel.isAnonymous) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                WatchlistScreen(
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
                onRequireLogin = { navController.navigate("login") },
                onCategoryClick = { category, subcategory ->
                    navigateToCategory(category, subcategory)
                },
                onSellClick = { navController.navigate("sell") },
                onSellerClick = { sellerId, sellerName ->
                    navController.navigate("seller/$sellerId?sellerName=${Uri.encode(sellerName)}")
                }
            )
        }
        composable(
            "seller/{sellerId}?sellerName={sellerName}",
            arguments = listOf(
                navArgument("sellerId") { type = NavType.StringType },
                navArgument("sellerName") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            val sellerName = backStackEntry.arguments?.getString("sellerName") ?: ""
            SellerProfileScreen(
                sellerId = sellerId,
                sellerName = sellerName,
                onBack = { navController.popBackStack() },
                onProductClick = { id -> navController.navigate("product/$id") },
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
                SellerNotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOrderClick = { orderId -> navController.navigate("orderDetail/$orderId") },
                    onVerificationSubmittedClick = { uid ->
                        navController.navigate("adminIdentityVerifications?highlightUid=$uid")
                    }
                )
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
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
import com.dentalmarket.app.ui.screens.LoginScreen
import com.dentalmarket.app.ui.screens.MarketplaceScreen
import com.dentalmarket.app.ui.screens.MyListingsScreen
import com.dentalmarket.app.ui.screens.MyOrdersScreen
import com.dentalmarket.app.ui.screens.MyQuestionsScreen
import com.dentalmarket.app.ui.screens.ProductDetailScreen
import com.dentalmarket.app.ui.screens.ProfileScreen
import com.dentalmarket.app.ui.screens.ResetPasswordScreen
import com.dentalmarket.app.ui.screens.SellScreen
import com.dentalmarket.app.ui.theme.DentalMarketTheme
import com.dentalmarket.app.viewmodel.AuthViewModel
import com.dentalmarket.app.viewmodel.CartViewModel
import com.dentalmarket.app.viewmodel.InquiryViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.dentalmarket.app.ui.screens.NotificationPermissionScreen
class MainActivity : ComponentActivity() {

    // Holds whichever link opened (or re-opened) the app, if any. Compose
    // reads this below and reacts whenever it changes.
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

    // Fires if the app is already open in the background when the link is tapped.
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

    val startDestination = if (authViewModel.isLoggedIn) "authGate" else "login"

    // Whenever a reset-password link comes in, pull Firebase's code out of it
    // and jump straight to the reset screen, skipping normal login entirely.
    LaunchedEffect(deepLinkUri) {
        val code = deepLinkUri?.getQueryParameter("oobCode")
        if (!code.isNullOrBlank()) {
            navController.navigate("resetPassword/$code")
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("authGate") {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                authViewModel.checkProfileComplete { complete ->
                    val destination = when {
                        !complete -> "completeProfile"
                        shouldShowNotificationPrompt(context) -> "notificationPermission"
                        else -> "marketplace"
                    }
                    navController.navigate(destination) {
                        popUpTo("authGate") { inclusive = true }
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
                onLoginSuccess = {
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
            val context = LocalContext.current
            CompleteProfileScreen(
                authViewModel = authViewModel,
                onComplete = {
                    val destination = if (shouldShowNotificationPrompt(context)) "notificationPermission" else "marketplace"
                    navController.navigate(destination) {
                        popUpTo("completeProfile") { inclusive = true }
                    }
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("notificationPermission") {
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                markNotificationPromptShown(context)
                navController.navigate("marketplace") {
                    popUpTo("notificationPermission") { inclusive = true }
                }
            }
            NotificationPermissionScreen(
                onAllow = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        markNotificationPromptShown(context)
                        navController.navigate("marketplace") {
                            popUpTo("notificationPermission") { inclusive = true }
                        }
                    }
                },
                onSkip = {
                    markNotificationPromptShown(context)
                    navController.navigate("marketplace") {
                        popUpTo("notificationPermission") { inclusive = true }
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
                onMyListingsClick = { navController.navigate("myListings") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onMyQuestionsClick = { navController.navigate("myQuestions") },
                onAdminInquiriesClick = { navController.navigate("adminInquiries") }
            )
        }
        composable("sell") {
            SellScreen(
                onPosted = { navController.popBackStack() }
            )
        }
        composable(
            "editListing/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            SellScreen(
                onPosted = { navController.popBackStack() },
                listingId = listingId
            )
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
                onBack = { navController.popBackStack() }
            )
        }
        composable("cart") {
            CartScreen(
                cartViewModel = cartViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("myOrders") {
            MyOrdersScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("adminOrders") {
            AdminOrdersScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("myListings") {
            MyListingsScreen(
                onBack = { navController.popBackStack() },
                onEditListing = { id -> navController.navigate("editListing/$id") }
            )
        }
        composable("myQuestions") {
            MyQuestionsScreen(
                inquiryViewModel = inquiryViewModel,
                buyerId = authViewModel.currentUserId ?: "",
                onBack = { navController.popBackStack() }
            )
        }
        composable("adminInquiries") {
            AdminInquiriesScreen(
                inquiryViewModel = inquiryViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun shouldShowNotificationPrompt(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return false
    }
    val prefs = context.getSharedPreferences("dentalmarket_prefs", android.content.Context.MODE_PRIVATE)
    return !prefs.getBoolean("notification_prompt_shown", false)
}

private fun markNotificationPromptShown(context: android.content.Context) {
    val prefs = context.getSharedPreferences("dentalmarket_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putBoolean("notification_prompt_shown", true).apply()
}
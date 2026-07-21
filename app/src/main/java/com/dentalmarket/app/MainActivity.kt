package com.dentalmarket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.dentalmarket.app.ui.screens.SellScreen
import com.dentalmarket.app.ui.screens.SignUpScreen
import com.dentalmarket.app.ui.theme.DentalMarketTheme
import com.dentalmarket.app.viewmodel.AuthViewModel
import com.dentalmarket.app.viewmodel.CartViewModel
import com.dentalmarket.app.viewmodel.InquiryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DentalMarketTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DentalMarketApp()
                }
            }
        }
    }
}

@Composable
fun DentalMarketApp() {
    val navController = rememberNavController()
    val cartViewModel: CartViewModel = viewModel()
    val inquiryViewModel: InquiryViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()

    // Every successful login/signup routes here first — it silently checks
    // whether the dentist has finished their profile, then sends them to the
    // right place. Keeps that check in one spot instead of three.
    val startDestination = if (authViewModel.isLoggedIn) "authGate" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("authGate") {
            LaunchedEffect(Unit) {
                authViewModel.checkProfileComplete { complete ->
                    val destination = if (complete) "marketplace" else "completeProfile"
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
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }
        composable("signup") {
            SignUpScreen(
                authViewModel = authViewModel,
                onSignUpSuccess = {
                    navController.navigate("authGate") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable("completeProfile") {
            CompleteProfileScreen(
                authViewModel = authViewModel,
                onComplete = {
                    navController.navigate("marketplace") {
                        popUpTo("completeProfile") { inclusive = true }
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
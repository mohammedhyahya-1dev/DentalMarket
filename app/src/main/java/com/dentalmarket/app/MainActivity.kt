package com.dentalmarket.app

import android.os.Bundle
import com.dentalmarket.app.ui.screens.SellScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dentalmarket.app.ui.screens.CartScreen
import com.dentalmarket.app.ui.screens.LoginScreen
import com.dentalmarket.app.ui.screens.MarketplaceScreen
import com.dentalmarket.app.ui.screens.ProductDetailScreen
import com.dentalmarket.app.ui.screens.SignUpScreen
import com.dentalmarket.app.ui.theme.DentalMarketTheme
import com.dentalmarket.app.viewmodel.AuthViewModel
import com.dentalmarket.app.viewmodel.CartViewModel

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
    val authViewModel: AuthViewModel = viewModel()

    // If Firebase already remembers this dentist from last time, skip
    // straight to the marketplace instead of asking them to log in again.
    val startDestination = if (authViewModel.isLoggedIn) "marketplace" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("marketplace") {
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
                    navController.navigate("marketplace") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable("marketplace") {
            MarketplaceScreen(
                cartViewModel = cartViewModel,
                onProductClick = { id -> navController.navigate("product/$id") },
                onCartClick = { navController.navigate("cart") },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSellClick = { navController.navigate("sell") }
            )
        }
        composable("sell") {
            SellScreen(
                onPosted = { navController.popBackStack() }
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
                onBack = { navController.popBackStack() }
            )
        }
        composable("cart") {
            CartScreen(
                cartViewModel = cartViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

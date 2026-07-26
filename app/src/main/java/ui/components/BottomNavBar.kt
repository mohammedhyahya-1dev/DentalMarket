package com.dentalmarket.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.dentalmarket.app.ui.theme.WarmAmber

enum class BottomNavTab {
    HOME, CATEGORIES, CART, MY_ORDERS, PROFILE
}

@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onHomeClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onCartClick: () -> Unit,
    onMyOrdersClick: () -> Unit,
    onProfileClick: () -> Unit,
    cartItemCount: Int = 0
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.HOME,
            onClick = onHomeClick,
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.CATEGORIES,
            onClick = onCategoriesClick,
            icon = { Icon(Icons.Filled.Category, contentDescription = "Categories") },
            label = { Text("Categories") }
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.CART,
            onClick = onCartClick,
            icon = {
                BadgedBox(badge = {
                    if (cartItemCount > 0) {
                        Badge(containerColor = WarmAmber) { Text("$cartItemCount") }
                    }
                }) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
                }
            },
            label = { Text("Cart") }
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.MY_ORDERS,
            onClick = onMyOrdersClick,
            icon = { Icon(Icons.Filled.Receipt, contentDescription = "My Orders") },
            label = { Text("Orders") }
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.PROFILE,
            onClick = onProfileClick,
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}
package com.dentalmarket.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class BottomNavTab {
    HOME, FAVORITES, SELL, MY_ORDERS, PROFILE
}

@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onHomeClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSellClick: () -> Unit,
    onMyOrdersClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == BottomNavTab.HOME,
                onClick = onHomeClick,
                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                label = { Text("Home") }
            )
            NavigationBarItem(
                selected = selectedTab == BottomNavTab.FAVORITES,
                onClick = onFavoritesClick,
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                label = { Text("Favorites") }
            )
            // Empty placeholder item — keeps the five-slot spacing even, but
            // the raised FAB drawn on top of it is what actually handles the
            // Sell tap (it sits higher in z-order than this item).
            NavigationBarItem(
                selected = false,
                enabled = false,
                onClick = {},
                icon = {},
                label = null
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
        ) {
            FloatingActionButton(onClick = onSellClick) {
                Icon(Icons.Filled.Storefront, contentDescription = "Sell a device")
            }
            Text(
                "Sell",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
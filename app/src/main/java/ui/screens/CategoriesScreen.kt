package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dentalmarket.app.data.AuthRepository
import com.dentalmarket.app.model.DeviceCategory
import com.dentalmarket.app.ui.components.BottomNavBar
import com.dentalmarket.app.ui.components.BottomNavTab
import com.dentalmarket.app.ui.components.GuestSignInPrompt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onCategoryClick: (DeviceCategory) -> Unit,
    onHomeClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSellClick: () -> Unit,
    onMyOrdersClick: () -> Unit,
    onProfileClick: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val authRepository = remember { AuthRepository() }
    val isGuest = authRepository.isAnonymous
    var showGuestPrompt by remember { mutableStateOf(false) }

    fun requireLogin(action: () -> Unit) {
        if (isGuest) showGuestPrompt = true else action()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Categories", style = MaterialTheme.typography.headlineMedium) })
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = BottomNavTab.HOME,
                onHomeClick = onHomeClick,
                onFavoritesClick = { requireLogin(onFavoritesClick) },
                onSellClick = { requireLogin(onSellClick) },
                onMyOrdersClick = { requireLogin(onMyOrdersClick) },
                onProfileClick = { requireLogin(onProfileClick) }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(DeviceCategory.entries) { category ->
                CategoryCell(category = category, onClick = { onCategoryClick(category) })
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

@Composable
private fun CategoryCell(category: DeviceCategory, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(category.emoji, fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            category.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
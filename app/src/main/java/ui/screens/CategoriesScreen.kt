package com.dentalmarket.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

// Two-level drill-down: screen 1 is the top-level category grid; tapping a
// category with subcategories pushes screen 2 (that category's subcategory
// grid) via local `selectedCategory` state rather than a separate nav route
// — keeps the bottom nav / guest-prompt plumbing in one place. Tapping a
// category with no subcategories, or any subcategory itself, is a leaf and
// hands off to onLeafSelected instead of navigating within this screen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onLeafSelected: (category: DeviceCategory, subcategory: String?) -> Unit,
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

    var selectedCategory by remember { mutableStateOf<DeviceCategory?>(null) }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selectedCategory?.label ?: "Categories",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    if (selectedCategory != null) {
                        IconButton(onClick = { selectedCategory = null }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
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
        val category = selectedCategory
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (category == null) {
                items(DeviceCategory.entries) { cat ->
                    GridCell(
                        emoji = cat.emoji,
                        label = cat.label,
                        onClick = {
                            if (cat.subcategories.isEmpty()) {
                                onLeafSelected(cat, null)
                            } else {
                                selectedCategory = cat
                            }
                        }
                    )
                }
            } else {
                items(category.subcategories) { sub ->
                    GridCell(
                        emoji = category.emoji,
                        label = sub,
                        onClick = { onLeafSelected(category, sub) }
                    )
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

@Composable
private fun GridCell(emoji: String, label: String, onClick: () -> Unit) {
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
            Text(emoji, fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

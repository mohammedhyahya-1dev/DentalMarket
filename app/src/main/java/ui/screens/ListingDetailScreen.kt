package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.viewmodel.MyListingsViewModel
import com.dentalmarket.app.viewmodel.calculateCommission

// Read-only view of a seller's own listing, reached by tapping a card in
// My Listings. Unlike ProductDetailScreen (the buyer-facing marketplace
// view with cart/inquiry actions), this has no buy button and no
// inquiries — it's just the seller's own item plus a pre-sale commission
// estimate, so it doesn't belong sharing that screen's logic.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listingId: String,
    onBack: () -> Unit,
    viewModel: MyListingsViewModel = viewModel()
) {
    val listing = viewModel.selectedListing.value
    val isLoading = viewModel.isLoadingListing.value
    val commissionPercentage = viewModel.commissionPercentage.value
    val deliveryFeeAmount = viewModel.deliveryFeeAmount.value ?: 0.0

    LaunchedEffect(listingId) {
        viewModel.loadListing(listingId)
        viewModel.loadCommissionConfig()
        viewModel.loadDeliveryConfig()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listing Details", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val currentListing = listing
        if (isLoading || currentListing == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) CircularProgressIndicator() else Text("Listing not found")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentListing.emoji, fontSize = 40.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(currentListing.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "$" + "%.2f".format(currentListing.price),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                val isSold = currentListing.status == "SOLD"
                Text(
                    text = if (isSold) "Sold" else "Available",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(
                            if (isSold) Color(0xFF64748B) else Color(0xFF10B981),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (commissionPercentage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val breakdown = calculateCommission(currentListing.price, commissionPercentage)
                Text(
                    "You'll receive approximately $" + "%.2f".format(breakdown.sellerReceives) +
                        " after our ${"%.1f".format(commissionPercentage)}% platform fee.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Delivery", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(
                if (currentListing.deliveryMethod == "DENTALMARKET_DELIVERS") {
                    "DentalMarket delivers (+$" + "%.2f".format(deliveryFeeAmount) + ")"
                } else {
                    "Seller delivers"
                },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(currentListing.category, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Condition", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(currentListing.condition, style = MaterialTheme.typography.bodyLarge)

            if (currentListing.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Description", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                Text(currentListing.description, style = MaterialTheme.typography.bodyLarge)
            }

            if (currentListing.specifics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Specifications", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                currentListing.specifics.forEach { (key, value) ->
                    Text(
                        "$key: $value",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

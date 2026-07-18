package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.dentalmarket.app.model.Listing
import com.dentalmarket.app.viewmodel.MyListingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    onBack: () -> Unit,
    onEditListing: (String) -> Unit,
    viewModel: MyListingsViewModel = viewModel()
) {
    val listings = viewModel.listings.value
    val isLoading = viewModel.isLoading.value
    var listingPendingDelete by remember { mutableStateOf<Listing?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMyListings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Listings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                listings.isEmpty() -> Text(
                    "You haven't posted any devices yet",
                    style = MaterialTheme.typography.titleMedium
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(listings, key = { it.id }) { listing ->
                        MyListingCard(
                            listing = listing,
                            onEdit = { onEditListing(listing.id) },
                            onDelete = { listingPendingDelete = listing }
                        )
                    }
                }
            }
        }
    }

    listingPendingDelete?.let { listing ->
        AlertDialog(
            onDismissRequest = { listingPendingDelete = null },
            title = { Text("Delete listing?") },
            text = { Text("This will permanently remove \"${listing.name}\" from the marketplace.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteListing(listing.id)
                    listingPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { listingPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MyListingCard(listing: Listing, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isSold = listing.status == "SOLD"

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(listing.emoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(listing.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$" + "%.2f".format(listing.price),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
            if (!isSold) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Text("Edit")
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
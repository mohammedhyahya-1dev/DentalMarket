package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dentalmarket.app.data.ListingRepository
import com.dentalmarket.app.model.Condition
import com.dentalmarket.app.model.Inquiry
import com.dentalmarket.app.model.Listing
import com.dentalmarket.app.ui.components.ConditionBadge
import com.dentalmarket.app.ui.theme.BoneWhite
import com.dentalmarket.app.ui.theme.WarmAmber
import com.dentalmarket.app.viewmodel.CartViewModel
import com.dentalmarket.app.viewmodel.InquiryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    listingId: String,
    cartViewModel: CartViewModel,
    inquiryViewModel: InquiryViewModel,
    buyerId: String,
    buyerName: String,
    onBack: () -> Unit
) {
    var listing by remember { mutableStateOf<Listing?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var showAddedMessage by remember { mutableStateOf(false) }
    val repository = remember { ListingRepository() }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var questionText by remember { mutableStateOf("") }

    LaunchedEffect(listingId) {
        val result = repository.getListingById(listingId)
        result.onSuccess { listing = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listing?.name ?: "", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val currentListing = listing
        if (currentListing == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(BoneWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(currentListing.emoji, fontSize = 48.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            ConditionBadge(Condition.valueOf(currentListing.condition))
            Spacer(modifier = Modifier.height(12.dp))
            Text(currentListing.category, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$" + "%.2f".format(currentListing.price),
                style = MaterialTheme.typography.headlineMedium,
                color = WarmAmber
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(currentListing.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quantity", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { if (quantity > 1) quantity-- }) {
                    Text("\u2212", fontSize = 22.sp)
                }
                Text("$quantity", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { quantity++ }) {
                    Text("+", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    cartViewModel.addToCart(currentListing, quantity)
                    showAddedMessage = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to Cart")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showQuestionDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ask a Question")
            }

            if (showAddedMessage) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Added to cart \u2713",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (showQuestionDialog) {
                AlertDialog(
                    onDismissRequest = { showQuestionDialog = false },
                    title = { Text("Ask a Question") },
                    text = {
                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            label = { Text("Your question") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                inquiryViewModel.submitInquiry(
                                    Inquiry(
                                        listingId = currentListing.id,
                                        listingName = currentListing.name,
                                        buyerId = buyerId,
                                        buyerName = buyerName,
                                        sellerName = currentListing.sellerName
                                    ).copy(question = questionText)
                                ) {
                                    showQuestionDialog = false
                                    questionText = ""
                                }
                            },
                            enabled = questionText.isNotBlank()
                        ) {
                            Text("Send")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showQuestionDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
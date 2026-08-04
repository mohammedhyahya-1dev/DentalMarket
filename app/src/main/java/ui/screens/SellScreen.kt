package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.model.Condition
import com.dentalmarket.app.model.DeviceCategory
import com.dentalmarket.app.viewmodel.ListingViewModel
import com.dentalmarket.app.viewmodel.calculatePayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import com.dentalmarket.app.viewmodel.SpecRow
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(
    onPosted: () -> Unit,
    listingId: String? = null,
    viewModel: ListingViewModel = viewModel()
) {
    var conditionExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val isEditMode = listingId != null

    LaunchedEffect(listingId) {
        if (listingId != null) {
            viewModel.loadListingForEdit(listingId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadCommissionConfig()
        viewModel.loadDeliveryConfig()
    }

    LaunchedEffect(viewModel.postSuccess.value) {
        if (viewModel.postSuccess.value) onPosted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isEditMode) "Edit Listing" else "Sell a Device",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = viewModel.name.value,
            onValueChange = { viewModel.name.value = it },
            label = { Text("Device name") },
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = viewModel.category.value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                DeviceCategory.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text("${cat.emoji} ${cat.label}") },
                        onClick = {
                            viewModel.category.value = cat.label
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = conditionExpanded,
            onExpandedChange = { conditionExpanded = it }
        ) {
            OutlinedTextField(
                value = viewModel.condition.value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Condition") },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = conditionExpanded,
                onDismissRequest = { conditionExpanded = false }
            ) {
                Condition.entries.forEach { cond ->
                    DropdownMenuItem(
                        text = { Text(cond.label) },
                        onClick = {
                            viewModel.condition.value = cond.name
                            conditionExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = viewModel.price.value,
            onValueChange = { viewModel.price.value = it },
            label = { Text("Price ($)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = viewModel.description.value,
            onValueChange = { viewModel.description.value = it },
            label = { Text("Description (optional)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        val commissionPercentage = viewModel.commissionPercentage.value
        val priceValue = viewModel.price.value.toDoubleOrNull()
        if (commissionPercentage != null && priceValue != null && priceValue > 0) {
            val deliveryFeeToSubtract = if (viewModel.deliveryMethod.value == "DENTALMARKET_DELIVERS") {
                viewModel.deliveryFeeAmount.value ?: 0.0
            } else {
                0.0
            }
            val breakdown = calculatePayout(priceValue, commissionPercentage, deliveryFeeToSubtract)
            Text(
                "You'll receive approximately $" + "%.2f".format(breakdown.sellerReceives) +
                    " after our ${"%.1f".format(commissionPercentage)}% platform fee" +
                    if (deliveryFeeToSubtract > 0) " and $" + "%.2f".format(deliveryFeeToSubtract) + " delivery fee." else ".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Text("Delivery", style = MaterialTheme.typography.titleMedium)
        val deliveryFeeAmount = viewModel.deliveryFeeAmount.value ?: 0.0
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = viewModel.deliveryMethod.value == "SELLER_DELIVERS",
                onClick = { viewModel.deliveryMethod.value = "SELLER_DELIVERS" }
            )
            Text("I'll deliver it myself", style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = viewModel.deliveryMethod.value == "DENTALMARKET_DELIVERS",
                onClick = { viewModel.deliveryMethod.value = "DENTALMARKET_DELIVERS" }
            )
            Text(
                "DentalMarket delivers (fee deducted from your payout: $" + "%.2f".format(deliveryFeeAmount) + ")",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text("Specifications (optional)", style = MaterialTheme.typography.titleMedium)

        viewModel.specifics.forEach { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = row.key.value,
                    onValueChange = { row.key.value = it },
                    label = { Text("e.g. Brand") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = row.value.value,
                    onValueChange = { row.value.value = it },
                    label = { Text("e.g. Sirona") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.removeSpecRow(row) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                }
            }
        }

        OutlinedButton(
            onClick = { viewModel.addSpecRow() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add Specification")
        }

        OutlinedTextField(
            value = viewModel.emoji.value,
            onValueChange = { viewModel.emoji.value = it },
            label = { Text("Emoji icon") },
            modifier = Modifier.fillMaxWidth()
        )

        viewModel.errorMessage.value?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { viewModel.postListing() },
            enabled = !viewModel.isLoading.value,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    viewModel.isLoading.value -> if (isEditMode) "Saving..." else "Posting..."
                    isEditMode -> "Save Changes"
                    else -> "Post Listing"
                }
            )
        }
    }
}
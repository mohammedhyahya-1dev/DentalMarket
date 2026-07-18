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
import com.dentalmarket.app.viewmodel.ListingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(
    onPosted: () -> Unit,
    listingId: String? = null,
    viewModel: ListingViewModel = viewModel()
) {
    var conditionExpanded by remember { mutableStateOf(false) }
    val isEditMode = listingId != null

    LaunchedEffect(listingId) {
        if (listingId != null) {
            viewModel.loadListingForEdit(listingId)
        }
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

        OutlinedTextField(
            value = viewModel.category.value,
            onValueChange = { viewModel.category.value = it },
            label = { Text("Category (e.g. Sterilizer)") },
            modifier = Modifier.fillMaxWidth()
        )

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
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

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
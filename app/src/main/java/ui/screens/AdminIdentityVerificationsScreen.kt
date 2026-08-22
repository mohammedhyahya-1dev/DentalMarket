package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.dentalmarket.app.model.IdentityVerification
import com.dentalmarket.app.viewmodel.IdentityVerificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIdentityVerificationsScreen(
    onBack: () -> Unit,
    viewModel: IdentityVerificationViewModel = viewModel()
) {
    val pending by viewModel.pendingSubmissions
    val isLoading by viewModel.isLoadingPending
    val submitterLabels by viewModel.submitterLabels
    val imageUrls by viewModel.imageUrls
    val processingUid by viewModel.processingUid
    var rejectTarget by remember { mutableStateOf<IdentityVerification?>(null) }

    LaunchedEffect(Unit) { viewModel.loadPendingSubmissions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identity Verifications (Admin)", style = MaterialTheme.typography.titleLarge) },
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
                pending.isEmpty() -> Text("No pending submissions", style = MaterialTheme.typography.titleMedium)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(pending, key = { it.uid }) { submission ->
                        SubmissionCard(
                            submission = submission,
                            submitterLabel = submitterLabels[submission.uid] ?: submission.uid,
                            imageUrls = imageUrls,
                            isProcessing = processingUid == submission.uid,
                            onApprove = { viewModel.approve(submission.uid) },
                            onRejectClick = { rejectTarget = submission }
                        )
                    }
                }
            }
        }
    }

    rejectTarget?.let { submission ->
        RejectVerificationDialog(
            onDismiss = { rejectTarget = null },
            onConfirm = { reason ->
                viewModel.reject(submission.uid, reason)
                rejectTarget = null
            }
        )
    }
}

@Composable
private fun SubmissionCard(
    submission: IdentityVerification,
    submitterLabel: String,
    imageUrls: Map<String, String>,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onRejectClick: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Text(submitterLabel, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(
                    listOf(
                        "National ID (front)" to submission.nationalIdFrontPath,
                        "National ID (back)" to submission.nationalIdBackPath,
                        "Dental Assoc. ID (front)" to submission.dentalAssocIdFrontPath,
                        "Dental Assoc. ID (back)" to submission.dentalAssocIdBackPath
                    )
                ) { (label, path) ->
                    Column {
                        val url = imageUrls[path]
                        if (url != null) {
                            AsyncImage(
                                model = url,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            }
                        }
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Approve")
                }
                OutlinedButton(
                    onClick = onRejectClick,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RejectVerificationDialog(
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Verification") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason (shown to the seller)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(reason) }) {
                Text("Reject")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

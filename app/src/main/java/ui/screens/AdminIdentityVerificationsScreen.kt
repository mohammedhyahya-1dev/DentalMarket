package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }

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
                            onRejectClick = { rejectTarget = submission },
                            onImageClick = { url -> zoomedImageUrl = url }
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

    zoomedImageUrl?.let { url ->
        ZoomableImageDialog(url = url, onDismiss = { zoomedImageUrl = null })
    }
}

@Composable
private fun SubmissionCard(
    submission: IdentityVerification,
    submitterLabel: String,
    imageUrls: Map<String, String>,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onRejectClick: () -> Unit,
    onImageClick: (String) -> Unit
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
                                    .clickable { onImageClick(url) }
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

// Full-screen pinch-to-zoom-and-pan viewer for a single ID thumbnail —
// these are the one place in this admin screen where actually reading the
// card's text matters. Built on plain Compose foundation gestures
// (detectTransformGestures + graphicsLayer), no new dependency beyond the
// Coil AsyncImage already in use elsewhere on this screen. Dismiss is a
// close button (always visible, unambiguous) plus system back — not a tap
// on the image itself, since that would collide with "tap to reset zoom"
// gestures every other photo viewer in Android already trains people to
// expect here.
@Composable
private fun ZoomableImageDialog(url: String, onDismiss: () -> Unit) {
    var scale by remember(url) { mutableStateOf(1f) }
    var offsetX by remember(url) { mutableStateOf(0f) }
    var offsetY by remember(url) { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(url) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 6f)
                        // Clamp panning to how far the scaled image can
                        // actually extend past the viewport — otherwise a
                        // zoomed-in pan can drag the image edge into the
                        // middle of the screen with empty space around it.
                        val maxOffsetX = (size.width * (newScale - 1)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1)) / 2f
                        scale = newScale
                        offsetX = if (newScale <= 1f) 0f else (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                        offsetY = if (newScale <= 1f) 0f else (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    }
                }
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Zoomed identity document",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

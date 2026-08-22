package com.dentalmarket.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.dentalmarket.app.data.identityVerificationImageSlots
import com.dentalmarket.app.util.ImageCompression
import com.dentalmarket.app.viewmodel.IdentityVerificationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SlotSpec(val key: String, val label: String)

private fun labelForSlot(key: String): String = when (key) {
    "nationalIdFront" -> "National ID (front)"
    "nationalIdBack" -> "National ID (back)"
    "dentalAssocIdFront" -> "Dental Association ID (front)"
    "dentalAssocIdBack" -> "Dental Association ID (back)"
    else -> key
}

// Keyed off the repository's own slot list rather than a second hardcoded
// set, so the two can't drift apart.
private val SLOTS = identityVerificationImageSlots().map { SlotSpec(it, labelForSlot(it)) }

// Reached two ways — right after CompleteProfileScreen (with a Skip option,
// see MainActivity's fromOnboarding nav arg) and later anytime from
// ProfileScreen's "Verify your identity" row. Same screen either way; only
// the exit destination differs, decided by the caller via onDone.
@Composable
fun IdentityVerificationScreen(
    onDone: () -> Unit,
    showSkip: Boolean,
    viewModel: IdentityVerificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val mySubmission by viewModel.mySubmission
    val isLoading by viewModel.isLoadingMySubmission
    val isSubmitting by viewModel.isSubmitting
    val submitError by viewModel.submitError

    // slot -> picked image Uri (for the thumbnail) and its compressed bytes
    // (what actually gets uploaded) — kept together so re-picking a slot
    // replaces both at once.
    var pickedUris by remember { mutableStateOf<Map<String, Uri>>(emptyMap()) }
    var compressedBytes by remember { mutableStateOf<Map<String, ByteArray>>(emptyMap()) }
    var compressingSlot by remember { mutableStateOf<String?>(null) }
    var activeSlot by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadMySubmission() }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val slot = activeSlot
        activeSlot = null
        if (uri == null || slot == null) return@rememberLauncherForActivityResult
        pickedUris = pickedUris + (slot to uri)
    }

    // Compresses off the main thread as soon as a slot's Uri changes —
    // decoding/rescaling a full-size photo is real CPU work, and this keeps
    // it from janking the picker UI.
    LaunchedEffect(pickedUris) {
        for ((slot, uri) in pickedUris) {
            if (compressedBytes.containsKey(slot)) continue
            compressingSlot = slot
            val bytes = withContext(Dispatchers.Default) {
                ImageCompression.compressToJpeg { context.contentResolver.openInputStream(uri)!! }
            }
            compressedBytes = compressedBytes + (slot to bytes)
        }
        compressingSlot = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Verify Your Identity", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Upload clear photos of your National ID and Iraqi Dental Association ID " +
                "(front and back of each). This is reviewed by our team before your profile " +
                "shows as identity-verified to buyers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(24.dp))

        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            mySubmission?.status == "PENDING" -> PendingCard()
            mySubmission?.status == "APPROVED" -> ApprovedCard()
            else -> {
                mySubmission?.takeIf { it.status == "REJECTED" }?.let { rejected ->
                    RejectedBanner(rejected.rejectionReason)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SLOTS.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { slot ->
                            UploadSlot(
                                label = slot.label,
                                pickedUri = pickedUris[slot.key],
                                isCompressing = compressingSlot == slot.key,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeSlot = slot.key
                                    pickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                submitError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val allSlotsReady = SLOTS.all { compressedBytes.containsKey(it.key) }
                Button(
                    onClick = { viewModel.submit(compressedBytes, onSuccess = onDone) },
                    enabled = allSlotsReady && !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    } else {
                        Text("Submit for Review")
                    }
                }

                if (showSkip) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text("Skip for now")
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadSlot(
    label: String,
    pickedUri: Uri?,
    isCompressing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (pickedUri != null) {
                AsyncImage(
                    model = pickedUri,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isCompressing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(28.dp))
                    }
                }
            } else {
                Icon(Icons.Filled.CloudUpload, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PendingCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Verification pending", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Your submission is with our team for review. This usually doesn't take long.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ApprovedCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF15803D))
                Spacer(modifier = Modifier.width(8.dp))
                Text("You're identity-verified", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun RejectedBanner(reason: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Your last submission wasn't approved",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF9A3412)
            )
            if (reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(reason, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9A3412))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Upload fresh photos below to try again.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9A3412)
            )
        }
    }
}

package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dentalmarket.app.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onMyQuestionsClick: () -> Unit,
    onAdminInquiriesClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onVerifyIdentityClick: () -> Unit,
    onAdminIdentityVerificationsClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val profile = viewModel.profile.value
    val isLoading = viewModel.isLoading.value
    val isEmailVerified = viewModel.isEmailVerified.value
    val resendSuccess = viewModel.resendSuccess.value
    val verificationStatus = viewModel.verificationStatus.value
    var showUsernameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", style = MaterialTheme.typography.titleLarge) },
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
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 40.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        profile?.name?.takeIf { it.isNotBlank() } ?: "Unknown",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        profile?.email ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Blank only in the brief window between account
                    // creation and AuthRepository.ensureUsername()'s async
                    // backfill completing — nothing to show or edit yet.
                    profile?.username?.takeIf { it.isNotBlank() }?.let { username ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "@$username",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            TextButton(onClick = { showUsernameDialog = true }) {
                                Text("Edit")
                            }
                        }
                    }

                    if (viewModel.isAdmin) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Administrator",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .background(Color(0xFF6366F1), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!isEmailVerified) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Email not verified",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF9A3412)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Check your inbox for a verification link (check your Spam folder " +
                                        "if you don't see it), or resend it below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9A3412)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                if (resendSuccess) {
                                    Text(
                                        "Verification email sent \u2713",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF15803D)
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.resendVerificationEmail() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Resend Verification Email")
                                    }
                                    viewModel.errorMessage.value?.let { message ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            message,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedButton(
                        onClick = onWatchlistClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Watchlist")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onMyQuestionsClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Questions")
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    VerificationRow(status = verificationStatus, onClick = onVerifyIdentityClick)

                    if (viewModel.isAdmin) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onAdminInquiriesClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buyer Questions (Admin)")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onAdminIdentityVerificationsClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Identity Verifications (Admin)")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    LegalLink("Terms of Service", "https://dentalmarket-abdf6.firebaseapp.com/terms.html")
                    Spacer(modifier = Modifier.height(8.dp))
                    LegalLink("Privacy Policy", "https://dentalmarket-abdf6.firebaseapp.com/privacy.html")

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }

    if (showUsernameDialog) {
        UsernameEditDialog(
            currentUsername = profile?.username ?: "",
            isSaving = viewModel.isChangingUsername.value,
            errorMessage = viewModel.usernameErrorMessage.value,
            onDismiss = { showUsernameDialog = false },
            onSave = { newUsername ->
                viewModel.changeUsername(newUsername) { showUsernameDialog = false }
            }
        )
    }
}

@Composable
private fun UsernameEditDialog(
    currentUsername: String,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentUsername) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Username") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "3-20 characters, start with a letter, letters/numbers/underscores only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(text) },
                enabled = !isSaving && text.isNotBlank() && text != currentUsername
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

// status is identityVerifications/{uid}.status, or null if no submission
// exists yet — APPROVED shows a static indicator (nothing left to do),
// PENDING shows a non-clickable "awaiting review" row, and null/REJECTED
// both invite tapping through to IdentityVerificationScreen (REJECTED
// reaches that screen's own resubmit flow).
@Composable
private fun VerificationRow(status: String?, onClick: () -> Unit) {
    when (status) {
        "APPROVED" -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Identity verified")
        }
        "PENDING" -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Verification pending")
        }
        else -> OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Verify your identity")
        }
    }
}

@Composable
private fun LegalLink(text: String, url: String) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    )
    Text(
        text = buildAnnotatedString {
            withLink(LinkAnnotation.Url(url, linkStyle)) {
                append(text)
            }
        },
        style = MaterialTheme.typography.bodyMedium
    )
}
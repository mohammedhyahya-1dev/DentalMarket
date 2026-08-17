package com.dentalmarket.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// actionLabel reads as "before $actionLabel" — e.g. "selling" or "checking
// out" — so the same dialog fits both Sell and Checkout without a second
// near-identical component.
@Composable
fun VerifyEmailPrompt(
    actionLabel: String,
    isResending: Boolean,
    resendSuccess: Boolean,
    resendError: String?,
    onDismiss: () -> Unit,
    onResend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify your email") },
        text = {
            Column {
                Text(
                    "Please verify your email before $actionLabel. Check your inbox for the link " +
                        "(check your Spam folder if you don't see it), or resend it below."
                )
                if (resendSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Verification email sent ✓", color = MaterialTheme.colorScheme.primary)
                }
                resendError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onResend, enabled = !isResending && !resendSuccess) {
                if (isResending) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Resend Verification Email")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

package com.dentalmarket.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GuestSignInPrompt(onDismiss: () -> Unit, onSignIn: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign in to continue") },
        text = { Text("Create a free account or log in to do that.") },
        confirmButton = {
            Button(onClick = onSignIn) {
                Text("Sign Up / Log In")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

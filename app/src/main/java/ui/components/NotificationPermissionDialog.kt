package com.dentalmarket.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat

// Mercari-style translucent overlay card, shown from MarketplaceScreen
// instead of NotificationPermissionScreen's old full-screen nav
// destination — see shouldShowNotificationPrompt() below for when.
@Composable
fun NotificationPermissionDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text("🔔", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Please Enable Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Get important updates about your orders as they happen",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                        Text("Allow")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Later")
                    }
                }
            }
        }
    }
}

// Gates the prompt to once ever per install: skip if the OS permission is
// already granted (Android 13+ only — earlier versions have no runtime
// POST_NOTIFICATIONS permission at all), or if the user already saw this
// dialog before, regardless of which of its three exits they used (Allow,
// X, or Later all call markNotificationPromptShown below).
fun shouldShowNotificationPrompt(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return false
    }
    val prefs = context.getSharedPreferences("dentalmarket_prefs", Context.MODE_PRIVATE)
    return !prefs.getBoolean("notification_prompt_shown", false)
}

fun markNotificationPromptShown(context: Context) {
    val prefs = context.getSharedPreferences("dentalmarket_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("notification_prompt_shown", true).apply()
}

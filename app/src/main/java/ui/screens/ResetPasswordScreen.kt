package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dentalmarket.app.viewmodel.AuthViewModel
import androidx.compose.foundation.background

@Composable
fun ResetPasswordScreen(
    authViewModel: AuthViewModel,
    code: String,
    onDone: () -> Unit
) {
    var isVerifying by remember { mutableStateOf(true) }
    var isCodeValid by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf<String?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var isResetComplete by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    LaunchedEffect(code) {
        authViewModel.verifyResetCode(code) { valid, verifiedEmail ->
            isCodeValid = valid
            email = verifiedEmail
            isVerifying = false
        }
    }

    val hasMinLength = newPassword.length >= 8
    val hasUppercase = newPassword.any { it.isUpperCase() }
    val hasNumber = newPassword.any { it.isDigit() }
    val hasSpecialChar = newPassword.any { !it.isLetterOrDigit() }
    val rulesMet = listOf(hasMinLength, hasUppercase, hasNumber, hasSpecialChar).count { it }
    val strengthLabel = when {
        rulesMet <= 1 -> "Weak"
        rulesMet <= 3 -> "Good"
        else -> "Excellent"
    }
    val strengthColor = when (strengthLabel) {
        "Weak" -> Color(0xFFE53935)
        "Good" -> Color(0xFFFB8C00)
        else -> Color(0xFF43A047)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (isVerifying) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (!isCodeValid) {
            Text("This reset link is invalid or has expired.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Login")
            }
            return@Column
        }

        if (isResetComplete) {
            Text("Your password has been reset.", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Login")
            }
            return@Column
        }

        Text("Reset Password", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Setting a new password for ${email ?: ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (newPassword.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                repeat(3) { index ->
                    val segmentsFilled = when (strengthLabel) {
                        "Weak" -> 1
                        "Good" -> 2
                        else -> 3
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (index < segmentsFilled) strengthColor else MaterialTheme.colorScheme.outlineVariant)
                    )
                    if (index < 2) Spacer(modifier = Modifier.width(4.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Password strength: $strengthLabel",
                style = MaterialTheme.typography.bodySmall,
                color = strengthColor
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                authViewModel.confirmPasswordReset(code, newPassword) {
                    isResetComplete = true
                }
            },
            enabled = !isLoading && rulesMet == 4,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Reset Password")
            }
        }
    }
}
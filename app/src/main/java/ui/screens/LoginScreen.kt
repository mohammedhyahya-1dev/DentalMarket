package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.dentalmarket.app.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasNumber = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }
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
        Text("Welcome to DentalMarket", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetSignInWithGoogleOption
                        .Builder("921438998372-atagvcl4ukgp89q1chc0mo36kb3cvrpi.apps.googleusercontent.com")
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    try {
                        val result = credentialManager.getCredential(context, request)
                        val credential = result.credential
                        if (credential is CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            authViewModel.signInWithGoogle(googleIdTokenCredential.idToken) { onLoginSuccess() }
                        }
                    } catch (e: GetCredentialException) {
                        // User cancelled the picker \u2014 safe to ignore
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFF4285F4), CircleShape)
            ) {
                Text(
                    "G",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Continue with Google")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Divider(modifier = Modifier.weight(1f))
            Text(
                "  OR  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Divider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (password.isNotEmpty()) {
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

            Spacer(modifier = Modifier.height(8.dp))
            PasswordRequirementRow(met = hasMinLength, text = "At least 8 characters")
            PasswordRequirementRow(met = hasUppercase, text = "One uppercase letter")
            PasswordRequirementRow(met = hasNumber, text = "One number")
            PasswordRequirementRow(met = hasSpecialChar, text = "One special character (!@#\$ etc.)")
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { authViewModel.logInOrSignUp(email, password) { onLoginSuccess() } },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun PasswordRequirementRow(met: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (met) "\u2713" else "\u25CB",
            color = if (met) Color(0xFF43A047) else MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (met) Color(0xFF43A047) else MaterialTheme.colorScheme.outline
        )
    }
}
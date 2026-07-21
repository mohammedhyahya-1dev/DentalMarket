package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dentalmarket.app.viewmodel.AuthViewModel

private val titleOptions = listOf("Dr.", "Mr.", "Ms.")

private val specialtyOptions = listOf(
    "General Dentistry", "Restorative Dentistry", "Oral and Maxillofacial Surgery",
    "Endodontics", "Orthodontics", "Prosthodontics", "Periodontics", "Pedodontics",
    "Oral Medicine", "Oral and Maxillofacial Pathology", "Oral and Maxillofacial Radiology",
    "Dental Public Health", "Dental Student", "Other"
)

private val provinceOptions = listOf(
    "Basra", "Baghdad", "Erbil", "Dohuk", "Ninawa", "Kirkuk", "Karbala",
    "Al Anbar", "Babil", "Sulaymaniyah", "Halabja", "Najaf", "Salah Al-Din",
    "Muthanna", "Maysan", "Diyala", "Al-Qadisiyyah", "Dhi Qar", "Wasit"
)

@Composable
fun CompleteProfileScreen(
    authViewModel: AuthViewModel,
    onComplete: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var extraMobile by remember { mutableStateOf("") }

    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Complete Your Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We need a few more details before you can start buying and selling.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            PickerField(
                label = "Title",
                options = titleOptions,
                selected = title,
                onSelected = { title = it },
                modifier = Modifier.weight(0.28f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.weight(0.36f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.weight(0.36f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PickerField(
            label = "Specialty",
            options = specialtyOptions,
            selected = specialty,
            onSelected = { specialty = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        PickerField(
            label = "Province",
            options = provinceOptions,
            selected = province,
            onSelected = { province = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = it },
            label = { Text("Mobile") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = extraMobile,
            onValueChange = { extraMobile = it },
            label = { Text("Extra Mobile (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                authViewModel.completeProfile(
                    title, firstName, lastName, specialty, province, mobile, extraMobile
                ) { onComplete() }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Save and Continue")
            }
        }
    }
}

// A tap-to-open field that shows a scrollable list of choices in a dialog —
// same building blocks (OutlinedTextField + AlertDialog) the app already
// uses elsewhere, instead of the newer ExposedDropdownMenu APIs.
@Composable
private fun PickerField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        // Invisible layer on top that catches the tap — needed because a
        // read-only text field alone won't reliably respond to clicks.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }
    if (showDialog) {
    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text("Select $label") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    Text(
                        option,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelected(option)
                                showDialog = false
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showDialog = false }) {
                Text("Cancel")
            }
        }
    )
    }
}

package com.dentalmarket.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dentalmarket.app.model.Inquiry
import com.dentalmarket.app.viewmodel.InquiryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminInquiriesScreen(
    inquiryViewModel: InquiryViewModel,
    onBack: () -> Unit
) {
    val inquiries by inquiryViewModel.inquiries.collectAsState()
    val isLoading by inquiryViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        inquiryViewModel.loadAllInquiries()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buyer Questions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (inquiries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No questions yet", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(inquiries) { inquiry ->
                InquiryCard(
                    inquiry = inquiry,
                    onAnswer = { answer ->
                        inquiryViewModel.answerInquiry(inquiry.id, answer) {
                            inquiryViewModel.loadAllInquiries()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InquiryCard(
    inquiry: Inquiry,
    onAnswer: (String) -> Unit
) {
    var answerText by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${inquiry.listingName} \u2014 from ${inquiry.buyerName}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Seller: ${inquiry.sellerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                "Q: ${inquiry.question}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (inquiry.status == "ANSWERED") {
                Text(
                    "A: ${inquiry.answer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                OutlinedTextField(
                    value = answerText,
                    onValueChange = { answerText = it },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Button(
                    onClick = { onAnswer(answerText) },
                    enabled = answerText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Send Answer")
                }
            }
        }
    }
}
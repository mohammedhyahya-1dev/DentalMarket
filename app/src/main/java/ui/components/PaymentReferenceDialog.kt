package com.dentalmarket.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dentalmarket.app.model.Order
import com.dentalmarket.app.model.PaymentConfig
import com.dentalmarket.app.model.formatPrice
import com.dentalmarket.app.model.roundPrice

// Shared by MyOrdersScreen (buyer taps "Enter Payment Reference" on an
// existing order) and CartScreen (shown automatically right after
// checkout, one per order created) — same dialog either way.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReferenceDialog(
    order: Order,
    paymentConfig: PaymentConfig?,
    paymentConfigLoadFailed: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (reference: String) -> Unit
) {
    var reference by remember { mutableStateOf("") }
    // Delivery fee is deducted from the seller's payout, not charged to the
    // buyer — only the item price and an opted-in safety fee are collected
    // here.
    // Both parts rounded first, then added, so the "(Item: A + Safety fee: B)"
    // line below always adds up to the amount the buyer is told to transfer.
    val itemLineTotal = roundPrice(order.price) * order.quantity
    val safetyFee = roundPrice(order.safetyFee)
    val amountDue = itemLineTotal + safetyFee

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment Instructions") },
        text = {
            Column {
                Text(
                    "Pay " + formatPrice(amountDue) + " via QiCard or ZainCash to:",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (order.safetyFee > 0) {
                    Text(
                        "(Item: " + formatPrice(itemLineTotal) +
                            " + Safety fee: " + formatPrice(safetyFee) + ")",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    paymentConfigLoadFailed -> Text(
                        "Couldn't load payment account details. Please try again shortly or contact support.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    paymentConfig == null -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else -> {
                        Text(
                            "QiCard: ${paymentConfig.qicardAccount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "ZainCash: ${paymentConfig.zaincashAccount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Then enter the transaction reference number from your receipt below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Transaction reference number") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reference) },
                enabled = !isSubmitting && reference.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

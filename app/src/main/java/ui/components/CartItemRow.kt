package com.dentalmarket.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dentalmarket.app.model.CartItem

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.listing.emoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.listing.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "$" + "%.2f".format(item.listing.price) + " each",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        IconButton(onClick = onDecrease) {
            Text("\u2212", fontSize = 20.sp)
        }
        Text("${item.quantity}", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onIncrease) {
            Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove item")
        }
    }
}
package com.dentalmarket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dentalmarket.app.model.Condition
import com.dentalmarket.app.model.Listing
import com.dentalmarket.app.model.SellerRatingSummary
import com.dentalmarket.app.ui.theme.BoneWhite
import com.dentalmarket.app.ui.theme.WarmAmber

@Composable
fun ProductCard(
    listing: Listing,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
    isWatched: Boolean = false,
    onToggleWatch: (() -> Unit)? = null,
    sellerRating: SellerRatingSummary? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(BoneWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(listing.emoji, fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    listing.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConditionBadge(Condition.valueOf(listing.condition))
                    if (sellerRating != null && sellerRating.count > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        RatingBadge(sellerRating)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "$" + "%.2f".format(listing.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmAmber
                )
            }
            if (onToggleWatch != null) {
                IconButton(onClick = onToggleWatch) {
                    Icon(
                        if (isWatched) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isWatched) "Remove from watchlist" else "Add to watchlist",
                        tint = if (isWatched) WarmAmber else MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            FilledIconButton(onClick = onAddToCart) {
                Icon(Icons.Filled.Add, contentDescription = "Add to cart")
            }
        }
    }
}
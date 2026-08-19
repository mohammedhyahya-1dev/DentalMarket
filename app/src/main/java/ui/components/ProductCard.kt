package com.dentalmarket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(BoneWhite),
            contentAlignment = Alignment.Center
        ) {
            Text(listing.emoji, fontSize = 40.sp)
            Text(
                "$" + "%.2f".format(listing.price),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(WarmAmber, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                listing.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConditionBadge(Condition.valueOf(listing.condition))
                if (sellerRating != null && sellerRating.count > 0) {
                    Spacer(modifier = Modifier.size(6.dp))
                    RatingBadge(sellerRating)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onToggleWatch != null) {
                    IconButton(onClick = onToggleWatch, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isWatched) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isWatched) "Remove from watchlist" else "Add to watchlist",
                            tint = if (isWatched) WarmAmber else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                FilledIconButton(onClick = onAddToCart, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to cart")
                }
            }
        }
    }
}
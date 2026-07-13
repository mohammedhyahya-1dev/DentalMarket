package com.dentalmarket.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dentalmarket.app.model.Condition
import com.dentalmarket.app.ui.theme.ConditionFair
import com.dentalmarket.app.ui.theme.ConditionGood
import com.dentalmarket.app.ui.theme.ConditionLikeNew

@Composable
fun ConditionBadge(condition: Condition, modifier: Modifier = Modifier) {
    val color = when (condition) {
        Condition.LIKE_NEW -> ConditionLikeNew
        Condition.GOOD -> ConditionGood
        Condition.FAIR -> ConditionFair
    }
    Text(
        text = condition.label,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

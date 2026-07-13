package com.dentalmarket.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = DeepTeal,
    onPrimary = BoneWhite,
    secondary = WarmAmber,
    onSecondary = InkCharcoal,
    background = BoneWhite,
    onBackground = InkCharcoal,
    surface = CardWhite,
    onSurface = InkCharcoal,
)

@Composable
fun DentalMarketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = DentalMarketTypography,
        content = content
    )
}

package com.dentalmarket.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AccentBluePurple,
    onPrimary = Color.White,
    primaryContainer = AccentBluePurpleContainer,
    onPrimaryContainer = OnAccentBluePurpleContainer,
    secondary = AccentIndigo,
    onSecondary = Color.White,
    secondaryContainer = AccentIndigoContainer,
    onSecondaryContainer = OnAccentIndigoContainer,
    tertiary = AccentPeriwinkle,
    onTertiary = Color.White,
    tertiaryContainer = AccentPeriwinkleContainer,
    onTertiaryContainer = OnAccentPeriwinkleContainer,
    background = BackgroundCool,
    onBackground = InkCool,
    surface = CardWhite,
    onSurface = InkCool,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralOnSurfaceVariant,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,
)

@Composable
fun DentalMarketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = DentalMarketTypography,
        content = content
    )
}

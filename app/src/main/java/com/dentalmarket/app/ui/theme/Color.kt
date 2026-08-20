package com.dentalmarket.app.ui.theme

import androidx.compose.ui.graphics.Color

// Core brand accent — Mercari-style blue-purple. Only referenced from
// Theme.kt's lightColorScheme() below; feature code should reach these
// through MaterialTheme.colorScheme.* roles, never import a color constant
// directly, so a future palette change is a one-file edit again.
val AccentBluePurple = Color(0xFF5E6DF2)
val AccentBluePurpleContainer = Color(0xFFE3E4FC)
val OnAccentBluePurpleContainer = Color(0xFF1A1B4B)

val AccentIndigo = Color(0xFF4A4E8C)
val AccentIndigoContainer = Color(0xFFE1E0F9)
val OnAccentIndigoContainer = Color(0xFF14153B)

val AccentPeriwinkle = Color(0xFF7C6FE0)
val AccentPeriwinkleContainer = Color(0xFFE7E3FF)
val OnAccentPeriwinkleContainer = Color(0xFF1F1147)

val BackgroundCool = Color(0xFFFAFAFE)
val InkCool = Color(0xFF1B1B23)
val CardWhite = Color(0xFFFFFFFF)

val NeutralSurfaceVariant = Color(0xFFE6E5F0)
val NeutralOnSurfaceVariant = Color(0xFF46454F)
val NeutralOutline = Color(0xFF6F6D79)
val NeutralOutlineVariant = Color(0xFFC8C5D0)

val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedContainer = Color(0xFFFFDAD6)
val OnErrorRedContainer = Color(0xFF410002)

// Condition-severity badge colors — semantic (Like New/Good/Fair), not part
// of the brand palette, deliberately left untouched by the rebrand.
val ConditionLikeNew = Color(0xFF4C9A6A)
val ConditionGood = Color(0xFFE8A33D)
val ConditionFair = Color(0xFFC1653D)

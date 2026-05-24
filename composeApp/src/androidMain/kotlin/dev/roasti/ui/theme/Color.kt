package dev.roasti.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Ink palette — text / dark canvas
// ---------------------------------------------------------------------------
val Ink950 = Color(0xFF030213)
val Ink900 = Color(0xFF141416)
val Ink800 = Color(0xFF1C1D21)
val Ink700 = Color(0xFF2A2B31)
val Ink600 = Color(0xFF33343A)
val Ink500 = Color(0xFF717182)
val Ink400 = Color(0xFFA0A1B2)
val Ink100 = Color(0xFFE9EBEF)
val Ink075 = Color(0xFFECECF0)
val Ink050 = Color(0xFFEEF0F8)
val Ink025 = Color(0xFFF3F3F5)

// ---------------------------------------------------------------------------
// Stone palette — warm-neutral surfaces (light scheme canvas/inputs/chips)
// ---------------------------------------------------------------------------
val Stone50 = Color(0xFFFAFAF9)
val Stone100 = Color(0xFFF5F5F4)
val Stone200 = Color(0xFFE7E5E4)
val Stone300 = Color(0xFFD6D3D1)
val Stone400 = Color(0xFFA8A29E)
val Stone500 = Color(0xFF78716C)
val Stone600 = Color(0xFF57534E)
val Stone700 = Color(0xFF44403C)
val Stone800 = Color(0xFF292524)
val Stone900 = Color(0xFF1C1917)

// ---------------------------------------------------------------------------
// Sand palette — kept for tertiary / roast-level semantics + cream containers
// ---------------------------------------------------------------------------
val Sand50 = Color(0xFFFBF6EF)
val Sand100 = Color(0xFFF4E8D8)
val Sand200 = Color(0xFFE5D0B4)
val Sand300 = Color(0xFFD4B08A)
val Sand500 = Color(0xFFB78A5C)
val Sand600 = Color(0xFF9A7448)
val Sand700 = Color(0xFF6B4E2E)

// ---------------------------------------------------------------------------
// Supporting semantic colors
// ---------------------------------------------------------------------------
val Red50 = Color(0xFFFEF2F2)
val Red600 = Color(0xFFDC2626)
val Red700 = Color(0xFFB91C1C)
val DarkRedContainer = Color(0xFF5C2230)

// ---------------------------------------------------------------------------
// Semantic — light scheme
// ---------------------------------------------------------------------------
val LightBackground = Color(0xFFFFFFFF)
val LightForeground = Ink950
val LightCard = Color(0xFFFFFFFF)

val LightPrimary = Stone900
val LightPrimaryFg = Color(0xFFFFFFFF)
val LightPrimaryContainer = Sand100
val LightPrimaryContainerFg = Stone900

val LightSecondary = Stone100
val LightSecondaryFg = Stone900
val LightSecondaryContainer = Stone200
val LightSecondaryContainerFg = Stone900

val LightTertiary = Sand600
val LightTertiaryFg = Color(0xFFFFFFFF)
val LightTertiaryContainer = Sand100
val LightTertiaryContainerFg = Sand700

val LightMuted = Stone100
val LightMutedFg = Stone500
val LightBorder = Stone300
val LightBorderVariant = Stone200

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Stone50
val LightSurfaceContainer = Stone100
val LightSurfaceContainerHigh = Stone200
val LightSurfaceContainerHighest = Stone300

// ---------------------------------------------------------------------------
// Semantic — dark scheme
// ---------------------------------------------------------------------------
val DarkBackground = Ink900
val DarkForeground = Color(0xFFF5F5F6)
val DarkCard = Ink900

val DarkPrimary = Color(0xFFF5F0E8) // warm cream
val DarkPrimaryFg = Stone900
val DarkPrimaryContainer = Stone700
val DarkPrimaryContainerFg = Color(0xFFF5F0E8)

val DarkSecondary = Color(0xFF2E2E33)
val DarkSecondaryFg = Color(0xFFF5F5F6)
val DarkSecondaryContainer = Ink700
val DarkSecondaryContainerFg = Color(0xFFF5F5F6)

val DarkTertiary = Sand300
val DarkTertiaryFg = Color(0xFF1A140E)
val DarkTertiaryContainer = Color(0xFF4A3A2B)
val DarkTertiaryContainerFg = Sand100

val DarkSurfaceVariant = Ink700
val DarkMutedFg = Ink400
val DarkBorder = Ink600
val DarkBorderVariant = Ink700

val DarkSurfaceContainerLowest = Color(0xFF101012)
val DarkSurfaceContainerLow = Color(0xFF181A1E)
val DarkSurfaceContainer = Color(0xFF1F2126)
val DarkSurfaceContainerHigh = Color(0xFF272A2F)
val DarkSurfaceContainerHighest = Color(0xFF323439)

val DarkDestructive = Color(0xFFB64A60)

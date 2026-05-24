package dev.roasti.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightPrimaryFg,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightPrimaryContainerFg,
    secondary = LightSecondary,
    onSecondary = LightSecondaryFg,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightSecondaryContainerFg,
    tertiary = LightTertiary,
    onTertiary = LightTertiaryFg,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightTertiaryContainerFg,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightCard,
    onSurface = LightForeground,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightMutedFg,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightBorder,
    outlineVariant = LightBorderVariant,
    error = Red600,
    onError = LightPrimaryFg,
    errorContainer = Red50,
    onErrorContainer = Red700,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkPrimaryFg,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkPrimaryContainerFg,
    secondary = DarkSecondary,
    onSecondary = DarkSecondaryFg,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkSecondaryContainerFg,
    tertiary = DarkTertiary,
    onTertiary = DarkTertiaryFg,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkTertiaryContainerFg,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkForeground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedFg,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkBorder,
    outlineVariant = DarkBorderVariant,
    error = DarkDestructive,
    onError = DarkForeground,
    errorContainer = DarkRedContainer,
    onErrorContainer = Red50,
)

@Composable
fun RoastiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = RoastiTypography,
        shapes = RoastiShapes,
        content = content,
    )
}

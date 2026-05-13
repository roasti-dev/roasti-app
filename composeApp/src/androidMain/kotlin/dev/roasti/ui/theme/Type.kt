package dev.roasti.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Text sizes and line-heights from theme.css (Tailwind v4 defaults)
// --text-xs:   0.75rem  / lh = 1/0.75     = 1.333
// --text-sm:   0.875rem / lh = 1.25/0.875 = 1.429
// --text-base: 1rem     / lh = 1.5/1      = 1.5
// --text-lg:   1.125rem / lh = 1.75/1.125 = 1.556
// --text-xl:   1.25rem  / lh = 1.75/1.25  = 1.4
// --text-2xl:  1.5rem   / lh = 2/1.5      = 1.333
// --text-3xl:  1.875rem / lh = 2.25/1.875 = 1.2
// --text-4xl:  2.25rem  / lh = 2.5/2.25   = 1.111
// Font weights: normal=400, medium=500, semibold=600
// Letter spacing: tracking-tight = -0.025em

val RoastiTypography = Typography(
    // h1 — text-2xl, medium
    displayLarge = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Medium,
    ),
    // text-3xl, medium — large hero titles
    displayMedium = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.025).em,
    ),
    // h2 — text-xl, medium
    headlineLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
    ),
    // h3 — text-lg, medium
    titleLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
    ),
    // h4 / button / label — text-base, medium
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    // body — text-base, normal
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    // body — text-sm, normal
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    // body — text-xs, normal
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
    ),
    // labels — medium weight
    labelLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
    ),
)

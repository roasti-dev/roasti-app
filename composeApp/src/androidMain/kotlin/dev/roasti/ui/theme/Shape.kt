package dev.roasti.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RoastiShapes = Shapes(
    extraSmall = RoundedCornerShape(4),   // radius-xs
    small      = RoundedCornerShape(6),   // radius-sm
    medium     = RoundedCornerShape(10),   // radius-md
    large      = RoundedCornerShape(12),  // radius-lg (base)
    extraLarge = RoundedCornerShape(15),  // radius-xl
)

// Use directly where needed (e.g. bottom sheet, modal)
val ShapeXxl = RoundedCornerShape(24.dp)     // radius-3xl

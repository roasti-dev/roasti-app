package dev.roasti.ui.theme

import androidx.compose.ui.unit.dp

// --spacing: 0.25rem = 4dp (Tailwind base unit)
// Scale: spacing * n → dp values follow 4dp grid
object Spacing {
    val xs  = 4.dp   // spacing * 1
    val sm  = 8.dp   // spacing * 2
    val md  = 12.dp  // spacing * 3
    val lg  = 16.dp  // spacing * 4
    val xl  = 20.dp  // spacing * 5
    val xxl = 24.dp  // spacing * 6
    val xxxl = 32.dp // spacing * 8
    val xxxxl = 64.dp// spacing * 16
}

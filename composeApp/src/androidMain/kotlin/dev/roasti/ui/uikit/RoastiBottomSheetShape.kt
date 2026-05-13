package dev.roasti.ui.uikit

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Shared corner shape for ModalBottomSheet — only top corners rounded. */
val RoastiBottomSheetShape = RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 12.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp,
)

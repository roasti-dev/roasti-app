package dev.roasti.ui.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val PostCardKeyPrefix = "post_card_"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun postCardSharedBoundsModifier(
    postId: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return Modifier
    val surface = MaterialTheme.colorScheme.surface
    return with(sharedTransitionScope) {
        Modifier
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = "$PostCardKeyPrefix$postId"),
                animatedVisibilityScope = animatedVisibilityScope,
            )
            .background(surface)
    }
}

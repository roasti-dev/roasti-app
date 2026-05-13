package dev.roasti.ui.uikit.photoviewer

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

private const val PhotoKeyPrefix = "photo_"
private const val SharedBoundsDurationMillis = 280

/**
 * Hero/shared-element modifier for photos. Apply the same modifier (with same [imageUrl])
 * on the source thumbnail and the destination viewer image — Compose will animate bounds
 * + visuals between them when navigating.
 *
 * Uses [SharedTransitionScope.ResizeMode.ScaleToBounds] with [ContentScale.Crop] so the image
 * content fills the interpolated bounds consistently on both sides during the animation
 * (avoids the "jump" you'd otherwise see when source uses `FillWidth` and target uses `Fit`).
 *
 * Returns [Modifier] (no-op when transition scopes are unavailable, e.g. previews).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun photoSharedBoundsModifier(
    imageUrl: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return Modifier
    return with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = "$PhotoKeyPrefix$imageUrl"),
            animatedVisibilityScope = animatedVisibilityScope,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
            ),
            enter = fadeIn(tween(SharedBoundsDurationMillis)),
            exit = fadeOut(tween(SharedBoundsDurationMillis)),
        )
    }
}

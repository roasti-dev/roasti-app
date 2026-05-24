package dev.roasti.ui.uikit.photoviewer

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val PhotoKeyPrefix = "photo_"

/**
 * Hero modifier for photos. Apply with the same [imageUrl] on the source thumbnail
 * and the destination viewer image — Compose interpolates bounds and morphs the pixels
 * between them on navigation.
 *
 * Uses [SharedTransitionScope.sharedElement] (not sharedBounds) because both sides
 * render the same image bitmap; we want pixel-level morph, not container-bounds morph.
 * This avoids the crop/fit mismatch that sharedBounds + ScaleToBounds caused.
 *
 * Pass [enabled] = false to skip registration (e.g., during flick-to-dismiss, where
 * the viewer plays its own dismiss animation and the shared element would teleport
 * back to layout-fullscreen bounds before morphing).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun photoSharedElementModifier(
    imageUrl: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    enabled: Boolean = true,
): Modifier {
    if (!enabled || sharedTransitionScope == null || animatedVisibilityScope == null) {
        return Modifier
    }
    return with(sharedTransitionScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "$PhotoKeyPrefix$imageUrl"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

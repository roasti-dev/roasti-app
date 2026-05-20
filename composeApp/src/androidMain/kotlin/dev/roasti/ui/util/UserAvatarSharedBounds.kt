package dev.roasti.ui.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val UserAvatarKeyPrefix = "user_avatar_"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun userAvatarSharedElementModifier(
    tag: String?,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
): Modifier {
    if (tag == null || sharedTransitionScope == null || animatedVisibilityScope == null) {
        return Modifier
    }
    return with(sharedTransitionScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "$UserAvatarKeyPrefix$tag"),
            animatedVisibilityScope = animatedVisibilityScope,
            clipInOverlayDuringTransition = OverlayClip(CircleShape),
        )
    }
}

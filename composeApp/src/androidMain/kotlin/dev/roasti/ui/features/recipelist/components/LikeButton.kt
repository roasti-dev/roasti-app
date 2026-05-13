package dev.roasti.ui.features.recipelist.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.roasti.R

@Composable
internal fun LikeButton(
    isLiked: Boolean,
    likesCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        var skipAnimation by remember { mutableStateOf(true) }
        SideEffect { skipAnimation = false }
        val likeCountAlpha by animateFloatAsState(if(likesCount == 0) 0f else 1f)

        AnimatedContent(
            targetState = likesCount,
            transitionSpec = {
                when {
                    skipAnimation -> EnterTransition.None togetherWith ExitTransition.None
                    targetState > initialState ->
                        slideInVertically { -it } + fadeIn() togetherWith
                                slideOutVertically { it } + fadeOut()

                    else ->
                        slideInVertically { it } + fadeIn() togetherWith
                                slideOutVertically { -it } + fadeOut()
                }.using(SizeTransform(clip = false))
            },
            label = "likesCount",
        ) { count ->
            Text(
                text = count.coerceAtLeast(0).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.alpha(likeCountAlpha)
            )
        }

        var prevIsLiked by remember { mutableStateOf<Boolean?>(null) }
        val scale = remember { Animatable(1f) }
        LaunchedEffect(isLiked) {
            if (prevIsLiked == null) {   // первая композиция — пропустить
                prevIsLiked = isLiked
                return@LaunchedEffect
            }
            prevIsLiked = isLiked
            scale.animateTo(1.4f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                painter = painterResource(
                    if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outlined
                ),
                contentDescription = null,
                tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
            )
        }
    }
}

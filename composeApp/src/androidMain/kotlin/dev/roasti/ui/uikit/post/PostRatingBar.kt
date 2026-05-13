package dev.roasti.ui.uikit.post

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.roasti.R
import kotlin.math.abs
import kotlin.math.roundToInt

private const val THOUSAND_DIVISOR = 1_000L
private const val MILLION_DIVISOR = 1_000_000L
private const val BILLION_DIVISOR = 1_000_000_000L
private const val TENTH_MULTIPLIER = 10
private const val THOUSAND_SUFFIX = "K"
private const val MILLION_SUFFIX = "M"
private const val BILLION_SUFFIX = "B"

private const val BounceTotalMillis = 440
private const val BouncePeakMillis = 180
private const val BounceOvershootMillis = 320
private val BouncePeakDp = 7.dp
private val BounceOvershootDp = 2.dp
private const val CounterAnimMillis = 220

@Immutable
enum class PostUserReaction {
    UP, DOWN, NONE;

    fun isUpVote() = this == UP
    fun isDownVote() = this == DOWN
    fun isVoted() = this != NONE
}

@Immutable
data class PostRatingStateUi(
    val userReaction: PostUserReaction,
    val postRating: Int,
) {
    companion object {
        fun empty() = PostRatingStateUi(PostUserReaction.NONE, 0)
    }
}


@Composable
fun PostRatingBar(
    value: PostRatingStateUi,
    modifier: Modifier = Modifier,
    onClick: (PostUserReaction) -> Unit = {},
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UpVoteButton(value.userReaction.isUpVote()) {
            val intent = if (value.userReaction.isUpVote()) {
                PostUserReaction.NONE
            } else {
                PostUserReaction.UP
            }
            onClick(intent)
        }

        PostRatingCount(
            value.postRating,
            value.userReaction.isVoted()
        )

        DownVoteButton(value.userReaction.isDownVote()) {
            val intent = if (value.userReaction.isDownVote()) {
                PostUserReaction.NONE
            } else {
                PostUserReaction.DOWN
            }
            onClick(intent)
        }
    }
}

@Composable
private fun PostRatingCount(
    value: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant

    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val goingUp = targetState > initialState
            val slideDistance = 12
            val enter = fadeIn(tween(CounterAnimMillis)) + slideInVertically(
                tween(CounterAnimMillis),
            ) { full -> if (goingUp) -slideDistance else slideDistance }
            val exit = fadeOut(tween(CounterAnimMillis)) + slideOutVertically(
                tween(CounterAnimMillis),
            ) { full -> if (goingUp) slideDistance else -slideDistance }
            enter togetherWith exit
        },
        label = "post-rating-count",
        modifier = modifier,
    ) { animatedValue ->
        Text(
            text = formatValue(animatedValue),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

internal fun formatValue(value: Int): String {
    val absoluteValue = abs(value.toLong())
    val sign = if (value < 0) "-" else ""

    return when {
        absoluteValue < THOUSAND_DIVISOR -> value.toString()
        absoluteValue < MILLION_DIVISOR -> sign + formatCompactValue(
            absoluteValue,
            THOUSAND_DIVISOR,
            THOUSAND_SUFFIX,
        )

        absoluteValue < BILLION_DIVISOR -> sign + formatCompactValue(
            absoluteValue,
            MILLION_DIVISOR,
            MILLION_SUFFIX,
        )

        else -> sign + formatCompactValue(
            absoluteValue,
            BILLION_DIVISOR,
            BILLION_SUFFIX,
        )
    }
}

private fun formatCompactValue(
    value: Long,
    divisor: Long,
    suffix: String,
): String {
    val wholePart = value / divisor
    val decimalPart = ((value % divisor) * TENTH_MULTIPLIER) / divisor

    return if (wholePart >= 100 || decimalPart == 0L) {
        "$wholePart$suffix"
    } else {
        "$wholePart,$decimalPart$suffix"
    }
}

@Composable
private fun UpVoteButton(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    var pulseTrigger by remember { mutableIntStateOf(0) }
    val bounceModifier = rememberBounceModifier(
        trigger = pulseTrigger,
        peakDp = -BouncePeakDp,
        overshootDp = BounceOvershootDp,
    )

    Icon(
        painter = painterResource(R.drawable.ic_arrow_up),
        contentDescription = "up vote button",
        modifier = modifier
            .clip(StartCircleEndRoundedShape())
            .clickable(enabled) {
                pulseTrigger++
                onClick()
            }
            .padding(end = 4.dp, start = 5.dp, top = 5.dp, bottom = 5.dp)
            .then(bounceModifier),
        tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DownVoteButton(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    var pulseTrigger by remember { mutableIntStateOf(0) }
    val bounceModifier = rememberBounceModifier(
        trigger = pulseTrigger,
        peakDp = BouncePeakDp,
        overshootDp = -BounceOvershootDp,
    )

    Icon(
        painter = painterResource(R.drawable.ic_arrow_down),
        contentDescription = "down vote button",
        modifier = modifier
            .clip(StartRoundedEndCircleShape())
            .clickable(enabled) {
                pulseTrigger++
                onClick()
            }
            .padding(start = 4.dp, end = 5.dp, top = 5.dp, bottom = 5.dp)
            .then(bounceModifier),
        tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun rememberBounceModifier(
    trigger: Int,
    peakDp: Dp,
    overshootDp: Dp,
): Modifier {
    val density = LocalDensity.current
    val offset = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val peakPx = with(density) { peakDp.toPx() }
        val overshootPx = with(density) { overshootDp.toPx() }
        offset.snapTo(0f)
        offset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = BounceTotalMillis
                0f at 0
                peakPx at BouncePeakMillis using LinearOutSlowInEasing
                overshootPx at BounceOvershootMillis using LinearOutSlowInEasing
                0f at BounceTotalMillis using LinearOutSlowInEasing
            },
        )
    }

    return Modifier.offset { IntOffset(0, offset.value.roundToInt()) }
}

private fun StartCircleEndRoundedShape() = RoundedCornerShape(
    topStartPercent = 50,
    bottomStartPercent = 50,
    topEndPercent = 25,
    bottomEndPercent = 25,
)

private fun StartRoundedEndCircleShape() = RoundedCornerShape(
    topStartPercent = 25,
    bottomStartPercent = 25,
    topEndPercent = 50,
    bottomEndPercent = 50,
)

@Preview
@Composable
private fun RatingPreview() {
    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PostRatingCount(1300, false)
            PostRatingCount(130000, true)
            PostRatingCount(1800000, false)
            PostRatingCount(18000000, true)

        }
    }
}

@Preview
@Composable
private fun UpVotePreview() {
    MaterialTheme {
        Column {
            UpVoteButton(isSelected = true)
            UpVoteButton(isSelected = false)
        }
    }
}

@Preview
@Composable
private fun DownVotePreview() {
    MaterialTheme {
        Column {
            DownVoteButton(isSelected = true)
            DownVoteButton(isSelected = false)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PostRatingBarPreview() {
    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PostRatingBar(PostRatingStateUi(PostUserReaction.NONE, 0))
            PostRatingBar(PostRatingStateUi(PostUserReaction.UP, 156110))
            PostRatingBar(PostRatingStateUi(PostUserReaction.UP, -6110))
            PostRatingBar(PostRatingStateUi(PostUserReaction.DOWN, 101))
            PostRatingBar(PostRatingStateUi(PostUserReaction.DOWN, -10101))
        }
    }
}

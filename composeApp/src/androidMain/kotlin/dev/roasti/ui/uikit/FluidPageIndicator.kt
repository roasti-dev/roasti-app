package dev.roasti.ui.uikit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

/**
 * Worm-style page indicator with a scrolling window for many pages.
 *
 * Active dot slides continuously between page positions (uses
 * [currentPage] + [currentPageOffsetFraction]). When there are more pages
 * than [maxVisibleDots], the dot row "scrolls" so the active dot stays
 * near the center, and dots outside the window are scaled down (Telegram-style).
 *
 * Render nothing when [pageCount] <= 1.
 */
@Composable
fun FluidPageIndicator(
    pageCount: Int,
    currentPage: Int,
    currentPageOffsetFraction: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.4f),
    dotSize: Dp = 7.dp,
    spacing: Dp = 6.dp,
    maxVisibleDots: Int = 7,
) {
    if (pageCount <= 1) return

    val visibleCount = minOf(pageCount, maxVisibleDots)
    val canvasWidth = dotSize * visibleCount + spacing * (visibleCount - 1)
    val activePosition = currentPage + currentPageOffsetFraction

    Canvas(
        modifier = modifier
            .width(canvasWidth)
            .height(dotSize),
    ) {
        val dotSizePx = dotSize.toPx()
        val spacingPx = spacing.toPx()
        val step = dotSizePx + spacingPx
        val windowCenter = (visibleCount - 1) / 2f

        val scrollOffset = if (pageCount <= maxVisibleDots) {
            0f
        } else {
            val clampedActive = activePosition.coerceIn(
                windowCenter,
                pageCount - 1f - windowCenter,
            )
            (windowCenter - clampedActive) * step
        }

        val centerY = size.height / 2f

        for (i in 0 until pageCount) {
            val x = i * step + scrollOffset + dotSizePx / 2f
            if (x < -dotSizePx || x > size.width + dotSizePx) continue

            val distance = (i - activePosition).absoluteValue
            val scale = scaleForDistance(distance)
            if (scale <= 0f) continue

            drawCircle(
                color = inactiveColor,
                radius = (dotSizePx / 2f) * scale,
                center = Offset(x, centerY),
            )
        }

        val activeX = activePosition * step + scrollOffset + dotSizePx / 2f
        drawCircle(
            color = activeColor,
            radius = dotSizePx / 2f,
            center = Offset(activeX, centerY),
        )
    }
}

private fun scaleForDistance(distance: Float): Float = when {
    distance >= 3f -> 0f
    distance >= 2f -> (3f - distance) * 0.4f
    distance >= 1f -> 0.4f + (2f - distance) * 0.3f
    else -> 1f - distance * 0.3f
}

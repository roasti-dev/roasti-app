package dev.roasti.ui.uikit.photoviewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

const val MinZoomScale = 1f
const val MaxZoomScale = 5f
const val DoubleTapZoomScale = 2.5f

/**
 * State holder for a single zoomable image. Tracks scale + translation as [Animatable]s so that
 * gestures use [snapTo] (no animation) and programmatic actions use [animateTo] (spring).
 */
class ZoomableImageState {
    val scale = Animatable(MinZoomScale)
    val offsetX = Animatable(0f)
    val offsetY = Animatable(0f)

    var containerSize: IntSize = IntSize.Zero
    var contentSize: IntSize = IntSize.Zero

    val isZoomed: Boolean get() = scale.value > MinZoomScale + 0.01f

    suspend fun reset() = coroutineScope {
        launch { scale.animateTo(MinZoomScale, spring()) }
        launch { offsetX.animateTo(0f, spring()) }
        launch { offsetY.animateTo(0f, spring()) }
    }

    suspend fun zoomTo(targetScale: Float, focus: Offset) = coroutineScope {
        val clamped = targetScale.coerceIn(MinZoomScale, MaxZoomScale)
        val targetOffset = computeFocusedOffset(focus, clamped)
        launch { scale.animateTo(clamped, spring()) }
        launch { offsetX.animateTo(targetOffset.x, spring()) }
        launch { offsetY.animateTo(targetOffset.y, spring()) }
    }

    suspend fun snapBy(zoomChange: Float, panChange: Offset, centroid: Offset) {
        val oldScale = scale.value
        val newScale = (oldScale * zoomChange).coerceIn(MinZoomScale, MaxZoomScale)
        val effectiveZoomChange = newScale / oldScale

        val container = containerSize
        val center = Offset(container.width / 2f, container.height / 2f)
        val focus = centroid - center

        val oldOffset = Offset(offsetX.value, offsetY.value)
        val rawOffset = (oldOffset - focus * (effectiveZoomChange - 1f)) + panChange
        val clamped = clampOffset(rawOffset, newScale)

        scale.snapTo(newScale)
        offsetX.snapTo(clamped.x)
        offsetY.snapTo(clamped.y)
    }

    private fun computeFocusedOffset(focus: Offset, targetScale: Float): Offset {
        val container = containerSize
        val center = Offset(container.width / 2f, container.height / 2f)
        val rel = focus - center
        val raw = -rel * (targetScale - 1f)
        return clampOffset(raw, targetScale)
    }

    private fun clampOffset(offset: Offset, targetScale: Float): Offset {
        if (targetScale <= MinZoomScale) return Offset.Zero
        val container = containerSize
        val content = contentSize.takeIf { it.width > 0 && it.height > 0 } ?: container
        val scaledWidth = content.width * targetScale
        val scaledHeight = content.height * targetScale
        val maxX = ((scaledWidth - container.width) / 2f).coerceAtLeast(0f)
        val maxY = ((scaledHeight - container.height) / 2f).coerceAtLeast(0f)
        return Offset(
            x = offset.x.coerceIn(-maxX, maxX),
            y = offset.y.coerceIn(-maxY, maxY),
        )
    }
}

@Composable
fun rememberZoomableImageState(): ZoomableImageState = remember { ZoomableImageState() }

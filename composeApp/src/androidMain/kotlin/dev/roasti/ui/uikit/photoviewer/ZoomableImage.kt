package dev.roasti.ui.uikit.photoviewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch

/**
 * Single zoomable image. Handles:
 * - pinch-to-zoom around finger centroid (via custom multi-touch gesture loop).
 * - double-tap toggle between 1x and [DoubleTapZoomScale].
 * - single-tap routed to [onSingleTap].
 *
 * Pan is allowed only when zoomed past 1x or while pinching, and is clamped to image bounds.
 */
@Composable
fun ZoomableImage(
    url: String,
    state: ZoomableImageState,
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit = {},
    contentModifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { state.containerSize = it }
            .pointerInput(state) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { tapPoint ->
                        scope.launch {
                            if (state.isZoomed) {
                                state.reset()
                            } else {
                                state.zoomTo(DoubleTapZoomScale, tapPoint)
                            }
                        }
                    },
                )
            }
            .pointerInput(state) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)
                        val isPinching = event.changes.size > 1
                        val shouldHandlePan = state.isZoomed || isPinching
                        if (zoomChange != 1f || (shouldHandlePan && panChange != Offset.Zero)) {
                            scope.launch {
                                state.snapBy(
                                    zoomChange = zoomChange,
                                    panChange = if (shouldHandlePan) panChange else Offset.Zero,
                                    centroid = centroid,
                                )
                            }
                            if (state.isZoomed || isPinching) {
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = contentModifier
                .fillMaxSize()
                .onGloballyPositioned { coords -> state.contentSize = coords.size }
                .graphicsLayer {
                    scaleX = state.scale.value
                    scaleY = state.scale.value
                    translationX = state.offsetX.value
                    translationY = state.offsetY.value
                },
        ) {
            when (painter.state.value) {
                is AsyncImagePainter.State.Loading,
                is AsyncImagePainter.State.Empty,
                is AsyncImagePainter.State.Error -> Unit

                else -> SubcomposeAsyncImageContent()
            }
        }
    }
}

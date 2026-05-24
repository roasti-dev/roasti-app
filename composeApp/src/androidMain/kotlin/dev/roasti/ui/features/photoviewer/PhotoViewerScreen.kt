package dev.roasti.ui.features.photoviewer

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.roasti.R
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.photoviewer.photoSharedElementModifier
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.flick.FlickToDismiss
import me.saket.telephoto.flick.FlickToDismissState
import me.saket.telephoto.flick.rememberFlickToDismissState
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

/**
 * Fullscreen photo viewer.
 *
 * Responsibilities (SRP):
 * - own pager + flick-to-dismiss state
 * - drive system bars immersive mode
 * - delegate zoom/pan to Telephoto
 * - delegate hero animation to Compose shared element via [photoSharedElementModifier]
 *
 * The shared element key only registers on the page that matches the source caller
 * (i.e. [initialIndex]). Other pager pages animate on a plain fade — they have no
 * matching source in the previous screen. During an active flick gesture the shared
 * element is skipped so the flick animation plays out cleanly.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalTelephotoApi::class)
@Composable
fun PhotoViewerScreen(
    images: List<String>,
    initialIndex: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    if (images.isEmpty()) {
        DisposableEffect(Unit) {
            onClose()
            onDispose { }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size },
    )
    val flickState = rememberFlickToDismissState(rotateOnDrag = false)
    val isFlickActive = flickState.gestureState !is FlickToDismissState.GestureState.Idle

    LaunchedEffect(flickState) {
        snapshotFlowDismissed(flickState).collect { dismissed -> if (dismissed) onClose() }
    }

    var barsVisible by remember { mutableStateOf(true) }
    SystemBarsImmersiveEffect(barsVisible)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AnimatedVisibility(
                visible = barsVisible && !isFlickActive,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                PhotoViewerTopBar(
                    onClose = onClose,
                    currentPage = pagerState.currentPage,
                    pageCount = images.size,
                )
            }
        },
    ) {
        FlickToDismiss(
            state = flickState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            PhotoPager(
                images = images,
                pagerState = pagerState,
                initialIndex = initialIndex,
                isFlickActive = isFlickActive,
                onSingleTap = { barsVisible = !barsVisible },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PhotoPager(
    images: List<String>,
    pagerState: PagerState,
    initialIndex: Int,
    isFlickActive: Boolean,
    onSingleTap: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { page ->
        val url = images[page]
        val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = MaxZoomFactor))
        val zoomableImageState = rememberZoomableImageState(zoomableState)

        if (pagerState.settledPage != page) {
            LaunchedEffect(Unit) {
                zoomableState.resetZoom(animationSpec = SnapSpec())
            }
        }

        ZoomableAsyncImage(
            model = url,
            contentDescription = null,
            state = zoomableImageState,
            onClick = { onSingleTap() },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (page == initialIndex) {
                        photoSharedElementModifier(
                            imageUrl = url,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            enabled = !isFlickActive,
                        )
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

@Composable
private fun PhotoViewerTopBar(
    onClose: () -> Unit,
    currentPage: Int,
    pageCount: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = TopBarScrimAlpha))
            .statusBarsPadding()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                tint = Color.White,
            )
        }
        if (pageCount > 1) {
            Text(
                text = "${currentPage + 1} / $pageCount",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun SystemBarsImmersiveEffect(barsVisible: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window ?: return
    val controller = remember(view) { WindowCompat.getInsetsController(window, view) }

    DisposableEffect(Unit) {
        val previousBehavior = controller.systemBarsBehavior
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.systemBarsBehavior = previousBehavior
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(barsVisible) {
        if (barsVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }
}

private fun snapshotFlowDismissed(state: FlickToDismissState) =
    androidx.compose.runtime.snapshotFlow {
        state.gestureState is FlickToDismissState.GestureState.Dismissed
    }

private const val MaxZoomFactor = 5f
private const val TopBarScrimAlpha = 0.5f

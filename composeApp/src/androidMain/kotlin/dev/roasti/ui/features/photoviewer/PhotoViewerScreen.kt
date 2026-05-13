package dev.roasti.ui.features.photoviewer

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.roasti.R
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.photoviewer.ZoomableImagePager
import dev.roasti.ui.uikit.photoviewer.photoSharedBoundsModifier

@OptIn(ExperimentalSharedTransitionApi::class)
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
        // Defensive guard — empty input means the caller passed nothing useful.
        // Pop immediately to avoid showing a black screen forever.
        DisposableEffect(Unit) {
            onClose()
            onDispose { }
        }
        return
    }

    var barsVisible by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size },
    )
    SystemBarsImmersiveEffect(barsVisible)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
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
                    if (images.size > 1) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${images.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        },
    ) {
        ZoomableImagePager(
            images = images,
            pagerState = pagerState,
            onClose = onClose,
            onSingleTap = { barsVisible = !barsVisible },
            pageContentModifier = { url ->
                photoSharedBoundsModifier(
                    imageUrl = url,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SystemBarsImmersiveEffect(barsVisible: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window ?: return
    val controller = remember(view) { WindowCompat.getInsetsController(window, view) }

    DisposableEffect(Unit) {
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
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

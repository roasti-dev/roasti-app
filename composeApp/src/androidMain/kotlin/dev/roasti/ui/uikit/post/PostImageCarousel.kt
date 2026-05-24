package dev.roasti.ui.uikit.post

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.FluidPageIndicator

/**
 * Image carousel for posts. Single image renders as a plain [PostImageCard]
 * (no pager overhead, no indicator). Multiple images render in a [HorizontalPager]
 * with a [FluidPageIndicator] overlaid at bottom-center.
 *
 * [pageModifier] is applied per-page so callers can wire shared element transitions
 * (or any per-index decoration) without leaking pager internals.
 */
@Composable
fun PostImageCarousel(
    images: List<String>,
    onImageClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    pageModifier: @Composable (index: Int) -> Modifier = { Modifier },
) {
    if (images.isEmpty()) return

    if (images.size == 1) {
        PostImageCard(
            fullUrl = images[0],
            modifier = modifier
                .fillMaxWidth()
                .then(pageModifier(0))
                .pointerInput(Unit) { detectTapGestures { onImageClick(0) } },
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { images.size })

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            PostImageCard(
                fullUrl = images[page],
                modifier = Modifier
                    .fillMaxSize()
                    .then(pageModifier(page))
                    .pointerInput(page) { detectTapGestures { onImageClick(page) } },
            )
        }

        FluidPageIndicator(
            pageCount = images.size,
            currentPage = pagerState.currentPage,
            currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.sm),
        )
    }
}

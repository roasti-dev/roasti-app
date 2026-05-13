package dev.roasti.ui.uikit.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import dev.roasti.ui.uikit.ErrorStub
import dev.roasti.ui.uikit.LoadingStub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ContentScaffold(
    state: ContentUiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    events: Flow<UiEvent>? = null,
    content: @Composable (T) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    if (events != null) {
        EventEffect(events, snackbarHostState)
    }

    Box(modifier.fillMaxSize()) {
        when (state) {
            ContentUiState.Loading -> LoadingStub()

            is ContentUiState.FullscreenError -> ErrorStub(
                error = state.error,
                onRetry = onRetry,
            )

            is ContentUiState.Content -> if (onRefresh != null) {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    content(state.data)
                }
            } else {
                content(state.data)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

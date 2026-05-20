package dev.roasti.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.roasti.ui.features.feed.FeedScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedRoute(
    contentPadding: PaddingValues = PaddingValues(),
    onPostClick: (String) -> Unit = {},
    onCreatePost: () -> Unit = {},
    onEditPost: (String) -> Unit = {},
    onAuthorClick: (userId: String, username: String, avatarTag: String?) -> Unit = { _, _, _ -> },
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    FeedScreen(
        contentPadding = contentPadding,
        onPostClick = onPostClick,
        onCreatePost = onCreatePost,
        onEditPost = onEditPost,
        onAuthorClick = onAuthorClick,
        onImageClick = onImageClick,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(contentPadding),
    )
}

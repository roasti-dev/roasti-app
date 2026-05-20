package dev.roasti.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.roasti.ui.features.postdetail.PostDetailScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostDetailRoute(
    postId: String,
    onClose: () -> Unit,
    onEditPost: (String) -> Unit = {},
    onAuthorClick: (userId: String, username: String, avatarTag: String?) -> Unit = { _, _, _ -> },
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    PostDetailScreen(
        postId = postId,
        onClose = onClose,
        onEditPost = onEditPost,
        onAuthorClick = onAuthorClick,
        onImageClick = onImageClick,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = Modifier.fillMaxSize(),
    )
}

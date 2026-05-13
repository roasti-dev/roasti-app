package dev.roasti.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.roasti.ui.features.postcompose.PostComposeScreen

@Composable
fun PostComposeRoute(
    postId: String?,
    onClose: () -> Unit,
) {
    PostComposeScreen(
        postId = postId,
        onClose = onClose,
        modifier = Modifier.fillMaxSize(),
    )
}

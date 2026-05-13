package dev.roasti.ui.uikit.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.roasti.ui.theme.RoastiTheme
import dev.roasti.ui.uikit.AsyncImagePreviewProvider

@Composable
fun PostImageCard(fullUrl: String, modifier: Modifier = Modifier, fallbackRatio: Float = 4f / 3f) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(fullUrl)
            .crossfade(true)
            .build(),
        contentDescription = "post image",
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small),
        contentScale = ContentScale.FillWidth,
    ) {
        val state by painter.state.collectAsState()
        when (state) {
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Empty -> Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(fallbackRatio)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            else -> SubcomposeAsyncImageContent()
        }
    }
}

@Preview(showBackground = true, device = Devices.PHONE)
@Composable
private fun PostImageCardPreview() {
    RoastiTheme {
        AsyncImagePreviewProvider {
            PostImageCard(
                fullUrl = "", Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(40.dp)
            )
        }
    }
}
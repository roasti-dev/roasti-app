package dev.roasti.ui.uikit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Aspect ratio format for [ImageComponent].
 */
enum class ImageFormat(val ratio: Float) {
    Square(1f / 1f),
    Landscape4x3(4f / 3f),
    Landscape16x9(16f / 9f),
    Portrait3x4(3f / 4f),
    Portrait2x3(2f / 3f),
}

/**
 * Constrains image by one axis; aspect ratio is maintained via [ImageFormat].
 */
sealed interface ImageSize {
    /** Fixed width; height is derived from [ImageFormat.ratio]. */
    @JvmInline
    value class FixedWidth(val value: Dp) : ImageSize

    /** Fixed height; width is derived from [ImageFormat.ratio]. */
    @JvmInline
    value class FixedHeight(val value: Dp) : ImageSize

    /** Fills available width; height is derived from [ImageFormat.ratio]. */
    data object FillWidth : ImageSize
}

@Composable
fun ImageComponent(
    url: String?,
    format: ImageFormat,
    size: ImageSize,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    contentDescription: String? = null,
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val resolvedModifier = when (size) {
        is ImageSize.FixedWidth -> modifier
            .width(size.value)
            .aspectRatio(format.ratio)

        is ImageSize.FixedHeight -> modifier
            .height(size.value)
            .aspectRatio(format.ratio, matchHeightConstraintsFirst = true)

        ImageSize.FillWidth -> modifier
            .fillMaxWidth()
            .aspectRatio(format.ratio)
    }

    val finalModifier = if (shape != null) {
        resolvedModifier.clip(shape)
    } else {
        resolvedModifier
    }.background(surfaceVariant)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = finalModifier,
        contentScale = ContentScale.Crop,
    )
}

package dev.roasti.ui.features.recipelist.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.roasti.ui.theme.Spacing

internal val RecipeTileSize = 56.dp

/**
 * Ведущий тайл рецепта во flat-ряду списка.
 * Есть фото — crop. Нет — брендовый sand-тайл с цветной flat-иконкой brew-метода.
 */
@Composable
internal fun RecipeLeadingTile(
    imageUrl: String?,
    @DrawableRes brewMethodIconRes: Int,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = imageModifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(brewMethodIconRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.sm),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

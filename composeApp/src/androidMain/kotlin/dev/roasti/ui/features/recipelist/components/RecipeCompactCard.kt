package dev.roasti.ui.features.recipelist.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.roasti.R
import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel
import dev.roasti.ui.theme.RoastiTheme
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.AsyncImagePreviewProvider

internal val CompactCardSize = 152.dp
private val CompactCardIconSize = 56.dp

/**
 * Квадратная favorites-карточка (V2 split): картинка/brew-icon сверху + футер title + meta + like.
 * В одном flat-стиле со списком: без elevation, hairline-border, sand-плейсхолдер.
 */
@Composable
internal fun RecipeCompactCard(
    item: RecipeListItemUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (item.imageUrl != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Image(
                        painter = painterResource(item.brewMethodIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(CompactCardIconSize),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(item.brewMethodLabelRes) +
                            " · " + stringResource(item.difficultyLabelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    LikeButton(
                        isLiked = item.isLiked,
                        likesCount = item.likesCount,
                        onClick = onLikeClick,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RecipeCompactCardPreview() {
    RoastiTheme {
        AsyncImagePreviewProvider {
            RecipeCompactCard(
                item = RecipeListItemUiModel(
                    id = "1",
                    title = "Some tasty coffee recipe",
                    description = "Clean and bright with floral notes",
                    imageUrl = null,
                    brewMethodLabelRes = R.string.recipe_brew_method_v60,
                    brewMethodIconRes = R.drawable.ic_brew_v60,
                    difficultyLabelRes = R.string.recipe_difficulty_easy,
                    isLiked = false,
                    likesCount = 42,
                ),
                modifier = Modifier.size(CompactCardSize),
            )
        }
    }
}

package dev.roasti.ui.features.recipelist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel
import dev.roasti.ui.theme.RoastiTheme
import dev.roasti.ui.uikit.AsyncImagePreviewProvider
import dev.roasti.ui.uikit.ImageComponent
import dev.roasti.ui.uikit.ImageFormat
import dev.roasti.ui.uikit.ImageSize

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
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            ImageComponent(
                url = item.imageUrl,
                format = ImageFormat.Landscape4x3,
                size = ImageSize.FillWidth,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(item.brewMethodLabelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                LikeButton(item.isLiked, item.likesCount, onLikeClick)
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
                    difficultyLabelRes = R.string.recipe_difficulty_easy,
                    isLiked = false,
                    likesCount = 42,
                ),
            )
        }
    }
}

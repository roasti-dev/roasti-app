package dev.roasti.ui.features.recipelist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
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
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.AsyncImagePreviewProvider

// divider начинается от текста, под тайлом не идёт: lg + tile + md = 16+56+12
private val RecipeDividerInset = Spacing.lg + RecipeTileSize + Spacing.md

@Composable
internal fun RecipeCard(
    item: RecipeListItemUiModel,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecipeLeadingTile(
                imageUrl = item.imageUrl,
                brewMethodIconRes = item.brewMethodIconRes,
                modifier = Modifier.size(RecipeTileSize),
                imageModifier = imageModifier,
            )
            Spacer(Modifier.width(Spacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = recipeMetaLine(
                        brewMethod = stringResource(item.brewMethodLabelRes),
                        difficulty = stringResource(item.difficultyLabelRes),
                        description = item.description,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            LikeButton(
                isLiked = item.isLiked,
                likesCount = item.likesCount,
                onClick = onLikeClick,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = RecipeDividerInset),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        )
    }
}

// "V60 · Easy · floral, citrus" — пропускает пустые части
private fun recipeMetaLine(
    brewMethod: String,
    difficulty: String,
    description: String,
): String = listOf(brewMethod, difficulty, description)
    .filter { it.isNotBlank() }
    .joinToString(" · ")

@Preview
@Composable
private fun RecipeCardPreview() {
    RoastiTheme {
        AsyncImagePreviewProvider {
            RecipeCard(
                item = RecipeListItemUiModel(
                    id = "1",
                    title = "Ethiopia V60 Bright",
                    description = "floral, citrus, tea-like",
                    imageUrl = null,
                    brewMethodLabelRes = R.string.recipe_brew_method_v60,
                    brewMethodIconRes = R.drawable.ic_brew_v60,
                    difficultyLabelRes = R.string.recipe_difficulty_easy,
                    isLiked = true,
                    likesCount = 42,
                ),
                onLikeClick = {},
            )
        }
    }
}

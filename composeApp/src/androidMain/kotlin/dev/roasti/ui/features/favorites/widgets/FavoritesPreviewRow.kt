package dev.roasti.ui.features.favorites.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowRight
import dev.roasti.R
import dev.roasti.ui.features.favorites.model.FavoritesPreviewState
import dev.roasti.ui.features.recipelist.components.CompactCardSize
import dev.roasti.ui.features.recipelist.components.RecipeCompactCard
import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.LoadingStub
import dev.roasti.ui.uikit.TextCard

private val PreviewCardSize = CompactCardSize

@Composable
fun FavoritesPreviewRow(
    state: FavoritesPreviewState,
    onItemClick: (RecipeListItemUiModel) -> Unit,
    onLikeClick: (RecipeListItemUiModel) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = Spacing.lg,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = horizontalPadding),
    ) {
        when (state) {
            FavoritesPreviewState.Loading -> item(key = "favorites_preview_loading") {
                Box(
                    modifier = Modifier.size(PreviewCardSize),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingStub()
                }
            }

            FavoritesPreviewState.Empty -> item(key = "favorites_preview_empty") {
                TextCard(
                    text = stringResource(R.string.recipe_list_favorite_empty_state),
                    modifier = Modifier.size(PreviewCardSize),
                )
            }

            is FavoritesPreviewState.Content -> {
                items(items = state.items, key = { it.id }) { item ->
                    RecipeCompactCard(
                        item = item,
                        modifier = Modifier.size(PreviewCardSize),
                        onClick = { onItemClick(item) },
                        onLikeClick = { onLikeClick(item) },
                    )
                }
                if (state.hasMore) {
                    item(key = "favorites_preview_see_all") {
                        SeeAllCard(onClick = onSeeAllClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeeAllCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.size(PreviewCardSize),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.ArrowRight,
                contentDescription = stringResource(R.string.favorites_see_all),
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

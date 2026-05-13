package dev.roasti.ui.features.profile.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.features.favorites.model.FavoritesPreviewState
import dev.roasti.ui.features.favorites.widgets.FavoritesPreviewRow
import dev.roasti.ui.features.recipelist.model.RecipeListItemUiModel

@Composable
fun ProfileFavoriteRecipesRow(
    item: FavoritesPreviewState,
    onRecipeClick: (RecipeListItemUiModel) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPaddings: Dp = 16.dp,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.recipe_list_favorite_section_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPaddings),
        )
        FavoritesPreviewRow(
            state = item,
            onItemClick = onRecipeClick,
            onLikeClick = {},
            onSeeAllClick = onSeeAllClick,
            horizontalPadding = horizontalPaddings,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

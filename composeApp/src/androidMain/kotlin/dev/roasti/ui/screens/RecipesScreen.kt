package dev.roasti.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.roasti.ui.features.recipelist.RecipesListScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RecipesRoute(
    contentPadding: PaddingValues = PaddingValues(),
    onRecipeClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    onSeeAllFavorites: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        RecipesListScreen(
            onRecipeClick = onRecipeClick,
            onCreateClick = onCreateClick,
            onSeeAllFavorites = onSeeAllFavorites,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            contentPadding = contentPadding,
        )
    }
}

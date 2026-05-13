package dev.roasti.ui.features.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import dev.roasti.ui.features.profile.widgets.ProfileFavoriteRecipesRow
import dev.roasti.ui.features.profile.widgets.ProfileHeaderRow
import dev.roasti.ui.features.profile.widgets.StatisticsRow
import dev.roasti.ui.uikit.state.ContentScaffold

@Composable
internal fun ProfileRoute(
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onRecipeClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val viewModel: ProfileViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val listener = remember(viewModel, onNavigateToSettings) {
        object : ProfileRowListener {
            override fun onImagePicked(fileName: String, bytes: ByteArray) =
                viewModel.onImagePicked(fileName, bytes)

            override fun onEditClick() = viewModel.onEditClick()
            override fun onSettingsClick() = onNavigateToSettings()
            override fun onLogoutClick() = viewModel.onLogoutClick()
        }
    }

    ContentScaffold(
        state = state,
        onRetry = viewModel::retry,
        onRefresh = viewModel::retry,
        events = viewModel.events,
    ) { profile ->
        ProfileScreen(
            uiState = profile,
            listener = listener,
            onRecipeClick = onRecipeClick,
            onSeeAllFavorites = onNavigateToFavorites,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun ProfileScreen(
    uiState: ProfileState,
    listener: ProfileRowListener,
    onRecipeClick: (String) -> Unit,
    onSeeAllFavorites: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        contentWindowInsets = WindowInsets(0),
    ) { innerPaddings ->
        Column(
            Modifier.padding(
                top = innerPaddings.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            )
        ) {
            ProfileHeaderRow(
                userUiModel = uiState.user,
                listener = listener,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            StatisticsRow(
                item = uiState.statistics,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .padding(16.dp)
            )
            ProfileFavoriteRecipesRow(
                item = uiState.favoritesState,
                onRecipeClick = { onRecipeClick(it.id) },
                onSeeAllClick = onSeeAllFavorites,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalPaddings = 16.dp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileRootPreview() {
    ProfileScreen(
        uiState = ProfileState(),
        listener = object : ProfileRowListener {
            override fun onImagePicked(fileName: String, bytes: ByteArray) {}
            override fun onEditClick() {}
            override fun onSettingsClick() {}
            override fun onLogoutClick() {}
        },
        onRecipeClick = {},
        onSeeAllFavorites = {},
    )
}

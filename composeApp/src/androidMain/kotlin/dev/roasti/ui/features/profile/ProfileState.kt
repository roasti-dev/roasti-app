package dev.roasti.ui.features.profile

import androidx.compose.runtime.Immutable
import dev.roasti.ui.features.favorites.model.FavoritesPreviewState

@Immutable
data class ProfileState(
    val user: ProfileUserUiModel = ProfileUserUiModel.empty(),
    val statistics: ProfileStatisticsUiModel = ProfileStatisticsUiModel.empty(),
    val favoritesState: FavoritesPreviewState = FavoritesPreviewState.Empty,
) {
    companion object {
        fun empty() = ProfileState()
    }
}


data class ProfileUserUiModel(
    val imageId: String? = null,
    val nickname: String,
    val bio: String? = null,
    val email: String? = null,
    val isImageUploadInProgress: Boolean = false,
) {
    companion object {
        fun empty() = ProfileUserUiModel(nickname = "")
    }
}

data class ProfileStatisticsUiModel(
    val brewsCount: Int = 0,
    val postCount: Int = 0,
    val isLoading: Boolean = false,
) {
    companion object {
        fun empty() = ProfileStatisticsUiModel()
        fun loading() = ProfileStatisticsUiModel(isLoading = true)
        fun content(brewsCount: Int, postCount: Int) = ProfileStatisticsUiModel(
            brewsCount = brewsCount,
            postCount = postCount,
        )
    }
}
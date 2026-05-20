package dev.roasti.ui.features.userprofile.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfileUiModel(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val bio: String?,
)

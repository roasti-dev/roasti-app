package dev.roasti.ui.features.userprofile.mapper

import dev.roasti.core.utils.imageUrl
import dev.roasti.feature.auth.domain.model.PublicUserProfile
import dev.roasti.ui.features.userprofile.model.UserProfileUiModel

fun PublicUserProfile.toUiModel(): UserProfileUiModel = UserProfileUiModel(
    id = id,
    displayName = name?.takeIf { it.isNotBlank() } ?: username,
    username = username,
    avatarUrl = avatarId?.let { imageUrl(it) },
    bio = bio?.takeIf { it.isNotBlank() },
)

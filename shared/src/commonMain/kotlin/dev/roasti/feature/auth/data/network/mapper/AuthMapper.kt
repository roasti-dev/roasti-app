package dev.roasti.feature.auth.data.network.mapper

import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import dev.roasti.feature.auth.data.network.model.response.RefreshResponseDto
import dev.roasti.feature.auth.data.network.model.response.UserDto
import dev.roasti.feature.auth.domain.model.User
import dev.roasti.core.session.UserSession
import dev.roasti.feature.auth.data.network.model.response.PublicUserDto
import dev.roasti.feature.auth.domain.model.PublicUserProfile

fun UserDto.toDomain(): User = User(
    avatarId = avatarId,
    bio = bio,
    id = id,
    username = username,
    email = email,
)

fun AuthResponseDto.toDomain(): UserSession = UserSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
)

fun RefreshResponseDto.toDomain(): UserSession = UserSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
)

fun PublicUserDto.toDomain() = PublicUserProfile(avatarId, bio, id, name, username)
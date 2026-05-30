package dev.roasti.feature.auth

import dev.roasti.feature.auth.domain.repository.AuthRepository

@Throws(Throwable::class)
suspend fun AuthRepository.loginOrThrow(username: String, password: String) {
    login(username = username, password = password).getOrThrow()
}

@Throws(Throwable::class)
suspend fun AuthRepository.registerOrThrow(
    username: String,
    email: String,
    password: String,
    bio: String?,
    avatarId: String?,
) {
    register(
        username = username,
        email = email,
        password = password,
        bio = bio,
        avatarId = avatarId,
    ).getOrThrow()
}

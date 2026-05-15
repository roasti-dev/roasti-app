package dev.roasti.feature.auth.data.network.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class LogoutRequestDto(@SerialName("refresh_token") val refreshToken: String)
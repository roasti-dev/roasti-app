package dev.roasti.feature.auth.data.network.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsernameAvailabilityResponseDto(@SerialName("available") val available: Boolean)

package dev.roasti.feature.recipe.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RoastLevelDto {
    @SerialName("light")
    LIGHT,

    @SerialName("medium_light")
    MEDIUM_LIGHT,

    @SerialName("medium")
    MEDIUM,

    @SerialName("medium_dark")
    MEDIUM_DARK,

    @SerialName("dark")
    DARK,

    @SerialName("none")
    NONE,
}

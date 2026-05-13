package dev.roasti.feature.recipe.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BrewMethodDto {
    @SerialName("v60")
    V60,

    @SerialName("french_press")
    FRENCH_PRESS,

    @SerialName("aeropress")
    AEROPRESS,

    @SerialName("chemex")
    CHEMEX,

    @SerialName("cold_brew")
    COLD_BREW,

    @SerialName("expresso_machine")
    EXPRESSO_MACHINE,

    @SerialName("moka_pot")
    MOKA_POT,

    @SerialName("none")
    NONE,
}

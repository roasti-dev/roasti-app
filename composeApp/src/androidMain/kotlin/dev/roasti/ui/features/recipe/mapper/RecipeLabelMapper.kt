package dev.roasti.ui.features.recipe.mapper

import androidx.annotation.StringRes
import dev.roasti.R
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel

@StringRes
internal fun BrewMethod.labelRes(): Int = when (this) {
    BrewMethod.V60 -> R.string.recipe_brew_method_v60
    BrewMethod.FrenchPress -> R.string.recipe_brew_method_french_press
    BrewMethod.Aeropress -> R.string.recipe_brew_method_aeropress
    BrewMethod.Chemex -> R.string.recipe_brew_method_chemex
    BrewMethod.ColdBrew -> R.string.recipe_brew_method_cold_brew
    BrewMethod.EspressoMachine -> R.string.recipe_brew_method_espresso_machine
    BrewMethod.MokaPot -> R.string.recipe_brew_method_moka_pot
    BrewMethod.NONE -> R.string.recipe_missing_value
}

@StringRes
internal fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.Easy -> R.string.recipe_difficulty_easy
    Difficulty.Medium -> R.string.recipe_difficulty_medium
    Difficulty.Hard -> R.string.recipe_difficulty_hard
}

@StringRes
internal fun RoastLevel.labelRes(): Int = when (this) {
    RoastLevel.Light -> R.string.recipe_roast_level_light
    RoastLevel.MediumLight -> R.string.recipe_roast_level_medium_light
    RoastLevel.Medium -> R.string.recipe_roast_level_medium
    RoastLevel.MediumDark -> R.string.recipe_roast_level_medium_dark
    RoastLevel.Dark -> R.string.recipe_roast_level_dark
    RoastLevel.NONE -> R.string.recipe_missing_value
}

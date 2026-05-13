package dev.roasti.core.database

import app.cash.sqldelight.ColumnAdapter
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel

internal val brewMethodColumnAdapter = object : ColumnAdapter<BrewMethod, String> {
    override fun decode(databaseValue: String): BrewMethod = when (databaseValue) {
        "V60" -> BrewMethod.V60
        "FRENCH_PRESS", "FrenchPress" -> BrewMethod.FrenchPress
        "AEROPRESS", "Aeropress" -> BrewMethod.Aeropress
        "CHEMEX", "Chemex" -> BrewMethod.Chemex
        "COLD_BREW", "ColdBrew" -> BrewMethod.ColdBrew
        "EXPRESSO_MACHINE", "EspressoMachine" -> BrewMethod.EspressoMachine
        "MOKA_POT", "MokaPot" -> BrewMethod.MokaPot
        "NONE" -> BrewMethod.NONE
        else -> error("Unknown brew method value: $databaseValue")
    }

    override fun encode(value: BrewMethod): String = when (value) {
        BrewMethod.V60 -> "V60"
        BrewMethod.FrenchPress -> "FRENCH_PRESS"
        BrewMethod.Aeropress -> "AEROPRESS"
        BrewMethod.Chemex -> "CHEMEX"
        BrewMethod.ColdBrew -> "COLD_BREW"
        BrewMethod.EspressoMachine -> "EXPRESSO_MACHINE"
        BrewMethod.MokaPot -> "MOKA_POT"
        BrewMethod.NONE -> "NONE"
    }
}

internal val difficultyColumnAdapter = object : ColumnAdapter<Difficulty, String> {
    override fun decode(databaseValue: String): Difficulty = when (databaseValue) {
        "EASY", "Easy" -> Difficulty.Easy
        "MEDIUM", "Medium" -> Difficulty.Medium
        "HARD", "Hard" -> Difficulty.Hard
        else -> error("Unknown difficulty value: $databaseValue")
    }

    override fun encode(value: Difficulty): String = when (value) {
        Difficulty.Easy -> "EASY"
        Difficulty.Medium -> "MEDIUM"
        Difficulty.Hard -> "HARD"
    }
}

internal val roastLevelColumnAdapter = object : ColumnAdapter<RoastLevel, String> {
    override fun decode(databaseValue: String): RoastLevel = when (databaseValue) {
        "LIGHT", "Light" -> RoastLevel.Light
        "MEDIUM_LIGHT", "MediumLight" -> RoastLevel.MediumLight
        "MEDIUM", "Medium" -> RoastLevel.Medium
        "MEDIUM_DARK", "MediumDark" -> RoastLevel.MediumDark
        "DARK", "Dark" -> RoastLevel.Dark
        "NONE" -> RoastLevel.NONE
        else -> error("Unknown roast level value: $databaseValue")
    }

    override fun encode(value: RoastLevel): String = when (value) {
        RoastLevel.Light -> "LIGHT"
        RoastLevel.MediumLight -> "MEDIUM_LIGHT"
        RoastLevel.Medium -> "MEDIUM"
        RoastLevel.MediumDark -> "MEDIUM_DARK"
        RoastLevel.Dark -> "DARK"
        RoastLevel.NONE -> "NONE"
    }
}

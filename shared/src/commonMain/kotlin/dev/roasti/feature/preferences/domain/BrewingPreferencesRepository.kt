package dev.roasti.feature.preferences.domain

import kotlinx.coroutines.flow.StateFlow

interface BrewingPreferencesRepository {
    val preferences: StateFlow<BrewingPreferences>

    suspend fun setAutoAdvance(enabled: Boolean)
}

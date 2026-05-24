package dev.roasti.feature.preferences.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dev.roasti.feature.preferences.domain.BrewingPreferences
import dev.roasti.feature.preferences.domain.BrewingPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.userPreferencesDataStore by preferencesDataStore(name = "roasti_user_prefs")

internal class BrewingPreferencesRepositoryImpl(
    context: Context,
) : BrewingPreferencesRepository {

    private val dataStore = context.userPreferencesDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val preferences: StateFlow<BrewingPreferences> = dataStore.data
        .map { prefs ->
            BrewingPreferences(
                autoAdvance = prefs[AUTO_ADVANCE_KEY] ?: DEFAULT_AUTO_ADVANCE,
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = BrewingPreferences(autoAdvance = DEFAULT_AUTO_ADVANCE),
        )

    override suspend fun setAutoAdvance(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_ADVANCE_KEY] = enabled
        }
    }

    private companion object {
        const val DEFAULT_AUTO_ADVANCE = true
        val AUTO_ADVANCE_KEY = booleanPreferencesKey("brewing.auto_advance")
    }
}

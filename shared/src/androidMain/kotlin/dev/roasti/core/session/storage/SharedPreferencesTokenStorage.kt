package dev.roasti.core.session.storage

import android.content.Context
import kotlinx.serialization.json.Json
import dev.roasti.core.session.storage.TokenStorage
import dev.roasti.core.session.storage.TokensDto

private const val PreferencesName = "roasti_auth"
private const val TokensKey = "tokens"

class SharedPreferencesTokenStorage(
    context: Context,
) : TokenStorage {

    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun readTokens(): TokensDto? {
        val raw = preferences.getString(TokensKey, null) ?: return null
        return runCatching { json.decodeFromString<TokensDto>(raw) }.getOrNull()
    }

    override suspend fun writeTokens(tokens: TokensDto) {
        preferences.edit()
            .putString(TokensKey, json.encodeToString(TokensDto.serializer(), tokens))
            .apply()
    }

    override suspend fun clearTokens() {
        preferences.edit()
            .remove(TokensKey)
            .apply()
    }
}

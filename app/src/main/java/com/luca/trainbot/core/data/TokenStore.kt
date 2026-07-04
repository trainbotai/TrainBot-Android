package com.luca.trainbot.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trainbot_auth")

/**
 * Persists auth tokens in Jetpack DataStore (Preferences), encrypted at rest
 * with an Android Keystore key (see [TokenCrypto]) — DataStore alone is
 * plain-text and would otherwise leak tokens via Auto Backup / adb backup.
 */
class TokenStore(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_TENANT_ID = stringPreferencesKey("tenant_id")
    }

    private fun Preferences.decrypted(key: Preferences.Key<String>): String? =
        this[key]?.let { TokenCrypto.decrypt(it) }

    val accessTokenFlow: Flow<String?> = context.dataStore.data
        .map { it.decrypted(KEY_ACCESS_TOKEN) }

    val authStateFlow: Flow<AuthState> = context.dataStore.data.map { prefs ->
        val token = prefs.decrypted(KEY_ACCESS_TOKEN)
        if (token != null) {
            AuthState.Authenticated(
                accessToken = token,
                refreshToken = prefs.decrypted(KEY_REFRESH_TOKEN) ?: "",
                userId = prefs.decrypted(KEY_USER_ID) ?: "",
                userRole = prefs.decrypted(KEY_USER_ROLE) ?: "",
                userName = prefs.decrypted(KEY_USER_NAME) ?: "",
                tenantId = prefs.decrypted(KEY_TENANT_ID) ?: "",
            )
        } else {
            // token lipsă SAU nedecriptabil (ex. restore pe alt device) → re-login
            AuthState.Unauthenticated
        }
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: String,
        userRole: String,
        userName: String,
        tenantId: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = TokenCrypto.encrypt(accessToken)
            prefs[KEY_REFRESH_TOKEN] = TokenCrypto.encrypt(refreshToken)
            prefs[KEY_USER_ID] = TokenCrypto.encrypt(userId)
            prefs[KEY_USER_ROLE] = TokenCrypto.encrypt(userRole)
            prefs[KEY_USER_NAME] = TokenCrypto.encrypt(userName)
            prefs[KEY_TENANT_ID] = TokenCrypto.encrypt(tenantId)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val userRole: String,
        val userName: String,
        val tenantId: String,
    ) : AuthState()
}

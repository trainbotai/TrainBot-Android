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
 * Persists auth tokens in Jetpack DataStore (Preferences).
 * Mirrors iOS KeychainHelper — stores accessToken, refreshToken, and user info.
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

    val accessTokenFlow: Flow<String?> = context.dataStore.data
        .map { it[KEY_ACCESS_TOKEN] }

    val authStateFlow: Flow<AuthState> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_ACCESS_TOKEN]
        if (token != null) {
            AuthState.Authenticated(
                accessToken = token,
                refreshToken = prefs[KEY_REFRESH_TOKEN] ?: "",
                userId = prefs[KEY_USER_ID] ?: "",
                userRole = prefs[KEY_USER_ROLE] ?: "",
                userName = prefs[KEY_USER_NAME] ?: "",
                tenantId = prefs[KEY_TENANT_ID] ?: "",
            )
        } else {
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
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_ROLE] = userRole
            prefs[KEY_USER_NAME] = userName
            prefs[KEY_TENANT_ID] = tenantId
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

package com.luca.trainbot.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "trainbot_onboarding")

/**
 * Persists the "has seen onboarding" flag.
 * Mirrors iOS @AppStorage("hasSeenOnboarding").
 */
class OnboardingStore(private val context: Context) {

    companion object {
        private val KEY_HAS_SEEN = booleanPreferencesKey("has_seen_onboarding")
    }

    val hasSeenOnboarding: Flow<Boolean> = context.onboardingDataStore.data
        .map { it[KEY_HAS_SEEN] ?: false }

    suspend fun markSeen() {
        context.onboardingDataStore.edit { it[KEY_HAS_SEEN] = true }
    }

    suspend fun resetOnboarding() {
        context.onboardingDataStore.edit { it[KEY_HAS_SEEN] = false }
    }
}

package com.luca.trainbot.feature.dailychallenge

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val Context.dailyChallengeDataStore by preferencesDataStore(name = "trainbot_daily_challenge")

/**
 * Persists and rotates the daily challenge.
 * Mirrors iOS DailyChallengeService (CoreData) — local-only.
 */
class DailyChallengeStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val KEY_CHALLENGE = stringPreferencesKey("daily_challenge")

    val challengeFlow: Flow<DailyChallenge?> = context.dailyChallengeDataStore.data
        .map { prefs ->
            val raw = prefs[KEY_CHALLENGE] ?: return@map refreshForToday(null)
            val saved = runCatching { json.decodeFromString<DailyChallenge>(raw) }.getOrNull()
            val todayKey = LocalDate.now().toString()
            if (saved?.dateKey == todayKey) saved else refreshForToday(saved)
        }

    private fun refreshForToday(previous: DailyChallenge?): DailyChallenge {
        val dayOfYear = LocalDate.now().dayOfYear
        val template = CHALLENGE_TEMPLATES[dayOfYear % CHALLENGE_TEMPLATES.size]
        return DailyChallenge(
            id = "daily_${LocalDate.now()}",
            dateKey = LocalDate.now().toString(),
            description = template.first,
            goal = template.second,
            xpReward = template.third,
        )
    }

    suspend fun incrementProgress(amount: Int = 1) {
        context.dailyChallengeDataStore.edit { prefs ->
            val raw = prefs[KEY_CHALLENGE]
            val current = raw?.let {
                runCatching { json.decodeFromString<DailyChallenge>(it) }.getOrNull()
            } ?: return@edit

            val todayKey = LocalDate.now().toString()
            if (current.dateKey != todayKey) return@edit // stale — will refresh on next read

            if (!current.completed) {
                val newProgress = (current.progress + amount).coerceAtMost(current.goal)
                val updated = current.copy(
                    progress = newProgress,
                    completed = newProgress >= current.goal,
                )
                prefs[KEY_CHALLENGE] = json.encodeToString(updated)
            }
        }
    }

    suspend fun ensureToday() {
        context.dailyChallengeDataStore.edit { prefs ->
            val raw = prefs[KEY_CHALLENGE]
            val saved = raw?.let {
                runCatching { json.decodeFromString<DailyChallenge>(it) }.getOrNull()
            }
            val todayKey = LocalDate.now().toString()
            if (saved?.dateKey != todayKey) {
                prefs[KEY_CHALLENGE] = json.encodeToString(refreshForToday(saved))
            }
        }
    }
}

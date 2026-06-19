package com.luca.trainbot.feature.achievements

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.achievementsDataStore by preferencesDataStore(name = "trainbot_achievements")

/**
 * Persists achievement progress locally.
 * Mirrors iOS AchievementsService (CoreData) — local-only, no backend.
 */
class AchievementsStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val KEY_PROGRESS = stringPreferencesKey("achievement_progress")

    val progressFlow: Flow<List<AchievementProgress>> = context.achievementsDataStore.data
        .map { prefs ->
            val raw = prefs[KEY_PROGRESS] ?: return@map emptyList()
            runCatching { json.decodeFromString<List<AchievementProgress>>(raw) }
                .getOrDefault(emptyList())
        }

    suspend fun incrementProgress(achievementId: String, amount: Int = 1) {
        context.achievementsDataStore.edit { prefs ->
            val raw = prefs[KEY_PROGRESS] ?: "[]"
            val list = runCatching {
                json.decodeFromString<List<AchievementProgress>>(raw).toMutableList()
            }.getOrDefault(mutableListOf())

            val idx = list.indexOfFirst { it.id == achievementId }
            val def = ALL_ACHIEVEMENTS.find { it.id == achievementId } ?: return@edit
            if (idx >= 0) {
                val current = list[idx]
                if (!current.unlocked) {
                    val newProgress = (current.progress + amount).coerceAtMost(def.target)
                    list[idx] = current.copy(
                        progress = newProgress,
                        unlocked = newProgress >= def.target,
                    )
                }
            } else {
                val newProgress = amount.coerceAtMost(def.target)
                list.add(AchievementProgress(
                    id = achievementId,
                    progress = newProgress,
                    unlocked = newProgress >= def.target,
                ))
            }
            prefs[KEY_PROGRESS] = json.encodeToString(list)
        }
    }

    /** Returns all achievements merged with current progress. */
    fun allWithProgress(progressList: List<AchievementProgress>)
        : List<Triple<AchievementDefinition, Int, Boolean>> {
        val progressMap = progressList.associateBy { it.id }
        return ALL_ACHIEVEMENTS.map { def ->
            val p = progressMap[def.id]
            Triple(def, p?.progress ?: 0, p?.unlocked ?: false)
        }
    }
}

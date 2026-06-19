package com.luca.trainbot.feature.dailychallenge

import kotlinx.serialization.Serializable

/**
 * A daily challenge task — mirrors iOS DailyChallenge model.
 * Fully local — no backend endpoint. Persisted to DataStore as JSON.
 */
@Serializable
data class DailyChallenge(
    val id: String,
    val dateKey: String,        // "YYYY-MM-DD" — used to detect new day
    val description: String,
    val goal: Int,
    val progress: Int = 0,
    val completed: Boolean = false,
    val xpReward: Int = 10,
)

/**
 * Static pool of challenge templates, rotated by day-of-year.
 * Mirrors iOS DailyChallengeService definitions.
 */
val CHALLENGE_TEMPLATES = listOf(
    Triple("Adaugă 3 poze noi la un proiect.", 3, 10),
    Triple("Antrenează un model.", 1, 15),
    Triple("Testează AI-ul cu 2 poze diferite.", 2, 10),
    Triple("Trimite un mesaj bot-ului tău.", 1, 10),
    Triple("Creează un proiect nou.", 1, 20),
    Triple("Adaugă 5 poze la o etichetă.", 5, 15),
    Triple("Testează AI-ul cu 3 poze.", 3, 12),
)

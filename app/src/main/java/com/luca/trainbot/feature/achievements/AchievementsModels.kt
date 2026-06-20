package com.luca.trainbot.feature.achievements

import kotlinx.serialization.Serializable

/**
 * Mirrors iOS AchievementDefinition — static definitions for all achievable badges.
 */
data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,   // Material icon name equivalent — rendered as emoji in Compose
    val emoji: String,
    val target: Int,    // Number of actions required to unlock
)

/**
 * Serializable progress record — persisted to DataStore as JSON.
 */
@Serializable
data class AchievementProgress(
    val id: String,
    val progress: Int = 0,
    val unlocked: Boolean = false,
)

/** All supported achievements — mirrors iOS AchievementsService definitions. */
val ALL_ACHIEVEMENTS = listOf(
    AchievementDefinition(
        id = "first_project",
        title = "Prima clasă!",
        description = "Creează primul proiect de antrenare.",
        icon = "school",
        emoji = "🎓",
        target = 1,
    ),
    AchievementDefinition(
        id = "ten_images",
        title = "Colecționar",
        description = "Adaugă 10 poze în total.",
        icon = "photo_library",
        emoji = "📸",
        target = 10,
    ),
    AchievementDefinition(
        id = "first_train",
        title = "Antrenor",
        description = "Antrenează primul model.",
        icon = "psychology",
        emoji = "🧠",
        target = 1,
    ),
    AchievementDefinition(
        id = "first_test",
        title = "Detectiv",
        description = "Testează AI-ul tău cu o poză.",
        icon = "search",
        emoji = "🔍",
        target = 1,
    ),
    AchievementDefinition(
        id = "first_chat",
        title = "Conversație",
        description = "Trimite primul mesaj bot-ului tău.",
        icon = "chat",
        emoji = "💬",
        target = 1,
    ),
    AchievementDefinition(
        id = "five_projects",
        title = "Expert",
        description = "Creează 5 proiecte de antrenare.",
        icon = "star",
        emoji = "⭐",
        target = 5,
    ),
    AchievementDefinition(
        id = "bias_lesson",
        title = "Detectiv AI",
        description = "Ai descoperit limitele AI-ului! Ai completat lecția despre bias.",
        icon = "psychology_alt",
        emoji = "🕵️",
        target = 1,
    ),
)

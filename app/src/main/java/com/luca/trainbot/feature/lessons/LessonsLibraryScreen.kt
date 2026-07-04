package com.luca.trainbot.feature.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luca.trainbot.feature.achievements.AchievementsStore
import com.luca.trainbot.ui.navigation.Routes
import com.luca.trainbot.ui.components.Mascot
import com.luca.trainbot.ui.components.MascotState
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.Success

data class LessonEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val achievementId: String,
    val isHandsOn: Boolean,
    val route: String,
)

val LESSON_ENTRIES by lazy {
    listOf(
        LessonEntry(
            id = "real_training",
            title = "Antrenează primul tău model",
            subtitle = "Fă poze cu camera și învață AI-ul să recunoască lucruri!",
            emoji = "🤖",
            achievementId = "primul_model",
            isHandsOn = true,
            route = Routes.LESSON_REAL_TRAINING,
        ),
        LessonEntry(
            id = "real_bot",
            title = "Creează un bot AI",
            subtitle = "Dă exemple și fă-ți propriul bot care îți răspunde!",
            emoji = "✨",
            achievementId = "creator_de_boti",
            isHandsOn = true,
            route = Routes.LESSON_REAL_BOT,
        ),
        LessonEntry(
            id = "bias",
            title = "De ce greșește AI-ul?",
            subtitle = "Descoperă limitele AI și înțelege ce e bias-ul. Lecție interactivă!",
            emoji = "🕵️",
            achievementId = "bias_lesson",
            isHandsOn = false,
            route = Routes.BIAS_LESSON,
        ),
    )
}

@Composable
fun LessonsLibraryScreen(
    achievementsStore: AchievementsStore,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val progressList by achievementsStore.progressFlow.collectAsState(initial = emptyList())
    val unlockedIds = progressList.filter { it.unlocked }.map { it.id }.toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Înapoi")
            }
        }

        Text(
            text = "Lecții AI",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        // Mascot intro card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                    contentAlignment = Alignment.Center,
                ) {
                    Mascot(state = MascotState.HAPPY, size = 56.dp)
                }
                Text(
                    text = "Alege o lecție și învață AI-ul pas cu pas, cu mâna ta!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Lesson cards
        LESSON_ENTRIES.forEach { lesson ->
            LessonCard(
                lesson = lesson,
                isCompleted = lesson.achievementId in unlockedIds,
                onClick = { onNavigate(lesson.route) },
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun LessonCard(
    lesson: LessonEntry,
    isCompleted: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Emoji icon circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) Success.copy(alpha = 0.12f)
                        else PrimaryPurple.copy(alpha = 0.10f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(lesson.emoji, style = MaterialTheme.typography.headlineMedium)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (lesson.isHandsOn) {
                        PracticBadge()
                    }
                }
                Text(
                    text = lesson.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completat",
                    tint = Success,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PracticBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentBlue.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "practic",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = AccentBlue,
        )
    }
}

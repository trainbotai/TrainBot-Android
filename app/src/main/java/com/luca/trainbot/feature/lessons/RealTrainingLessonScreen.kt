package com.luca.trainbot.feature.lessons

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luca.trainbot.feature.achievements.AchievementsStore
import com.luca.trainbot.ui.components.ConfettiOverlay
import com.luca.trainbot.ui.components.Mascot
import com.luca.trainbot.ui.components.MascotState
import com.luca.trainbot.ui.components.rememberHapticConfirm
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.Success
import kotlinx.coroutines.launch

private data class TrainingStep(val mascotState: MascotState, val mascotText: String, val stepContent: String, val stepEmoji: String)

private val TRAINING_STEPS = listOf(
    TrainingStep(
        mascotState = MascotState.HAPPY,
        mascotText = "Hai să învățăm AI-ul să recunoască 2 lucruri de la tine — de exemplu creionul și guma!",
        stepContent = "Vei face 2 categorii de obiecte, de exemplu: creion și gumă. AI-ul va învăța să le deosebească.",
        stepEmoji = "✏️",
    ),
    TrainingStep(
        mascotState = MascotState.LEARNING,
        mascotText = "Acum faci poze cu fiecare obiect! Cu cât mai multe, cu atât AI-ul învață mai bine.",
        stepContent = "Fa minim 5 poze pentru fiecare categorie, din unghiuri diferite. Camera ta devine ochii AI-ului!",
        stepEmoji = "📸",
    ),
    TrainingStep(
        mascotState = MascotState.THINKING,
        mascotText = "Apasa Antreneaza si lasa AI-ul sa studieze pozele tale. Dureaza doar cateva secunde!",
        stepContent = "AI-ul analizează toate pozele și găsește ce face fiecare obiect special. Acesta e momentul magic!",
        stepEmoji = "🧠",
    ),
)

@Composable
fun RealTrainingLessonScreen(
    achievementsStore: AchievementsStore,
    onNavigateToTraining: () -> Unit,
    onBack: () -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var showConfetti by remember { mutableStateOf(false) }
    val hapticConfirm = rememberHapticConfirm()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
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
                text = "Antrenează primul tău model",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            // Step indicator
            LessonStepIndicator(total = TRAINING_STEPS.size, current = currentStep)

            val step = TRAINING_STEPS[currentStep]

            // Mascot narrator
            LessonMascotNarrator(mascotState = step.mascotState, text = step.mascotText)

            // Step content card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(step.stepEmoji, style = MaterialTheme.typography.displayMedium)
                    Text(
                        text = "Pasul ${currentStep + 1} din ${TRAINING_STEPS.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = step.stepContent,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Action button
            if (currentStep < TRAINING_STEPS.size - 1) {
                Button(
                    onClick = { currentStep++ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                ) {
                    Text("Continuă →", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                // Final step — hand-off to real training flow
                Button(
                    onClick = {
                        hapticConfirm()
                        showConfetti = true
                        scope.launch {
                            achievementsStore.incrementProgress("primul_model")
                        }
                        onNavigateToTraining()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                ) {
                    Text("→ Hai să începem!", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showConfetti) {
            ConfettiOverlay(onFinished = { showConfetti = false })
        }
    }
}

@Composable
internal fun LessonStepIndicator(total: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (i <= current) PrimaryPurple else PrimaryPurple.copy(alpha = 0.2f)),
            )
        }
    }
}

@Composable
internal fun LessonMascotNarrator(mascotState: MascotState, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                contentAlignment = Alignment.Center,
            ) {
                Mascot(state = mascotState, size = 64.dp)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

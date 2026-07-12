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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luca.trainbot.feature.achievements.AchievementsStore
import com.luca.trainbot.ui.components.ConfettiOverlay
import com.luca.trainbot.ui.components.MascotState
import com.luca.trainbot.ui.components.rememberHapticConfirm
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.Success
import kotlinx.coroutines.launch

private data class BotLessonStep(val mascotState: MascotState, val mascotText: String, val stepContent: String, val stepEmoji: String)

private val BOT_STEPS = listOf(
    BotLessonStep(
        mascotState = MascotState.HAPPY,
        mascotText = "Hai să-ți faci propriul bot AI! Îi dai exemple de întrebare→răspuns și învață să-ți răspundă.",
        stepContent = "Un bot AI este un program care raspunde la intrebari. Tu ii arati cum sa răspundă, dandu-i exemple.",
        stepEmoji = "🤖",
    ),
    BotLessonStep(
        mascotState = MascotState.LEARNING,
        mascotText = "Dă-i botului tău câteva exemple: scrie o întrebare și răspunsul corect. Cu cât mai multe, cu atât mai deștept!",
        stepContent = "Exemplu: Întrebare: 'Bună ziua!' -> Raspuns: 'Bună! Cum te pot ajuta?'. Adaugă 3-5 exemple ca sa înceapă sa învețe.",
        stepEmoji = "💬",
    ),
    BotLessonStep(
        mascotState = MascotState.THINKING,
        mascotText = "Salvează botul și încearcă să conversezi cu el! Vei vedea cum răspunde folosind ce l-ai învățat tu.",
        stepContent = "Apasă Salveaza si apoi Converseaza - botul tau e acum activ! Poti oricand sa-i adaugi mai multe exemple.",
        stepEmoji = "✨",
    ),
)

@Composable
fun RealBotLessonScreen(
    achievementsStore: AchievementsStore,
    onNavigateToLlm: () -> Unit,
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
                text = "Creează un bot AI",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            // Step indicator
            LessonStepIndicator(total = BOT_STEPS.size, current = currentStep)

            val step = BOT_STEPS[currentStep]

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
                        text = "Pasul ${currentStep + 1} din ${BOT_STEPS.size}",
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
            if (currentStep < BOT_STEPS.size - 1) {
                Button(
                    onClick = { currentStep++ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                ) {
                    Text("Continuă →", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                // Final step — hand-off to real LLM bot-creation flow
                Button(
                    onClick = {
                        hapticConfirm()
                        showConfetti = true
                        scope.launch {
                            achievementsStore.incrementProgress("creator_de_boti")
                        }
                        onNavigateToLlm()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                ) {
                    Text("→ Hai să facem botul!", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showConfetti) {
            ConfettiOverlay(onFinished = { showConfetti = false })
        }
    }
}

package com.luca.trainbot.feature.biaslesson

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luca.trainbot.TrainBotApplication
import com.luca.trainbot.core.ml.ClassifierPrediction
import com.luca.trainbot.feature.achievements.AchievementsStore
import com.luca.trainbot.ui.components.ConfettiOverlay
import com.luca.trainbot.ui.components.Mascot
import com.luca.trainbot.ui.components.MascotState
import com.luca.trainbot.ui.components.rememberHapticConfirm
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.Success
import com.luca.trainbot.ui.theme.Warning

@Composable
fun BiasLessonScreen(
    achievementsStore: AchievementsStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val vm: BiasLessonViewModel = viewModel(
        factory = BiasLessonViewModel.Factory(context, achievementsStore),
    )
    val state by vm.state.collectAsState()
    val hapticConfirm = rememberHapticConfirm()

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
                text = "De ce greșește AI-ul?",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            // Step indicator
            StepIndicator(currentStep = state.step)

            // Step content
            when (state.step) {
                LessonStep.INTRO -> IntroStep(
                    onNext = { vm.nextStep() },
                )
                LessonStep.TRAINING -> TrainingStep(
                    isTraining = state.isTraining,
                    error = state.trainError,
                    onTrain = { vm.startTraining() },
                )
                LessonStep.TESTING -> TestingStep(
                    isTesting = state.isTesting,
                    prediction = state.prediction,
                    onTest = { vm.runTest() },
                    onNext = { vm.nextStep() },
                )
                LessonStep.CONCLUSION -> ConclusionStep(
                    lessonCompleted = state.lessonCompleted,
                    onComplete = {
                        hapticConfirm()
                        vm.completeLesson()
                    },
                    onBack = onBack,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Confetti overlay
        if (state.showConfetti) {
            ConfettiOverlay(onFinished = { vm.dismissConfetti() })
        }
    }
}

@Composable
private fun StepIndicator(currentStep: LessonStep) {
    val steps = LessonStep.values()
    val currentIdx = steps.indexOf(currentStep)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { i, _ ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (i <= currentIdx) PrimaryPurple else PrimaryPurple.copy(alpha = 0.2f)),
            )
        }
    }
}

@Composable
private fun MascotNarrator(
    mascotState: MascotState,
    text: String,
) {
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

@Composable
private fun IntroStep(onNext: () -> Unit) {
    MascotNarrator(
        mascotState = MascotState.HAPPY,
        text = "AI învață DOAR din ce-i arăți. Hai să vedem ce se întâmplă dacă-l înveți greșit.",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Planul:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LessonBullet("1. Antrenăm un bot să recunoască un MĂR")
            LessonBullet("2. Dar îi dăm DOAR mere roșii")
            LessonBullet("3. Testăm cu un măr VERDE")
            LessonBullet("4. Vedem ce se întâmplă!")
        }
    }

    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
    ) {
        Text("Hai să încercăm!", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LessonBullet(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = PrimaryPurple, style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TrainingStep(
    isTraining: Boolean,
    error: String?,
    onTrain: () -> Unit,
) {
    MascotNarrator(
        mascotState = if (isTraining) MascotState.LEARNING else MascotState.IDLE,
        text = "Antrenăm un bot să recunoască un MĂR — dar îi dăm doar mere ROȘII.",
    )

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
            // Simple visual of red apples
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) {
                    Text("", style = MaterialTheme.typography.headlineLarge)
                }
            }
            Text(
                text = "5 mere roșii pentru clasa \"Măr\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) {
                    Text("", style = MaterialTheme.typography.headlineLarge)
                }
            }
            Text(
                text = "5 forme colorate pentru clasa \"Altceva\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (isTraining) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryPurple)
                Text("Se antrenează botul...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    Button(
        onClick = onTrain,
        enabled = !isTraining,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
    ) {
        if (isTraining) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = if (isTraining) "Antrenez..." else "Antrenează botul!",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun TestingStep(
    isTesting: Boolean,
    prediction: ClassifierPrediction?,
    onTest: () -> Unit,
    onNext: () -> Unit,
) {
    MascotNarrator(
        mascotState = when {
            isTesting -> MascotState.THINKING
            prediction != null -> MascotState.CONFUSED
            else -> MascotState.IDLE
        },
        text = "Acum testează cu un măr VERDE. Ce crezi că va spune botul?",
    )

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
            Text("", style = MaterialTheme.typography.displayLarge)
            Text(
                text = "Măr verde de testat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isTesting) {
                CircularProgressIndicator(color = PrimaryPurple)
                Text("Botul analizează...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            prediction?.let { p ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Cât de sigur e botul:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Per-class breakdown sorted descending
                val sorted = p.allScores.entries.sortedByDescending { it.value }
                sorted.forEach { (label, score) ->
                    val pct = (score * 100).toInt()
                    val barColor = if (label == p.label && p.confidence > 0.6) PrimaryPurple else Warning
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("$pct%", style = MaterialTheme.typography.bodyMedium, color = barColor)
                        }
                        LinearProgressIndicator(
                            progress = { score.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (p.confidence < 0.6) Warning.copy(alpha = 0.15f)
                        else PrimaryPurple.copy(alpha = 0.1f),
                    ),
                ) {
                    Text(
                        text = if (p.confidence < 0.6)
                            "Botul nu e sigur! Probabil nu a mai văzut un măr verde."
                        else "Botul zice \"${p.label}\" cu ${(p.confidence * 100).toInt()}% siguranță.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    if (prediction == null) {
        Button(
            onClick = onTest,
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
        ) {
            Text("Testează mărul verde!", style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
        ) {
            Text("Continuă", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ConclusionStep(
    lessonCompleted: Boolean,
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    MascotNarrator(
        mascotState = MascotState.HAPPY,
        text = "Vezi? N-a văzut niciodată un măr verde, așa că nu-l recunoaște. Arată-i exemple VARIATE!",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Lecția de azi:",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LessonBullet("AI înțelege NUMAI ce i-ai arătat")
            LessonBullet("Dacă arăți doar mere roșii, nu va recunoaște mere verzi")
            LessonBullet("Asta se numește BIAS (prejudecată) în AI")
            LessonBullet("Soluția: date variate și echilibrate!")
        }
    }

    if (lessonCompleted) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🕵️", style = MaterialTheme.typography.displayMedium)
                Text(
                    text = "Felicitări! Ai deblocat emblema \"Detectiv AI\"!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Success,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Success),
        ) {
            Text("Înapoi acasă!", style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
        ) {
            Text("Completează lecția!", style = MaterialTheme.typography.labelLarge)
        }
    }
}

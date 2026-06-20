package com.luca.trainbot.feature.testing

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.foundation.shape.CircleShape
import com.luca.trainbot.R
import com.luca.trainbot.TrainBotApplication
import com.luca.trainbot.core.ml.ClassifierPrediction
import com.luca.trainbot.core.ml.MlProject
import com.luca.trainbot.ui.components.CameraCapture
import com.luca.trainbot.ui.components.Mascot
import com.luca.trainbot.ui.components.MascotState
import com.luca.trainbot.ui.components.rememberHaptic
import com.luca.trainbot.ui.components.rememberHapticConfirm
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.Success
import com.luca.trainbot.ui.theme.Warning

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TestingScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as TrainBotApplication
    val vm: TestingViewModel = viewModel(
        factory = TestingViewModel.Factory(
            app.container.mlProjectRepository,
            context,
            app.container.achievementsStore,
        ),
    )
    val state by vm.state.collectAsState()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var showCamera by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadProjects() }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { vm.predictFromUri(it) }
    }

    // Camera overlay
    if (showCamera) {
        Dialog(
            onDismissRequest = { showCamera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                CameraCapture(modifier = Modifier.fillMaxSize()) { bitmap ->
                    vm.predictFromBitmap(bitmap)
                    showCamera = false
                }
                IconButton(
                    onClick = { showCamera = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Închide", tint = Color.White)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.testing_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        if (state.selectedProject == null) {
            ProjectChooser(
                projects = state.projects,
                onSelect = { vm.selectProject(it) },
            )
        } else {
            val project = state.selectedProject!!
            TesterPanel(
                project = project,
                state = state,
                onBack = { vm.deselectProject() },
                onCameraClick = {
                    if (cameraPermission.status.isGranted) showCamera = true
                    else cameraPermission.launchPermissionRequest()
                },
                onGalleryClick = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        }
    }
}

@Composable
private fun ProjectChooser(
    projects: List<MlProject>,
    onSelect: (MlProject) -> Unit,
) {
    if (projects.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Mascot confused state
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                    contentAlignment = Alignment.Center,
                ) {
                    Mascot(state = MascotState.CONFUSED, size = 70.dp)
                }
                Text(
                    text = stringResource(R.string.testing_no_models),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    } else {
        Text(
            text = stringResource(R.string.testing_choose_model),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        projects.forEach { project ->
            Card(
                onClick = { onSelect(project) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Bot-ul tău e ${(project.trainedAccuracy * 100).toInt()}% inteligent",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TesterPanel(
    project: MlProject,
    state: TestingUiState,
    onBack: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    val haptic = rememberHaptic()
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.testing_back))
        }
        Spacer(Modifier.weight(1f))
        Text(project.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(72.dp))
    }

    // Captured image preview
    if (state.testImage != null) {
        Image(
            bitmap = state.testImage.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp)),
        )
    }

    // Prediction / progress card
    when {
        state.isPredicting -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Mascot thinking state
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Mascot(state = MascotState.THINKING, size = 90.dp)
                    }
                    Text(
                        text = stringResource(R.string.testing_thinking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
        }

        state.prediction != null -> {
            PredictionResultCard(prediction = state.prediction)
        }
    }

    // Error
    if (state.error != null) {
        Text(
            text = state.error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // Action buttons
    val hasImage = state.testImage != null
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { haptic(); onCameraClick() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.testing_camera_button), style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = { haptic(); onGalleryClick() },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (!hasImage) stringResource(R.string.testing_pick_image) else stringResource(R.string.testing_another_image),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun PredictionResultCard(prediction: ClassifierPrediction) {
    val isConfident = prediction.confidence > 0.6
    val mascotState = if (isConfident) MascotState.HAPPY else MascotState.CONFUSED
    val labelColor = if (isConfident) PrimaryPurple else Warning

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                contentAlignment = Alignment.Center,
            ) {
                Mascot(state = mascotState, size = 70.dp)
            }
            Text(
                text = stringResource(R.string.testing_result_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = prediction.label,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = labelColor,
            )
            Text(
                text = stringResource(R.string.testing_result_confidence, (prediction.confidence * 100).toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Per-class breakdown — mirrors iOS explainability
            if (prediction.allScores.size > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cât de sigur e botul:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                val sorted = prediction.allScores.entries.sortedByDescending { it.value }
                sorted.forEach { (label, score) ->
                    val pct = (score * 100).toInt()
                    val barColor = if (label == prediction.label) labelColor else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
                            Text("$pct%", style = MaterialTheme.typography.bodySmall, color = barColor)
                        }
                        LinearProgressIndicator(
                            progress = { score.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

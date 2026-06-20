package com.luca.trainbot.feature.training

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.luca.trainbot.R
import com.luca.trainbot.TrainBotApplication
import com.luca.trainbot.core.ml.MlLabel
import com.luca.trainbot.core.ml.MlProject
import com.luca.trainbot.ui.components.CameraCapture
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.Success

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrainingScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as TrainBotApplication
    val vm: TrainingViewModel = viewModel(
        factory = TrainingViewModel.Factory(
            app.container.mlProjectRepository,
            context,
            app.container.achievementsStore,
            app.container.mlSyncService,
        ),
    )
    val state by vm.state.collectAsState()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Dialog state
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showNewLabelDialog by remember { mutableStateOf(false) }
    var cameraTargetLabelId by remember { mutableStateOf<String?>(null) }
    var galleryTargetLabelId by remember { mutableStateOf<String?>(null) }

    // Gallery picker (multi-select, no permission needed on Android 13+)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        val labelId = galleryTargetLabelId ?: return@rememberLauncherForActivityResult
        val projectId = state.selectedProject?.id ?: return@rememberLauncherForActivityResult
        uris.forEach { uri -> vm.addImageFromUri(projectId, labelId, uri) }
        galleryTargetLabelId = null
    }

    // Camera overlay
    if (cameraTargetLabelId != null) {
        val labelId = cameraTargetLabelId!!
        Dialog(
            onDismissRequest = { cameraTargetLabelId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                CameraCapture(modifier = Modifier.fillMaxSize()) { bitmap ->
                    val projectId = state.selectedProject?.id ?: return@CameraCapture
                    vm.addImageFromBitmap(projectId, labelId, bitmap)
                    cameraTargetLabelId = null
                }
                IconButton(
                    onClick = { cameraTargetLabelId = null },
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
            text = stringResource(R.string.training_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        if (state.selectedProject == null) {
            ProjectList(
                projects = state.projects,
                onSelectProject = { vm.selectProject(it.id) },
                onNewProject = { showNewProjectDialog = true },
            )
        } else {
            val project = state.selectedProject!!
            ProjectEditor(
                project = project,
                state = state,
                onBack = { vm.deselectProject() },
                onNewLabel = { showNewLabelDialog = true },
                onCameraClick = { labelId ->
                    if (cameraPermission.status.isGranted) {
                        cameraTargetLabelId = labelId
                    } else {
                        cameraPermission.launchPermissionRequest()
                    }
                },
                onGalleryClick = { labelId ->
                    galleryTargetLabelId = labelId
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onTrain = { vm.train(project.id) },
            )
        }
    }

    // Dialogs
    if (showNewProjectDialog) {
        NameInputDialog(
            title = stringResource(R.string.training_new_project),
            hint = stringResource(R.string.training_new_project_hint),
            onConfirm = { name ->
                vm.createProject(name)
                showNewProjectDialog = false
            },
            onDismiss = { showNewProjectDialog = false },
        )
    }

    if (showNewLabelDialog) {
        NameInputDialog(
            title = "Etichetă nouă",
            hint = stringResource(R.string.training_new_label_hint),
            onConfirm = { name ->
                state.selectedProject?.id?.let { vm.addLabel(it, name) }
                showNewLabelDialog = false
            },
            onDismiss = { showNewLabelDialog = false },
        )
    }
}

@Composable
private fun ProjectList(
    projects: List<MlProject>,
    onSelectProject: (MlProject) -> Unit,
    onNewProject: () -> Unit,
) {
    Button(
        onClick = onNewProject,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.training_new_project), style = MaterialTheme.typography.labelLarge)
    }

    if (projects.isEmpty()) {
        Text(
            text = stringResource(R.string.training_no_projects),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    } else {
        projects.forEach { project ->
            ProjectCard(project = project, onClick = { onSelectProject(project) })
        }
    }
}

@Composable
private fun ProjectCard(project: MlProject, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
                    text = stringResource(R.string.training_labels_count, project.labels.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (project.isTrained) {
                    Text(
                        text = "Bot-ul tău e ${(project.trainedAccuracy * 100).toInt()}% inteligent!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
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

@Composable
private fun ProjectEditor(
    project: MlProject,
    state: TrainingUiState,
    onBack: () -> Unit,
    onNewLabel: () -> Unit,
    onCameraClick: (labelId: String) -> Unit,
    onGalleryClick: (labelId: String) -> Unit,
    onTrain: () -> Unit,
) {
    // Back button row
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.training_back))
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = project.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(72.dp)) // balance the back button
    }

    // Label cards
    project.labels.forEach { label ->
        LabelCard(
            label = label,
            thumbnails = state.thumbnails[label.id] ?: emptyList(),
            onCameraClick = { onCameraClick(label.id) },
            onGalleryClick = { onGalleryClick(label.id) },
        )
    }

    // Add label button
    OutlinedButton(
        onClick = onNewLabel,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(stringResource(R.string.training_new_label))
    }

    // Training progress / result card
    if (state.isTraining) {
        TrainingProgressCard(message = state.trainProgress ?: "Se antrenează…")
    } else if (state.lastAccuracy != null) {
        TrainedResultCard(accuracy = state.lastAccuracy)
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

    // Train button — mirrors iOS: minLabels=2, minImagesPerLabel=3
    val canTrain = project.labels.size >= 2 &&
        project.labels.all { it.imageFileNames.size >= MIN_IMAGES_PER_LABEL } &&
        !state.isTraining

    Button(
        onClick = onTrain,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = canTrain,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryPurple,
            disabledContainerColor = PrimaryPurple.copy(alpha = 0.4f),
        ),
    ) {
        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.training_train_button), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LabelCard(
    label: MlLabel,
    thumbnails: List<Bitmap>,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.training_photos_count, label.imageFileNames.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (thumbnails.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(thumbnails) { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCameraClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.training_camera_button), style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onGalleryClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.training_gallery_button), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun TrainingProgressCard(message: String) {
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
            // Mascot placeholder: gradient circle with brain emoji (mirrors iOS MascotView learning state)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                contentAlignment = Alignment.Center,
            ) {
                Text("🧠", style = MaterialTheme.typography.headlineLarge)
            }
            Text(
                text = stringResource(R.string.training_training_in_progress),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryPurple,
            )
        }
    }
}

@Composable
private fun TrainedResultCard(accuracy: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Mascot happy state
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                contentAlignment = Alignment.Center,
            ) {
                Text("🤖", style = MaterialTheme.typography.headlineLarge)
            }
            Text(
                text = stringResource(R.string.training_bot_learned, (accuracy * 100).toInt()),
                style = MaterialTheme.typography.headlineMedium,
                color = Success,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NameInputDialog(
    title: String,
    hint: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(hint) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            ) {
                Text(stringResource(R.string.training_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anulează") }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

private const val MIN_IMAGES_PER_LABEL = 3 // mirrors ImageClassifier.MIN_IMAGES_PER_LABEL

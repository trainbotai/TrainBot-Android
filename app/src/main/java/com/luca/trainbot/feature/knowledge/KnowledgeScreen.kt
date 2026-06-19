package com.luca.trainbot.feature.knowledge

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.luca.trainbot.core.ml.MlLabel
import com.luca.trainbot.core.ml.MlProject
import com.luca.trainbot.core.ml.MlProjectRepository
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.PrimaryPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shows what the kid's trained models contain: projects → labels → thumbnail images.
 * Mirrors iOS KnowledgeView. Local data only — reads from [MlProjectRepository].
 */
@Composable
fun KnowledgeScreen(mlProjectRepository: MlProjectRepository) {
    var projects by remember { mutableStateOf<List<MlProject>>(emptyList()) }

    LaunchedEffect(Unit) {
        projects = withContext(Dispatchers.IO) { mlProjectRepository.loadAllProjects() }
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
            text = "Cunoștințe",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        if (projects.isEmpty()) {
            EmptyKnowledgeCard()
        } else {
            projects.forEach { project ->
                ProjectKnowledgeCard(project = project, repository = mlProjectRepository)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun EmptyKnowledgeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🤖", style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nu ai cunoștințe încă.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Antrenează un proiect pentru a vedea ce ai învățat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ProjectKnowledgeCard(project: MlProject, repository: MlProjectRepository) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Project header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val labelCount = project.labels.size
                    val status = if (project.isTrained) {
                        "${(project.trainedAccuracy * 100).toInt()}% acuratețe"
                    } else {
                        "Neantrenat"
                    }
                    Text(
                        text = "$labelCount etichete · $status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (project.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                project.labels.forEach { label ->
                    LabelRow(label = label, projectId = project.id, repository = repository)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun LabelRow(label: MlLabel, projectId: String, repository: MlProjectRepository) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${label.imageFileNames.size} poze",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (label.imageFileNames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp),
            ) {
                items(label.imageFileNames) { filename ->
                    ThumbnailImage(
                        projectId = projectId,
                        labelId = label.id,
                        filename = filename,
                        repository = repository,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbnailImage(
    projectId: String,
    labelId: String,
    filename: String,
    repository: MlProjectRepository,
) {
    var bitmap by remember(filename) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(filename) {
        bitmap = withContext(Dispatchers.IO) {
            repository.loadBitmap(projectId, labelId, filename)
        }
    }

    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(text = "🖼", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

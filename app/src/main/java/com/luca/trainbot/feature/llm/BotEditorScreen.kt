package com.luca.trainbot.feature.llm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.SessionSummary
import com.luca.trainbot.ui.theme.Danger
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.SecondaryPurple
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Sheet-style editor for creating a new bot or adding a version to an existing one.
 * Mirrors iOS BotEditorView + BotEditorFormView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotEditorScreen(
    editing: SessionSummary?,
    repo: LlmRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val vm: BotEditorViewModel = viewModel(
        key = editing?.id,
        factory = BotEditorViewModel.Factory(repo, editing),
    )
    val title = if (editing == null) "Bot nou" else "Editeaza ${editing.name}"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Anuleaza")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Name
            SectionLabel("Nume")
            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                placeholder = { Text("Numele botului tău") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Examples
            SectionLabel("Exemple (${vm.examples.size}/${BotEditorViewModel.MAX_EXAMPLES})")
            vm.examples.forEachIndexed { index, entry ->
                ExamplePairCard(
                    index = index,
                    entry = entry,
                    onUserChange = { vm.updateExampleUser(entry.id, it) },
                    onAiChange = { vm.updateExampleAi(entry.id, it) },
                    onDelete = { vm.removeExample(entry.id) },
                )
            }

            if (vm.canAddExample) {
                TextButton(onClick = { vm.addExample() }) {
                    Text("+ Adaugă exemplu", color = MaterialTheme.colorScheme.primary)
                }
            }

            // Error
            if (vm.errorMessage != null) {
                Text(
                    text = vm.errorMessage!!,
                    color = Danger,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (vm.isValid && !vm.isSaving)
                            Brush.linearGradient(listOf(PrimaryPurple, SecondaryPurple))
                        else
                            Brush.linearGradient(listOf(Color.Gray, Color.Gray))
                    )
                    .clickable(enabled = vm.isValid && !vm.isSaving) {
                        vm.save { onSaved() }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        vm.isSaving -> "Se salveaza..."
                        editing == null -> "Creează bot"
                        else -> "Salvează versiune nouă"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ExamplePairCard(
    index: Int,
    entry: ExampleEntry,
    onUserChange: (String) -> Unit,
    onAiChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Exemplul ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Șterge exemplu",
                        tint = Danger,
                    )
                }
            }
            OutlinedTextField(
                value = entry.user,
                onValueChange = onUserChange,
                label = { Text("Mesaj utilizator") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(8.dp),
            )
            OutlinedTextField(
                value = entry.ai,
                onValueChange = onAiChange,
                label = { Text("Răspuns bot") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(8.dp),
            )
        }
    }
}

package com.luca.trainbot.feature.llm

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.LlmStreamingRepository
import com.luca.trainbot.core.network.QueryQuota
import com.luca.trainbot.core.network.SessionSummary
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.Danger
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.SecondaryPurple

/**
 * Entry point for "AI-ul tau" — the LLM bot list.
 * Manages navigation sub-state internally (list → editor / chat).
 * Mirrors iOS BotListView.
 */
@Composable
fun LlmScreen(
    llmRepository: LlmRepository,
    llmStreamingRepository: LlmStreamingRepository,
    onBack: (() -> Unit)? = null,
) {
    val vm = remember { BotListViewModel(llmRepository) }
    var screen by remember { mutableStateOf<LlmSubScreen>(LlmSubScreen.List) }

    when (val s = screen) {
        LlmSubScreen.List -> BotListScreen(
            vm = vm,
            onBack = onBack,
            onOpenChat = { session ->
                screen = LlmSubScreen.Chat(session.id, session.name)
            },
            onNewBot = { screen = LlmSubScreen.Editor(null) },
        )

        is LlmSubScreen.Editor -> BotEditorScreen(
            editing = (s as? LlmSubScreen.Editor)?.session,
            repo = llmRepository,
            onDismiss = { screen = LlmSubScreen.List },
            onSaved = {
                vm.load()
                screen = LlmSubScreen.List
            },
        )

        is LlmSubScreen.Chat -> ChatScreen(
            sessionId = s.sessionId,
            sessionName = s.sessionName,
            repo = llmRepository,
            streaming = llmStreamingRepository,
            onBack = { screen = LlmSubScreen.List },
        )
    }
}

private sealed class LlmSubScreen {
    object List : LlmSubScreen()
    data class Editor(val session: SessionSummary?) : LlmSubScreen()
    data class Chat(val sessionId: String, val sessionName: String) : LlmSubScreen()
}

// ---------------------------------------------------------------------------
// BotListScreen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BotListScreen(
    vm: BotListViewModel,
    onBack: (() -> Unit)?,
    onOpenChat: (SessionSummary) -> Unit,
    onNewBot: () -> Unit,
) {
    val sessions by vm.sessions.collectAsState()
    val quota by vm.quota.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI-ul tau") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Inapoi")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNewBot) {
                        Icon(Icons.Default.Add, contentDescription = "Bot nou")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                isLoading -> {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Danger,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = { vm.load() }) {
                            Text("Reincearca", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (quota != null) {
                            QuotaCard(quota!!)
                        }
                        if (sessions.isEmpty()) {
                            EmptyBotState()
                        } else {
                            sessions.forEach { session ->
                                SessionCard(
                                    session = session,
                                    onClick = { onOpenChat(session) },
                                )
                            }
                        }
                        NewBotButton(onClick = onNewBot)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotaCard(quota: QueryQuota) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Mesaje ramase azi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${quota.remaining} / ${quota.limit}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${session.versionsCount} versiuni · ${session.queriesCount} mesaje",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyBotState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
        Text(
            text = "Niciun bot inca",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Creeaza primul tau bot AI daruindu-i exemple de cum sa raspunda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun NewBotButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(PrimaryPurple, SecondaryPurple)))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Bot nou",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

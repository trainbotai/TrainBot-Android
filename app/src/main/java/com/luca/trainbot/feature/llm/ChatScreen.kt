package com.luca.trainbot.feature.llm

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.LlmStreamingRepository
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.Danger
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.SecondaryPurple
import com.luca.trainbot.ui.theme.SurfaceLight
import com.luca.trainbot.ui.theme.Warning

/**
 * Chat screen — streams the bot reply word-by-word.
 * Shows a thinking indicator (3 animated dots) while waiting for the first chunk.
 * Handles flagged/safety-blocked responses with a kid-friendly message.
 * Mirrors iOS ChatView + ChatViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    sessionName: String,
    repo: LlmRepository,
    streaming: LlmStreamingRepository,
    onBack: () -> Unit,
) {
    val vm = remember(sessionId) { ChatViewModel(sessionId, sessionName, repo, streaming) }
    var inputText by remember { mutableStateOf("") }
    var showReport by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { vm.load() }

    // Auto-scroll when history or streaming changes
    val historySize = vm.history.size
    val streamText = vm.pendingMessage?.accumulatedResponse
    LaunchedEffect(historySize, streamText) {
        val itemCount = vm.history.size * 2 + if (vm.pendingMessage != null) 2 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem((itemCount - 1).coerceAtLeast(0))
        }
    }

    if (showReport) {
        ReportDialog(
            onDismiss = { showReport = false },
            onSubmit = { reason ->
                vm.reportSession(reason) { ok ->
                    if (ok) showReport = false
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(sessionName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Inapoi")
                    }
                },
                actions = {
                    IconButton(onClick = { showReport = true }) {
                        Icon(Icons.Default.Flag, contentDescription = "Raporteaza")
                    }
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Quota bar
                vm.quota?.let { quota ->
                    Text(
                        text = "${quota.remaining} / ${quota.limit} mesaje ramase azi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }

                // Error
                vm.errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                // Input row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Scrie un mesaj...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                    )
                    val canSend = inputText.isNotBlank() && vm.pendingMessage == null
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) Brush.linearGradient(listOf(PrimaryPurple, SecondaryPurple))
                                else Brush.linearGradient(listOf(Color.LightGray, Color.LightGray)),
                            )
                            .clickable(enabled = canSend) {
                                val text = inputText.trim()
                                if (text.isNotEmpty()) {
                                    inputText = ""
                                    vm.send(text)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Trimite",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceLight)
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vm.history, key = { it.id }) { item ->
                MessageBubble(text = item.userPrompt, isUser = true, flagged = false)
                Spacer(modifier = Modifier.height(4.dp))
                MessageBubble(
                    text = if (item.flagged) "Mesajul tau nu poate fi procesat. Incearca alt subiect." else item.aiResponse,
                    isUser = false,
                    flagged = item.flagged,
                )
            }
            vm.pendingMessage?.let { pending ->
                item(key = "pending") {
                    MessageBubble(text = pending.userPrompt, isUser = true, flagged = false)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (pending.accumulatedResponse.isEmpty()) {
                        ThinkingIndicator()
                    } else {
                        MessageBubble(
                            text = pending.accumulatedResponse,
                            isUser = false,
                            flagged = false,
                        )
                    }
                }
            }
            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun MessageBubble(text: String, isUser: Boolean, flagged: Boolean) {
    val userGradient = Brush.linearGradient(listOf(PrimaryPurple, SecondaryPurple))
    val botGradient = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface),
    )
    val flaggedGradient = Brush.linearGradient(
        listOf(Warning.copy(alpha = 0.15f), Warning.copy(alpha = 0.15f)),
    )
    val bubbleBrush = when {
        flagged -> flaggedGradient
        isUser -> userGradient
        else -> botGradient
    }
    val textColor = when {
        flagged -> Warning
        isUser -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🤖", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUser) 18.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp,
                    ),
                )
                .background(bubbleBrush)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
        }
    }
}

/** Three pulsing dots — mirrors iOS ThinkingIndicator. */
@Composable
private fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 38.dp, top = 4.dp, bottom = 4.dp),
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 150,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String?) -> Unit,
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Raporteaza") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Spune-i profesorului ca ceva nu e in regula cu acest bot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivul (optional)") },
                    placeholder = { Text("Ce s-a intamplat?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(reason.trim().ifEmpty { null }) }) {
                Text("Trimite raport", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuleaza") }
        },
    )
}

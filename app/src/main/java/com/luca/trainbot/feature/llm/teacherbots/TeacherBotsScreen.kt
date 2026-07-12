package com.luca.trainbot.feature.llm.teacherbots

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luca.trainbot.core.network.ChatHistoryItem
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.LlmStreamingRepository
import com.luca.trainbot.core.network.TeacherBot
import com.luca.trainbot.feature.llm.PendingMessage
import com.luca.trainbot.ui.components.Mascot
import com.luca.trainbot.ui.components.MascotState
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.Danger
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.SecondaryPurple
import com.luca.trainbot.ui.theme.SurfaceLight
import com.luca.trainbot.ui.theme.Warning

/**
 * Entry point for "Boții profesorului".
 * Manages list → chat navigation internally, mirroring the LlmScreen pattern.
 */
@Composable
fun TeacherBotsScreen(
    llmRepository: LlmRepository,
    llmStreamingRepository: LlmStreamingRepository,
    onBack: () -> Unit,
) {
    val listVm: TeacherBotsViewModel = viewModel(factory = TeacherBotsViewModel.Factory(llmRepository))
    var subScreen by remember { mutableStateOf<TeacherBotSubScreen>(TeacherBotSubScreen.List) }

    when (val s = subScreen) {
        TeacherBotSubScreen.List -> TeacherBotListScreen(
            vm = listVm,
            onBack = onBack,
            onOpenBot = { bot -> subScreen = TeacherBotSubScreen.Chat(bot.id, bot.name) },
        )
        is TeacherBotSubScreen.Chat -> TeacherBotChatScreen(
            botId = s.botId,
            botName = s.botName,
            llmRepository = llmRepository,
            llmStreamingRepository = llmStreamingRepository,
            onBack = { subScreen = TeacherBotSubScreen.List },
        )
    }
}

private sealed class TeacherBotSubScreen {
    object List : TeacherBotSubScreen()
    data class Chat(val botId: String, val botName: String) : TeacherBotSubScreen()
}

// ---------------------------------------------------------------------------
// List screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherBotListScreen(
    vm: TeacherBotsViewModel,
    onBack: () -> Unit,
    onOpenBot: (TeacherBot) -> Unit,
) {
    val bots by vm.bots.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Boții profesorului") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
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
                    CircularProgressIndicator(
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
                            Text("Reîncearcă", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                bots.isEmpty() -> {
                    EmptyTeacherBotsState(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        bots.forEach { bot ->
                            TeacherBotCard(bot = bot, onClick = { onOpenBot(bot) })
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherBotCard(bot: TeacherBot, onClick: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrimaryPurple, AccentBlue))),
                contentAlignment = Alignment.Center,
            ) {
                Mascot(state = MascotState.IDLE, size = 38.dp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bot.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "creat de ${bot.teacherName}",
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
private fun EmptyTeacherBotsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
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
            Mascot(state = MascotState.IDLE, size = 70.dp)
        }
        Text(
            text = "Niciun bot deocamdată",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Profesorul tău n-a creat încă boți. Revino mai târziu!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Chat screen — reuses ChatScreen composables via TeacherBotChatViewModel
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeacherBotChatScreen(
    botId: String,
    botName: String,
    llmRepository: LlmRepository,
    llmStreamingRepository: LlmStreamingRepository,
    onBack: () -> Unit,
) {
    val vm: TeacherBotChatViewModel = viewModel(
        key = botId,
        factory = TeacherBotChatViewModel.Factory(botId, botName, llmRepository, llmStreamingRepository),
    )
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val historySize = vm.history.size
    val streamText = vm.pendingMessage?.accumulatedResponse
    LaunchedEffect(historySize, streamText) {
        val itemCount = vm.history.size * 2 + if (vm.pendingMessage != null) 2 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem((itemCount - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(botName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
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
                vm.errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
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
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceLight)
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vm.history, key = { it.id }) { item ->
                TeacherBotMessageBubble(text = item.userPrompt, isUser = true)
                Spacer(modifier = Modifier.height(4.dp))
                TeacherBotMessageBubble(
                    text = if (item.flagged) "Mesajul tău nu poate fi procesat. Încearcă alt subiect." else item.aiResponse,
                    isUser = false,
                )
            }
            vm.pendingMessage?.let { pending ->
                item(key = "pending") {
                    TeacherBotMessageBubble(text = pending.userPrompt, isUser = true)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (pending.accumulatedResponse.isEmpty()) {
                        TeacherBotThinkingIndicator()
                    } else {
                        TeacherBotMessageBubble(text = pending.accumulatedResponse, isUser = false)
                    }
                }
            }
            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun TeacherBotMessageBubble(text: String, isUser: Boolean) {
    val userGradient = Brush.linearGradient(listOf(PrimaryPurple, SecondaryPurple))
    val botGradient = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface),
    )
    val bubbleBrush = if (isUser) userGradient else botGradient
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface

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
                Mascot(state = MascotState.IDLE, size = 28.dp)
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

@Composable
private fun TeacherBotThinkingIndicator() {
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

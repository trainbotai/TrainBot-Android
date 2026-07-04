package com.luca.trainbot.feature.llm.teacherbots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luca.trainbot.core.network.ApiResult
import com.luca.trainbot.core.network.ChatHistoryItem
import com.luca.trainbot.core.network.HttpException
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.LlmStreamingRepository
import com.luca.trainbot.core.network.QueryQuota
import com.luca.trainbot.core.network.SseEvent
import com.luca.trainbot.core.network.UnauthorizedException
import com.luca.trainbot.feature.llm.PendingMessage
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Chat ViewModel for teacher bots.
 * Mirrors [com.luca.trainbot.feature.llm.ChatViewModel] but:
 * - Uses [LlmStreamingRepository.streamTeacherBotQuery] (teacher-bot endpoint).
 * - No persisted history — each session starts fresh.
 * - Still shows quota so the student knows remaining messages.
 */
class TeacherBotChatViewModel(
    val botId: String,
    val botName: String,
    private val repo: LlmRepository,
    private val streaming: LlmStreamingRepository,
) : ViewModel() {

    var history by mutableStateOf<List<ChatHistoryItem>>(emptyList())
        private set

    var pendingMessage by mutableStateOf<PendingMessage?>(null)
        private set

    var quota by mutableStateOf<QueryQuota?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadQuota()
    }

    private fun loadQuota() {
        viewModelScope.launch {
            val qr = repo.getQuota()
            if (qr is ApiResult.Success) quota = qr.data
        }
    }

    fun send(prompt: String) {
        if (pendingMessage != null) return
        viewModelScope.launch {
            val pending = PendingMessage(userPrompt = prompt)
            pendingMessage = pending
            errorMessage = null

            try {
                streaming.streamTeacherBotQuery(botId, prompt)
                    .catch { e ->
                        val msg = when (e) {
                            is UnauthorizedException -> "Sesiunea ta a expirat. Reconecteaza-te."
                            is HttpException -> e.detail
                            else -> "Eroare la trimitere."
                        }
                        errorMessage = msg
                        pendingMessage = null
                    }
                    .collect { event ->
                        when (event) {
                            is SseEvent.Chunk -> {
                                pendingMessage = pendingMessage?.copy(
                                    accumulatedResponse = (pendingMessage?.accumulatedResponse ?: "") + event.text,
                                )
                            }
                            is SseEvent.Done -> {
                                pendingMessage = pendingMessage?.copy(isComplete = true)
                            }
                        }
                    }

                val completed = pendingMessage
                if (completed != null && completed.isComplete) {
                    history = history + ChatHistoryItem(
                        id = UUID.randomUUID().toString(),
                        userPrompt = completed.userPrompt,
                        aiResponse = completed.accumulatedResponse,
                        flagged = false,
                        createdAt = java.time.Instant.now().toString(),
                    )
                }
                pendingMessage = null
                // Refresh quota after each message
                val qr = repo.getQuota()
                if (qr is ApiResult.Success) quota = qr.data
            } catch (e: UnauthorizedException) {
                errorMessage = "Sesiunea ta a expirat. Reconecteaza-te."
                pendingMessage = null
            } catch (e: HttpException) {
                errorMessage = e.detail
                pendingMessage = null
            } catch (e: Exception) {
                errorMessage = "Eroare la trimitere."
                pendingMessage = null
            }
        }
    }
}

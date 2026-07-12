package com.luca.trainbot.feature.llm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luca.trainbot.core.network.ApiResult
import com.luca.trainbot.core.network.ChatHistoryItem
import com.luca.trainbot.core.network.HttpException
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.LlmStreamingRepository
import com.luca.trainbot.core.network.QueryQuota
import com.luca.trainbot.core.network.SseEvent
import com.luca.trainbot.core.network.UnauthorizedException
import com.luca.trainbot.feature.achievements.AchievementsStore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * In-progress message while streaming is active.
 * Mirrors iOS PendingMessage.
 */
data class PendingMessage(
    val id: String = UUID.randomUUID().toString(),
    val userPrompt: String,
    var accumulatedResponse: String = "",
    var isComplete: Boolean = false,
)

/** Mirrors iOS ChatViewModel. */
class ChatViewModel(
    val sessionId: String,
    val sessionName: String,
    private val repo: LlmRepository,
    private val streaming: LlmStreamingRepository,
    private val achievementsStore: AchievementsStore,
) : ViewModel() {

    var history by mutableStateOf<List<ChatHistoryItem>>(emptyList())
        private set

    var pendingMessage by mutableStateOf<PendingMessage?>(null)
        private set

    var quota by mutableStateOf<QueryQuota?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val historyDeferred = async { repo.listQueries(sessionId) }
            val quotaDeferred = async { repo.getQuota() }
            when (val r = historyDeferred.await()) {
                is ApiResult.Success -> history = r.data
                is ApiResult.Error -> errorMessage = r.message
            }
            if (quota == null) {
                val qr = quotaDeferred.await()
                if (qr is ApiResult.Success) quota = qr.data
            }
            isLoading = false
        }
    }

    fun send(prompt: String) {
        if (pendingMessage != null) return
        viewModelScope.launch {
            val pending = PendingMessage(userPrompt = prompt)
            pendingMessage = pending
            errorMessage = null

            try {
                streaming.streamQuery(sessionId, prompt)
                    .catch { e ->
                        val msg = when (e) {
                            is UnauthorizedException -> "Sesiunea ta a expirat. Reconectează-te."
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
                    // Append to local history optimistically
                    history = history + ChatHistoryItem(
                        id = completed.id,
                        userPrompt = completed.userPrompt,
                        aiResponse = completed.accumulatedResponse,
                        flagged = false,
                        createdAt = java.time.Instant.now().toString(),
                    )
                    // Achievement: first_chat (target=1)
                    achievementsStore.incrementProgress("first_chat")
                }
                pendingMessage = null
                // Refresh quota
                val qr = repo.getQuota()
                if (qr is ApiResult.Success) quota = qr.data
            } catch (e: UnauthorizedException) {
                errorMessage = "Sesiunea ta a expirat. Reconectează-te."
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

    fun reportSession(reason: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repo.reportSession(sessionId, reason)
            onResult(result is ApiResult.Success)
            if (result is ApiResult.Error) {
                errorMessage = "Nu am putut trimite raportul."
            }
        }
    }

    class Factory(
        private val sessionId: String,
        private val sessionName: String,
        private val repo: LlmRepository,
        private val streaming: LlmStreamingRepository,
        private val achievementsStore: AchievementsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(sessionId, sessionName, repo, streaming, achievementsStore) as T
    }

}

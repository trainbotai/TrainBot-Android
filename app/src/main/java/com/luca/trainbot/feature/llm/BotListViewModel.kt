package com.luca.trainbot.feature.llm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luca.trainbot.core.network.ApiResult
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.QueryQuota
import com.luca.trainbot.core.network.SessionSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Mirrors iOS BotListViewModel. */
class BotListViewModel(private val repo: LlmRepository) : ViewModel() {

    private val _sessions = MutableStateFlow<List<SessionSummary>>(emptyList())
    val sessions: StateFlow<List<SessionSummary>> = _sessions.asStateFlow()

    private val _quota = MutableStateFlow<QueryQuota?>(null)
    val quota: StateFlow<QueryQuota?> = _quota.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val sessionsDeferred = async { repo.listSessions() }
            val quotaDeferred = async { repo.getQuota() }
            val sessionsResult = sessionsDeferred.await()
            val quotaResult = quotaDeferred.await()
            _isLoading.value = false

            when (sessionsResult) {
                is ApiResult.Success -> _sessions.value = sessionsResult.data
                is ApiResult.Error -> {
                    _errorMessage.value = sessionsResult.message
                    return@launch
                }
            }
            if (quotaResult is ApiResult.Success) {
                _quota.value = quotaResult.data
            }
        }
    }

    fun delete(session: SessionSummary) {
        viewModelScope.launch {
            when (val result = repo.deleteSession(session.id)) {
                is ApiResult.Success -> _sessions.value = _sessions.value.filter { it.id != session.id }
                is ApiResult.Error -> _errorMessage.value = result.message
            }
        }
    }
}

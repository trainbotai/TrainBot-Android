package com.luca.trainbot.feature.llm.teacherbots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luca.trainbot.core.network.ApiResult
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.TeacherBot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the teacher-bots list screen.
 * Fetches GET /student/llm/teacher-bots via [LlmRepository].
 */
class TeacherBotsViewModel(private val repo: LlmRepository) : ViewModel() {

    private val _bots = MutableStateFlow<List<TeacherBot>>(emptyList())
    val bots: StateFlow<List<TeacherBot>> = _bots.asStateFlow()

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
            when (val result = repo.listTeacherBots()) {
                is ApiResult.Success -> _bots.value = result.data
                is ApiResult.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    class Factory(private val repo: LlmRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TeacherBotsViewModel(repo) as T
    }

}

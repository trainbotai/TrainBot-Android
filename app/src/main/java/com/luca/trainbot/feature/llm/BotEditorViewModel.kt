package com.luca.trainbot.feature.llm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luca.trainbot.core.network.ApiResult
import com.luca.trainbot.core.network.ExampleDto
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.SessionDetail
import com.luca.trainbot.core.network.SessionSummary
import kotlinx.coroutines.launch
import java.util.UUID

/** A mutable example pair held in the editor. */
data class ExampleEntry(
    val id: String = UUID.randomUUID().toString(),
    var user: String = "",
    var ai: String = "",
)

/** Mirrors iOS BotEditorViewModel. */
class BotEditorViewModel(
    private val repo: LlmRepository,
    editing: SessionSummary? = null,
) : ViewModel() {

    companion object {
        const val MAX_EXAMPLES = 10
        const val MAX_EXAMPLE_LENGTH = 500
        const val MAX_NAME_LENGTH = 80
    }

    private val editingSessionId: String? = editing?.id

    var name by mutableStateOf(editing?.name ?: "")
    val examples = mutableStateListOf(ExampleEntry())

    var isSaving by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val canAddExample: Boolean get() = examples.size < MAX_EXAMPLES

    private val nonEmptyExamples: List<ExampleEntry>
        get() = examples.filter { it.user.isNotBlank() && it.ai.isNotBlank() }

    val isValid: Boolean
        get() {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty() || trimmedName.length > MAX_NAME_LENGTH) return false
            val valid = nonEmptyExamples
            if (valid.isEmpty()) return false
            return valid.all { it.user.length <= MAX_EXAMPLE_LENGTH && it.ai.length <= MAX_EXAMPLE_LENGTH }
        }

    fun addExample() {
        if (canAddExample) examples.add(ExampleEntry())
    }

    fun removeExample(id: String) {
        examples.removeIf { it.id == id }
        if (examples.isEmpty()) examples.add(ExampleEntry())
    }

    fun updateExampleUser(id: String, value: String) {
        val idx = examples.indexOfFirst { it.id == id }
        if (idx >= 0) examples[idx] = examples[idx].copy(user = value)
    }

    fun updateExampleAi(id: String, value: String) {
        val idx = examples.indexOfFirst { it.id == id }
        if (idx >= 0) examples[idx] = examples[idx].copy(ai = value)
    }

    fun save(onSaved: (SessionDetail) -> Unit) {
        if (!isValid) return
        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            val trimmedName = name.trim()
            val dtos = nonEmptyExamples.map {
                ExampleDto(user = it.user.trim(), ai = it.ai.trim())
            }
            val result = if (editingSessionId != null) {
                repo.addVersion(editingSessionId, dtos)
            } else {
                repo.createSession(trimmedName, dtos)
            }
            isSaving = false
            when (result) {
                is ApiResult.Success -> onSaved(result.data)
                is ApiResult.Error -> errorMessage = result.message
            }
        }
    }

    class Factory(
        private val repo: LlmRepository,
        private val editing: SessionSummary? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BotEditorViewModel(repo, editing) as T
    }

}

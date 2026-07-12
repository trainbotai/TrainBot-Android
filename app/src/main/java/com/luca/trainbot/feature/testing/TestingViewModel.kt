package com.luca.trainbot.feature.testing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luca.trainbot.core.ml.ClassifierPrediction
import com.luca.trainbot.core.ml.ImageClassifier
import com.luca.trainbot.core.ml.MlProject
import com.luca.trainbot.core.ml.MlProjectRepository
import com.luca.trainbot.feature.achievements.AchievementsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TestingUiState(
    val projects: List<MlProject> = emptyList(),
    val selectedProject: MlProject? = null,
    val testImage: Bitmap? = null,
    val prediction: ClassifierPrediction? = null,
    val isPredicting: Boolean = false,
    val error: String? = null,
)

class TestingViewModel(
    private val repository: MlProjectRepository,
    private val context: Context,
    private val achievementsStore: AchievementsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(TestingUiState())
    val state: StateFlow<TestingUiState> = _state.asStateFlow()

    private var classifier: ImageClassifier? = null

    fun loadProjects() {
        val projects = repository.loadAllProjects().filter { it.isTrained }
        _state.update { it.copy(projects = projects, error = null) }
    }

    fun selectProject(project: MlProject) {
        _state.update { it.copy(selectedProject = project, prediction = null, testImage = null, error = null) }
    }

    fun deselectProject() {
        _state.update { it.copy(selectedProject = null, prediction = null, testImage = null, error = null) }
    }

    fun predictFromBitmap(bitmap: Bitmap) {
        val project = _state.value.selectedProject ?: return
        viewModelScope.launch {
            _state.update { it.copy(testImage = bitmap, isPredicting = true, prediction = null, error = null) }
            runCatching {
                val result = withContext(Dispatchers.Default) {
                    getOrCreateClassifier().predict(bitmap, project)
                }
                if (result == null) {
                    _state.update { it.copy(isPredicting = false, error = "Modelul nu este gata. Antrenează mai întâi.") }
                } else {
                    _state.update { it.copy(isPredicting = false, prediction = result) }
                    // Achievement: first_test (target=1)
                    achievementsStore.incrementProgress("first_test")
                }
            }.onFailure { e ->
                _state.update { it.copy(isPredicting = false, error = e.message) }
            }
        }
    }

    fun predictFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bitmap = com.luca.trainbot.core.ml.decodeSampledFromUri(context, uri) ?: return@runCatching
                withContext(Dispatchers.Main) { predictFromBitmap(bitmap) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun getOrCreateClassifier(): ImageClassifier {
        return classifier ?: ImageClassifier(context).also { classifier = it }
    }

    override fun onCleared() {
        super.onCleared()
        classifier?.close()
    }

    class Factory(
        private val repository: MlProjectRepository,
        private val context: Context,
        private val achievementsStore: AchievementsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TestingViewModel(repository, context, achievementsStore) as T
    }
}

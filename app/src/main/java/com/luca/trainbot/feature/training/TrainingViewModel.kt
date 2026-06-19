package com.luca.trainbot.feature.training

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luca.trainbot.core.ml.ImageClassifier
import com.luca.trainbot.core.ml.MlLabel
import com.luca.trainbot.core.ml.MlProject
import com.luca.trainbot.core.ml.MlProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TrainingUiState(
    val projects: List<MlProject> = emptyList(),
    val selectedProject: MlProject? = null,
    val isTraining: Boolean = false,
    val trainProgress: String? = null,
    val lastAccuracy: Double? = null,
    val error: String? = null,
    /** Map labelId → list of thumbnail Bitmaps for display */
    val thumbnails: Map<String, List<Bitmap>> = emptyMap(),
)

class TrainingViewModel(
    private val repository: MlProjectRepository,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(TrainingUiState())
    val state: StateFlow<TrainingUiState> = _state.asStateFlow()

    private var classifier: ImageClassifier? = null

    init {
        loadProjects()
    }

    fun loadProjects() {
        val projects = repository.loadAllProjects()
        _state.update { it.copy(projects = projects, error = null) }
    }

    fun createProject(name: String) {
        val project = repository.createProject(name.trim())
        _state.update { it.copy(projects = repository.loadAllProjects()) }
        selectProject(project.id)
    }

    fun selectProject(projectId: String) {
        val project = repository.loadProject(projectId) ?: return
        _state.update {
            it.copy(
                selectedProject = project,
                lastAccuracy = if (project.isTrained) project.trainedAccuracy else null,
                error = null,
            )
        }
        loadThumbnails(project)
    }

    fun deselectProject() {
        loadProjects()
        _state.update { it.copy(selectedProject = null, lastAccuracy = null, error = null, thumbnails = emptyMap()) }
    }

    fun addLabel(projectId: String, name: String) {
        val updated = repository.addLabel(projectId, name.trim())
        _state.update { it.copy(selectedProject = updated) }
        loadThumbnails(updated)
    }

    fun addImageFromBitmap(projectId: String, labelId: String, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = repository.addImage(projectId, labelId, bitmap)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(selectedProject = updated) }
                loadThumbnails(updated)
            }
        }
    }

    fun addImageFromUri(projectId: String, labelId: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val updated = repository.addImageFromUri(projectId, labelId, uri)
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(selectedProject = updated) }
                    loadThumbnails(updated)
                }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun train(projectId: String) {
        val project = repository.loadProject(projectId) ?: return
        viewModelScope.launch {
            _state.update { it.copy(isTraining = true, error = null, lastAccuracy = null, trainProgress = "Se pregătește…") }
            runCatching {
                val cls = getOrCreateClassifier()
                val trained = withContext(Dispatchers.Default) {
                    cls.train(project, repository) { progress ->
                        val msg = when (progress) {
                            is ImageClassifier.TrainProgress.Preparing -> "Se pregătește…"
                            is ImageClassifier.TrainProgress.Computing ->
                                "Se calculează… ${progress.done}/${progress.total}"
                            is ImageClassifier.TrainProgress.Saving -> "Se salvează…"
                        }
                        _state.update { it.copy(trainProgress = msg) }
                    }
                }
                repository.saveTrainedProject(trained)
                _state.update {
                    it.copy(
                        selectedProject = trained,
                        lastAccuracy = trained.trainedAccuracy,
                        isTraining = false,
                        trainProgress = null,
                    )
                }
                loadProjects()
            }.onFailure { e ->
                _state.update { it.copy(isTraining = false, trainProgress = null, error = e.message) }
            }
        }
    }

    private fun loadThumbnails(project: MlProject) {
        viewModelScope.launch(Dispatchers.IO) {
            val map = project.labels.associate { label ->
                label.id to label.imageFileNames.take(6).mapNotNull { fn ->
                    repository.loadBitmap(project.id, label.id, fn)
                }
            }
            _state.update { it.copy(thumbnails = map) }
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TrainingViewModel(repository, context) as T
    }
}

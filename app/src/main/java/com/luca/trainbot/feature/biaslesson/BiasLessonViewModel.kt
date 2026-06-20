package com.luca.trainbot.feature.biaslesson

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luca.trainbot.core.ml.ClassifierPrediction
import com.luca.trainbot.core.ml.ImageClassifier
import com.luca.trainbot.core.ml.ImageEmbedder
import com.luca.trainbot.core.ml.MlLabel
import com.luca.trainbot.core.ml.MlProject
import com.luca.trainbot.feature.achievements.AchievementsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LessonStep { INTRO, TRAINING, TESTING, CONCLUSION }

data class BiasLessonUiState(
    val step: LessonStep = LessonStep.INTRO,
    val isTraining: Boolean = false,
    val isTesting: Boolean = false,
    val prediction: ClassifierPrediction? = null,
    val trainError: String? = null,
    val lessonCompleted: Boolean = false,
    val showConfetti: Boolean = false,
)

/**
 * ViewModel for the "Limitele AI" bias lesson.
 *
 * Trains a throwaway in-memory classifier on programmatically-generated demo images
 * (red apples vs coloured abstract shapes for "Altceva").
 * Tests with a green apple image → shows real low confidence.
 * Does NOT touch the kid's real MlProjectRepository.
 */
class BiasLessonViewModel(
    private val context: Context,
    private val achievementsStore: AchievementsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(BiasLessonUiState())
    val state: StateFlow<BiasLessonUiState> = _state.asStateFlow()

    private var trainedProject: MlProject? = null
    private var embedder: ImageEmbedder? = null

    fun nextStep() {
        _state.update { it.copy(step = when (it.step) {
            LessonStep.INTRO -> LessonStep.TRAINING
            LessonStep.TRAINING -> LessonStep.TESTING
            LessonStep.TESTING -> LessonStep.CONCLUSION
            LessonStep.CONCLUSION -> LessonStep.CONCLUSION
        }) }
    }

    fun startTraining() {
        _state.update { it.copy(isTraining = true, trainError = null) }
        viewModelScope.launch {
            runCatching {
                val project = withContext(Dispatchers.Default) { trainDemoModel() }
                trainedProject = project
                _state.update { it.copy(isTraining = false) }
                nextStep()
            }.onFailure { e ->
                _state.update { it.copy(isTraining = false, trainError = e.message) }
            }
        }
    }

    fun runTest() {
        val project = trainedProject ?: return
        _state.update { it.copy(isTesting = true, prediction = null) }
        viewModelScope.launch {
            runCatching {
                val result = withContext(Dispatchers.Default) {
                    val emb = getOrCreateEmbedder()
                    val greenApple = generateGreenAppleBitmap()
                    val queryEmb = emb.embed(greenApple)
                    // Manual predict using the trained project centroids
                    val labels = project.labels.filter { it.centroid.isNotEmpty() }
                    val rawScores: Map<String, Double> = labels.associate { label ->
                        label.name to emb.cosineSimilarity(queryEmb, label.centroid.toFloatArray())
                    }
                    val maxVal = rawScores.values.maxOrNull() ?: 0.0
                    val exps = rawScores.mapValues { (_, v) -> Math.exp(v - maxVal) }
                    val sum = exps.values.sum()
                    val softmax = exps.mapValues { (_, v) -> v / sum }
                    val best = softmax.maxByOrNull { it.value }!!
                    ClassifierPrediction(
                        label = best.key,
                        confidence = best.value,
                        allScores = softmax,
                    )
                }
                _state.update { it.copy(isTesting = false, prediction = result) }
            }.onFailure { e ->
                _state.update { it.copy(isTesting = false, trainError = e.message) }
            }
        }
    }

    fun completeLesson() {
        viewModelScope.launch {
            achievementsStore.incrementProgress("bias_lesson")
            _state.update { it.copy(lessonCompleted = true, showConfetti = true) }
        }
    }

    fun dismissConfetti() {
        _state.update { it.copy(showConfetti = false) }
    }

    // ── Demo image generation ─────────────────────────────────────────────────

    /**
     * Trains an in-memory classifier:
     *   Label "Măr" → 5 red-apple bitmaps (generated)
     *   Label "Altceva" → 5 abstract shape bitmaps (generated)
     *
     * Returns a fully trained [MlProject] with centroid embeddings.
     * Does NOT persist anything to disk.
     */
    private fun trainDemoModel(): MlProject {
        val emb = getOrCreateEmbedder()

        val redApples: List<FloatArray> = (0 until 5).map { i ->
            emb.embed(generateRedAppleBitmap(variant = i))
        }
        val others: List<FloatArray> = (0 until 5).map { i ->
            emb.embed(generateAbstractBitmap(variant = i))
        }

        fun centroid(embeddings: List<FloatArray>): List<Float> {
            val dim = embeddings.first().size
            val c = FloatArray(dim)
            for (e in embeddings) for (i in e.indices) c[i] += e[i]
            val n = embeddings.size.toFloat()
            for (i in c.indices) c[i] /= n
            return c.toList()
        }

        val labelMar = MlLabel(
            id = "mar",
            name = "Măr",
            imageFileNames = List(5) { "red_apple_$it.jpg" },
            centroid = centroid(redApples),
        )
        val labelAlt = MlLabel(
            id = "altceva",
            name = "Altceva",
            imageFileNames = List(5) { "other_$it.jpg" },
            centroid = centroid(others),
        )

        return MlProject(
            id = "bias_demo",
            name = "Demo bias",
            labels = listOf(labelMar, labelAlt),
            isTrained = true,
            trainedAccuracy = 1.0,
        )
    }

    /** Red circle (apple shape) on white/cream background. Varies slightly per variant. */
    private fun generateRedAppleBitmap(variant: Int = 0): Bitmap {
        val size = 224
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background: cream/light
        paint.color = android.graphics.Color.rgb(255, 250, 240)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Apple body: red circle, slightly different shade per variant
        val redShade = (200 + variant * 6).coerceAtMost(240)
        paint.color = android.graphics.Color.rgb(redShade, 30 + variant * 4, 20 + variant * 2)
        val cx = size / 2f
        val cy = size / 2f + 10f
        val r = 75f + variant * 3f
        canvas.drawCircle(cx, cy, r, paint)

        // Leaf: green rect at top
        paint.color = android.graphics.Color.rgb(34, 139, 34)
        canvas.drawRect(cx - 8f, 30f + variant * 2f, cx + 8f, 70f + variant * 2f, paint)

        // Stem: dark brown
        paint.color = android.graphics.Color.rgb(101, 67, 33)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(cx, 30f, cx + 15f, 15f, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    /** Abstract shapes (rectangles, circles in blue/yellow) — clearly "not an apple". */
    private fun generateAbstractBitmap(variant: Int = 0): Bitmap {
        val size = 224
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val bgColors = listOf(0xFF2196F3, 0xFFFFC107, 0xFF9C27B0, 0xFF009688, 0xFF795548)
        val shapeColors = listOf(0xFFFFEB3B, 0xFF4CAF50, 0xFFFF5722, 0xFF03A9F4, 0xFFE91E63)

        paint.color = (bgColors[variant % bgColors.size] or 0xFF000000.toLong()).toInt()
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        paint.color = (shapeColors[variant % shapeColors.size] or 0xFF000000.toLong()).toInt()
        when (variant % 3) {
            0 -> canvas.drawRect(40f, 40f, 184f, 184f, paint)
            1 -> canvas.drawCircle(112f, 112f, 70f, paint)
            else -> {
                val path = Path().apply {
                    moveTo(112f, 30f)
                    lineTo(194f, 194f)
                    lineTo(30f, 194f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
        }
        return bmp
    }

    /** Green circle (apple shape) — what the kid "tests" with. */
    fun generateGreenAppleBitmap(): Bitmap {
        val size = 224
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = android.graphics.Color.rgb(245, 255, 240)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Green apple body
        paint.color = android.graphics.Color.rgb(100, 180, 50)
        canvas.drawCircle(112f, 122f, 78f, paint)

        // Leaf
        paint.color = android.graphics.Color.rgb(34, 120, 20)
        canvas.drawRect(104f, 30f, 120f, 68f, paint)

        // Stem
        paint.color = android.graphics.Color.rgb(101, 67, 33)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(112f, 30f, 127f, 15f, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    private fun getOrCreateEmbedder(): ImageEmbedder {
        return embedder ?: ImageEmbedder(context).also { embedder = it }
    }

    override fun onCleared() {
        super.onCleared()
        embedder?.close()
    }

    class Factory(
        private val context: Context,
        private val achievementsStore: AchievementsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BiasLessonViewModel(context, achievementsStore) as T
    }
}

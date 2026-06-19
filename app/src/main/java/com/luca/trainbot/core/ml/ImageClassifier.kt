package com.luca.trainbot.core.ml

import android.content.Context
import android.graphics.Bitmap
import kotlin.math.exp

/**
 * On-device similarity classifier.
 * Mirrors iOS TrainedClassifier (CreateML ImageFeaturePrint + similarity).
 *
 * Training:  embed all images per label → centroid (mean embedding).
 * Inference: embed test image → cosine similarity vs each centroid
 *            → softmax → best label + confidence.
 */
class ImageClassifier(context: Context) {

    private val embedder = ImageEmbedder(context)

    companion object {
        const val MIN_LABELS = 2
        const val MIN_IMAGES_PER_LABEL = 3
    }

    // ── Training ─────────────────────────────────────────────────────────────

    sealed interface TrainProgress {
        data object Preparing : TrainProgress
        data class Computing(val done: Int, val total: Int) : TrainProgress
        data object Saving : TrainProgress
    }

    /**
     * Compute per-label centroid embeddings.
     * Returns updated [MlProject] with centroids filled in.
     */
    suspend fun train(
        project: MlProject,
        repository: MlProjectRepository,
        onProgress: (TrainProgress) -> Unit,
    ): MlProject {
        val labels = project.labels
        require(labels.size >= MIN_LABELS) { "Adaugă cel puțin $MIN_LABELS etichete." }
        for (label in labels) {
            require(label.imageFileNames.size >= MIN_IMAGES_PER_LABEL) {
                "Eticheta '${label.name}' are doar ${label.imageFileNames.size} poze. Adaugă minim $MIN_IMAGES_PER_LABEL."
            }
        }

        onProgress(TrainProgress.Preparing)

        val totalImages = labels.sumOf { it.imageFileNames.size }
        var processed = 0

        val trainedLabels = labels.map { label ->
            val embeddings = label.imageFileNames.mapNotNull { filename ->
                val bmp = repository.loadBitmap(project.id, label.id, filename)
                val emb = bmp?.let { embedder.embed(it) }
                processed++
                onProgress(TrainProgress.Computing(processed, totalImages))
                emb
            }
            val centroid = computeCentroid(embeddings)
            label.copy(centroid = centroid.toList())
        }

        onProgress(TrainProgress.Saving)

        val accuracy = computeTrainingAccuracy(project, trainedLabels, repository)

        return project.copy(
            labels = trainedLabels,
            isTrained = true,
            trainedAccuracy = accuracy,
        )
    }

    // ── Inference ─────────────────────────────────────────────────────────────

    /**
     * Classify [bitmap] against label centroids.
     * Returns null if project is not trained.
     */
    fun predict(bitmap: Bitmap, project: MlProject): ClassifierPrediction? {
        if (!project.isTrained) return null
        val labels = project.labels.filter { it.centroid.isNotEmpty() }
        if (labels.isEmpty()) return null

        val queryEmbedding = embedder.embed(bitmap)

        val rawScores: Map<MlLabel, Double> = labels.associateWith { label ->
            embedder.cosineSimilarity(queryEmbedding, label.centroid.toFloatArray())
        }

        val scores = softmax(rawScores)
        val best = scores.maxByOrNull { it.value } ?: return null

        return ClassifierPrediction(
            label = best.key.name,
            confidence = best.value,
            allScores = scores.mapKeys { it.key.name },
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun computeCentroid(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return FloatArray(0)
        val dim = embeddings.first().size
        val centroid = FloatArray(dim)
        for (emb in embeddings) {
            for (i in emb.indices) centroid[i] += emb[i]
        }
        val n = embeddings.size.toFloat()
        for (i in centroid.indices) centroid[i] /= n
        return centroid
    }

    private fun softmax(scores: Map<MlLabel, Double>): Map<MlLabel, Double> {
        val values = scores.values.toList()
        val maxVal = values.maxOrNull() ?: 0.0
        val exps = values.map { exp(it - maxVal) }
        val sum = exps.sum()
        val keys = scores.keys.toList()
        return keys.zip(exps.map { it / sum }).toMap()
    }

    private fun computeTrainingAccuracy(
        project: MlProject,
        trainedLabels: List<MlLabel>,
        repository: MlProjectRepository,
    ): Double {
        var correct = 0; var total = 0
        val tempProject = project.copy(labels = trainedLabels, isTrained = true)
        for (label in trainedLabels) {
            for (filename in label.imageFileNames) {
                val bmp = repository.loadBitmap(project.id, label.id, filename) ?: continue
                val pred = predict(bmp, tempProject) ?: continue
                if (pred.label == label.name) correct++
                total++
            }
        }
        return if (total == 0) 0.0 else correct.toDouble() / total
    }

    fun close() = embedder.close()
}

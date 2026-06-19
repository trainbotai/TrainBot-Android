package com.luca.trainbot.core.ml

import kotlinx.serialization.Serializable

/**
 * Mirrors iOS MLProjectEntity / MLLabelEntity.
 * Stored as JSON in app-private files directory (no Room needed — simpler, less overhead).
 */
@Serializable
data class MlProject(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val labels: List<MlLabel> = emptyList(),
    /** True once embeddings have been computed and the classifier is persisted. */
    val isTrained: Boolean = false,
    val trainedAccuracy: Double = 0.0,
)

@Serializable
data class MlLabel(
    val id: String,
    val name: String,
    /** File names of saved images relative to the project images dir. */
    val imageFileNames: List<String> = emptyList(),
    /**
     * Per-label centroid embedding (mean of all image embeddings).
     * Stored as FloatArray encoded as List<Float> for JSON.
     * Empty until training is done.
     */
    val centroid: List<Float> = emptyList(),
)

data class ClassifierPrediction(
    val label: String,
    val confidence: Double,
    val allScores: Map<String, Double>,
)

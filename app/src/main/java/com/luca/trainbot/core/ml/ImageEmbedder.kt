package com.luca.trainbot.core.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder as MediaPipeImageEmbedder

/**
 * Wraps MediaPipe ImageEmbedder (MobileNet V3 Small model bundled in assets).
 *
 * Mirrors the iOS CreateML ImageFeaturePrint approach:
 *   image → fixed-size float vector (embedding) → compare via cosine similarity.
 */
class ImageEmbedder(context: Context) {

    private val embedder: MediaPipeImageEmbedder

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .build()
        val options = MediaPipeImageEmbedder.ImageEmbedderOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setQuantize(false)
            .build()
        embedder = MediaPipeImageEmbedder.createFromOptions(context, options)
    }

    /** Returns the embedding float vector for [bitmap]. */
    fun embed(bitmap: Bitmap): FloatArray {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = embedder.embed(mpImage)
        // floatEmbedding() returns float[] directly
        return result.embeddingResult().embeddings().first().floatEmbedding()
    }

    /**
     * Cosine similarity between two embedding vectors via MediaPipe helper.
     * Both must be float embeddings from the same model.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = Math.sqrt(normA) * Math.sqrt(normB)
        return if (denom == 0.0) 0.0 else dot / denom
    }

    fun close() = embedder.close()

    companion object {
        private const val MODEL_ASSET = "mobilenet_v3_small.tflite"
    }
}

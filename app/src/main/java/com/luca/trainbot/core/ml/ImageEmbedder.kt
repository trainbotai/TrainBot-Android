package com.luca.trainbot.core.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Embedding
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder as MediaPipeImageEmbedder
import java.util.Optional

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
        val embA = Embedding.create(a, null, 0, Optional.empty())
        val embB = Embedding.create(b, null, 0, Optional.empty())
        return MediaPipeImageEmbedder.cosineSimilarity(embA, embB)
    }

    fun close() = embedder.close()

    companion object {
        private const val MODEL_ASSET = "mobilenet_v3_small.tflite"
    }
}

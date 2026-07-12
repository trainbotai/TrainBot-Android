package com.luca.trainbot.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.InputStream

/**
 * Decodează imagini downsamplate ca să evite OOM: o poză de 48MP din galerie =
 * ~200MB alocare la full-res, iar modelul MobileNet oricum lucrează la 224px.
 * Limităm la ~1024px (păstrează calitatea pentru afișare/grile), via inSampleSize.
 */
private const val MAX_DIMENSION = 1024

private fun sampleSizeFor(width: Int, height: Int, max: Int): Int {
    var sample = 1
    val halfW = width / 2
    val halfH = height / 2
    while (halfW / sample >= max || halfH / sample >= max) {
        sample *= 2
    }
    return sample
}

/** Decode dintr-un URI (galerie), downsamplat. Null dacă nu se poate decoda. */
fun decodeSampledFromUri(context: Context, uri: Uri, max: Int = MAX_DIMENSION): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, max)
    }
    return context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
        BitmapFactory.decodeStream(stream, null, opts)
    }
}

/** Decode dintr-un fișier de pe disc, downsamplat. */
fun decodeSampledFromFile(path: String, max: Int = MAX_DIMENSION): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, max)
    }
    return BitmapFactory.decodeFile(path, opts)
}

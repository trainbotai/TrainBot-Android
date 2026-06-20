package com.luca.trainbot.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val CONFETTI_EMOJIS = listOf("🎉", "⭐", "🌟", "✨", "🎊")
private val CONFETTI_COLORS = listOf(
    Color(0xFF6B3FA0),
    Color(0xFF4A90D9),
    Color(0xFFFFD700),
    Color(0xFFFF6B6B),
    Color(0xFF4ECDC4),
    Color(0xFFFF9500),
)

private data class Particle(
    val x: Float,          // 0..1 normalised starting x
    val color: Color,
    val size: Float,       // 0..1 normalised
    val speed: Float,      // 0..1 normalised
    val rotationSpeed: Float,
    val isEmoji: Boolean,
    val emoji: String,
)

/**
 * Celebratory confetti overlay.
 *
 * Usage:
 * ```kotlin
 * var showConfetti by remember { mutableStateOf(false) }
 * Box { /* content */ ; if (showConfetti) ConfettiOverlay { showConfetti = false } }
 * ```
 *
 * Mirrors the iOS emoji-confetti approach: lightweight Canvas particles + emojis,
 * no extra dependency. Auto-dismisses after [durationMs].
 */
@Composable
fun ConfettiOverlay(
    durationMs: Int = 2500,
    onFinished: () -> Unit = {},
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = durationMs, easing = LinearEasing))
        onFinished()
    }

    val particles = remember {
        List(60) {
            Particle(
                x = Random.nextFloat(),
                color = CONFETTI_COLORS.random(),
                size = Random.nextFloat() * 0.5f + 0.3f,
                speed = Random.nextFloat() * 0.4f + 0.4f,
                rotationSpeed = Random.nextFloat() * 720f - 360f,
                isEmoji = Random.nextFloat() < 0.35f,
                emoji = CONFETTI_EMOJIS.random(),
            )
        }
    }

    // Emoji text paint (native)
    val emojiPaint = remember { Paint().apply { textSize = 48f } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val p = progress.value
                particles.forEach { particle ->
                    val particleY = p * particle.speed
                    if (particleY < 1.1f) {
                        val px = particle.x * size.width
                        val py = particleY * size.height
                        val rotation = p * particle.rotationSpeed
                        val alpha = (1f - (p - 0.7f).coerceAtLeast(0f) / 0.3f).coerceIn(0f, 1f)
                        val rectSize = particle.size * 16f

                        if (particle.isEmoji) {
                            drawIntoCanvas { canvas ->
                                emojiPaint.alpha = (alpha * 255).toInt()
                                canvas.nativeCanvas.save()
                                canvas.nativeCanvas.translate(px, py)
                                canvas.nativeCanvas.rotate(rotation)
                                canvas.nativeCanvas.drawText(particle.emoji, -rectSize / 2f, rectSize / 2f, emojiPaint)
                                canvas.nativeCanvas.restore()
                            }
                        } else {
                            rotate(rotation, pivot = Offset(px, py)) {
                                drawRect(
                                    color = particle.color.copy(alpha = alpha),
                                    topLeft = Offset(px - rectSize / 2f, py - rectSize / 2f),
                                    size = androidx.compose.ui.geometry.Size(rectSize, rectSize * 0.5f),
                                )
                            }
                        }
                    }
                }
            }
    ) {}
}

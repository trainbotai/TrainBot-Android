package com.luca.trainbot.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luca.trainbot.ui.theme.AccentBlue
import com.luca.trainbot.ui.theme.PrimaryPurple
import com.luca.trainbot.ui.theme.SecondaryPurple

/**
 * Mirrors iOS MascotState enum — same cases, same semantics.
 */
enum class MascotState {
    /** Normal idle — eyes blink occasionally. */
    IDLE,
    /** Thinking — small dot eyes. */
    THINKING,
    /** Learning — big eyes with pulsing ring (training). */
    LEARNING,
    /** Happy — arc/smile eyes (success, achievement). */
    HAPPY,
    /** Confused — tilted capsule eyes. */
    CONFUSED,
    /** Error — X eyes. */
    ERROR,
}

/**
 * Compose port of iOS MascotView.
 *
 * Draws a rounded-rectangle robot head with two eyes that animate per [state].
 * Background gradient circle is intentionally NOT included here — callers wrap
 * this composable in whatever background they need (gradient circle, card, etc).
 */
@Composable
fun Mascot(
    state: MascotState,
    size: Dp = 100.dp,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot")

    // Blink animation — only used in IDLE
    val blinkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blink",
    )

    // Pulse animation — used in LEARNING
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // Overall body scale pulse in LEARNING
    val bodyScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "body_scale",
    )

    // Blink is a quick squint at a specific phase of the cycle
    val isBlinking = state == MascotState.IDLE && blinkProgress % 1f in 0.85f..0.90f

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val s = this.size.width
            val applyBodyScale = if (state == MascotState.LEARNING) bodyScale else 1f

            scale(applyBodyScale) {
                drawHead(s)
                drawEyes(
                    s = s,
                    state = state,
                    isBlinking = isBlinking,
                    pulseScale = pulseScale,
                )
            }
        }
    }
}

private fun DrawScope.drawHead(s: Float) {
    val cornerRadius = s * 0.28f
    val headHeight = s * 0.95f
    val topY = (s - headHeight) / 2f

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(PrimaryPurple, AccentBlue),
            start = Offset(0f, 0f),
            end = Offset(s, headHeight),
        ),
        topLeft = Offset(0f, topY),
        size = Size(s, headHeight),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
    )
}

private fun DrawScope.drawEyes(
    s: Float,
    state: MascotState,
    isBlinking: Boolean,
    pulseScale: Float,
) {
    val eyeOffsetY = s * 0.38f  // vertical center of eyes
    val eyeSpacingHalf = s * 0.16f  // half the gap between eye centres
    val leftX = s / 2f - eyeSpacingHalf - s * 0.065f
    val rightX = s / 2f + eyeSpacingHalf - s * 0.065f

    when (state) {
        MascotState.IDLE -> {
            val eyeW = s * 0.13f
            val eyeH = if (isBlinking) s * 0.02f else s * 0.18f
            val eyeRadius = eyeW / 2f
            listOf(leftX, rightX).forEach { cx ->
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx, eyeOffsetY - eyeH / 2f),
                    size = Size(eyeW, eyeH),
                    cornerRadius = CornerRadius(eyeRadius, eyeRadius),
                )
            }
        }

        MascotState.THINKING -> {
            val r = s * 0.04f
            listOf(leftX + s * 0.065f, rightX + s * 0.065f).forEach { cx ->
                drawCircle(
                    color = Color.White,
                    radius = r,
                    center = Offset(cx, eyeOffsetY),
                )
            }
        }

        MascotState.LEARNING -> {
            val r = s * 0.09f
            listOf(leftX + s * 0.065f, rightX + s * 0.065f).forEach { cx ->
                drawCircle(color = Color.White, radius = r, center = Offset(cx, eyeOffsetY))
                // Pulsing ring
                drawCircle(
                    color = SecondaryPurple.copy(alpha = (1f - (pulseScale - 1f) / 0.4f).coerceIn(0f, 1f)),
                    radius = r * pulseScale,
                    center = Offset(cx, eyeOffsetY),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        MascotState.HAPPY -> {
            // Arc smile eyes — mirrors iOS ArcEye shape
            val arcW = s * 0.18f
            val arcH = s * 0.10f
            listOf(leftX, rightX).forEach { startX ->
                val path = Path().apply {
                    moveTo(startX, eyeOffsetY)
                    quadraticTo(
                        startX + arcW / 2f, eyeOffsetY + arcH,
                        startX + arcW, eyeOffsetY,
                    )
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }

        MascotState.CONFUSED -> {
            val eyeW = s * 0.13f
            val eyeH = s * 0.18f
            val eyeRadius = eyeW / 2f
            listOf(leftX, rightX).forEach { cx ->
                // Rotate -15 degrees around eye center — mirrors iOS .rotationEffect(.degrees(-15))
                withTransform({
                    rotate(-15f, pivot = Offset(cx + eyeW / 2f, eyeOffsetY))
                }) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx, eyeOffsetY - eyeH / 2f),
                        size = Size(eyeW, eyeH),
                        cornerRadius = CornerRadius(eyeRadius, eyeRadius),
                    )
                }
            }
        }

        MascotState.ERROR -> {
            // X eyes
            val r = s * 0.065f
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            listOf(leftX + s * 0.065f, rightX + s * 0.065f).forEach { cx ->
                drawLine(Color.White, Offset(cx - r, eyeOffsetY - r), Offset(cx + r, eyeOffsetY + r), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(Color.White, Offset(cx + r, eyeOffsetY - r), Offset(cx - r, eyeOffsetY + r), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

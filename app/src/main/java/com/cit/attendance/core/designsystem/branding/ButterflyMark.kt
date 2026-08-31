package com.cit.attendance.core.designsystem.branding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cit.attendance.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * The signature brand visual (Section 7).
 *
 * Deliberately restrained: a single line-art mark that breathes and drifts a
 * few pixels. It never travels across the screen and never competes with the
 * login form for attention.
 *
 * @param emphasis 0..1, raised when the user selects a role. It brightens the
 *   mark and widens the wing beat - the butterfly acknowledging the choice
 *   rather than performing.
 */
@Composable
fun ButterflyMark(
    modifier: Modifier = Modifier,
    emphasis: Float = 0f,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion
    val scope = rememberCoroutineScope()

    // A brief extra beat when the user touches the mark.
    val tapPulse = remember { Animatable(0f) }
    val transition = rememberInfiniteTransition(label = "butterfly")

    val staticWing = remember { mutableFloatStateOf(0.5f) }
    val staticDrift = remember { mutableFloatStateOf(0.5f) }

    val wingBeat by if (motion.reduceMotion) {
        staticWing
    } else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "wingBeat",
        )
    }

    val drift by if (motion.reduceMotion) {
        staticDrift
    } else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "drift",
        )
    }

    val markDescription = "Chennai Institute of Technology butterfly emblem"

    Canvas(
        modifier = modifier
            .semantics { contentDescription = markDescription }
            .pointerInput(motion.reduceMotion) {
                if (motion.reduceMotion) return@pointerInput
                detectTapGestures {
                    scope.launch {
                        tapPulse.animateTo(1f, tween(220))
                        tapPulse.animateTo(0f, tween(680))
                    }
                }
            },
    ) {
        val pulse = tapPulse.value
        // Wing closure: 1.0 fully open, lower values fold the wings inward.
        val openness = 0.78f + 0.16f * wingBeat + 0.06f * emphasis - 0.10f * pulse
        val verticalDrift = (drift - 0.5f) * size.minDimension * 0.05f
        val strokeAlpha = 0.55f + 0.35f * emphasis
        val glowAlpha = (0.10f + 0.16f * emphasis) * (1f - 0.3f * pulse)

        withTransform({ translate(top = verticalDrift) }) {
            drawAmbientGlow(accent = colors.accent, alpha = glowAlpha)
            drawButterfly(
                openness = openness,
                strokeBrush = Brush.linearGradient(
                    colors = listOf(
                        colors.accentBright.copy(alpha = strokeAlpha),
                        colors.accent.copy(alpha = strokeAlpha),
                        colors.accentDeep.copy(alpha = strokeAlpha),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
                fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.16f + 0.12f * emphasis),
                        colors.accentDeep.copy(alpha = 0.04f),
                    ),
                ),
            )
        }
    }
}

private fun DrawScope.drawAmbientGlow(accent: Color, alpha: Float) {
    val radius = size.minDimension * 0.62f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = alpha), accent.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * Draws the mark from a normalised 100x100 design box so the geometry stays
 * identical at every size (Section 40).
 */
private fun DrawScope.drawButterfly(
    openness: Float,
    strokeBrush: Brush,
    fillBrush: Brush,
) {
    val box = size.minDimension
    val unit = box / 100f
    val originX = center.x - box / 2f
    val originY = center.y - box / 2f
    val strokeWidth = (box * 0.014f).coerceAtLeast(1.2f)
    val safeOpenness = openness.coerceAtLeast(0.4f)

    withTransform({ translate(left = originX, top = originY) }) {
        // One wing definition, mirrored, so the mark is exactly symmetric and
        // only a single path has to be maintained.
        listOf(1f, -1f).forEach { side ->
            withTransform({
                scale(
                    scaleX = side * safeOpenness,
                    scaleY = 1f,
                    pivot = Offset(50f * unit, 50f * unit),
                )
            }) {
                val wings = wingPath(unit)
                drawPath(path = wings, brush = fillBrush)
                drawPath(
                    path = wings,
                    brush = strokeBrush,
                    // Counter-scale so the stroke keeps an even weight as the
                    // wings fold.
                    style = Stroke(width = strokeWidth / safeOpenness),
                )
            }
        }
        drawBody(unit, strokeBrush, strokeWidth)
    }
}

private fun wingPath(u: Float): Path = Path().apply {
    // Upper wing
    moveTo(50f * u, 44f * u)
    cubicTo(57f * u, 17f * u, 77f * u, 3f * u, 89f * u, 11f * u)
    cubicTo(99f * u, 18f * u, 92f * u, 41f * u, 67f * u, 50f * u)
    cubicTo(59f * u, 53f * u, 51f * u, 50f * u, 50f * u, 44f * u)
    close()

    // Lower wing
    moveTo(50f * u, 51f * u)
    cubicTo(61f * u, 53f * u, 77f * u, 60f * u, 79f * u, 74f * u)
    cubicTo(80f * u, 85f * u, 69f * u, 91f * u, 61f * u, 85f * u)
    cubicTo(54f * u, 79f * u, 50f * u, 66f * u, 50f * u, 51f * u)
    close()
}

private fun DrawScope.drawBody(u: Float, brush: Brush, strokeWidth: Float) {
    val body = Path().apply {
        moveTo(50f * u, 36f * u)
        cubicTo(52f * u, 48f * u, 52f * u, 64f * u, 50f * u, 76f * u)
        cubicTo(48f * u, 64f * u, 48f * u, 48f * u, 50f * u, 36f * u)
        close()
    }
    drawPath(body, brush)

    val antennae = Path().apply {
        moveTo(50f * u, 37f * u)
        cubicTo(47f * u, 30f * u, 43f * u, 26f * u, 39f * u, 24f * u)
        moveTo(50f * u, 37f * u)
        cubicTo(53f * u, 30f * u, 57f * u, 26f * u, 61f * u, 24f * u)
    }
    drawPath(
        path = antennae,
        brush = brush,
        style = Stroke(width = strokeWidth * 0.7f),
    )
}

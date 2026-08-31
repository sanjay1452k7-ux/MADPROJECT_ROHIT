package com.cit.attendance.core.designsystem.branding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.designsystem.theme.CitTheme

/**
 * Original institutional emblem (Section 1 branding note).
 *
 * An abstract hexagonal aperture with a stylised monogram stroke. It is
 * intentionally generic-institutional geometry and is not a reproduction of any
 * real crest or seal.
 */
@Composable
fun CitEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val colors = AppTheme.colors
    Canvas(modifier = modifier.size(size)) {
        val box = this.size.minDimension
        val u = box / 100f
        val stroke = (box * 0.055f).coerceAtLeast(1.5f)

        val brush = Brush.linearGradient(
            colors = colors.accentGradient,
            start = Offset(0f, 0f),
            end = Offset(box, box),
        )

        // Hexagonal shield
        val hex = Path().apply {
            moveTo(50f * u, 6f * u)
            lineTo(88f * u, 28f * u)
            lineTo(88f * u, 72f * u)
            lineTo(50f * u, 94f * u)
            lineTo(12f * u, 72f * u)
            lineTo(12f * u, 28f * u)
            close()
        }
        drawPath(hex, brush, style = Stroke(width = stroke))

        // Monogram: an open C, left deliberately unclosed to read as an aperture.
        val monogram = Path().apply {
            moveTo(64f * u, 36f * u)
            cubicTo(52f * u, 28f * u, 36f * u, 36f * u, 36f * u, 50f * u)
            cubicTo(36f * u, 64f * u, 52f * u, 72f * u, 64f * u, 64f * u)
        }
        drawPath(
            monogram,
            Brush.linearGradient(listOf(colors.textPrimary, colors.textPrimary)),
            style = Stroke(width = stroke * 0.9f),
        )
    }
}

/**
 * Emblem plus institution wordmark, used on the splash and login surfaces.
 * The whole lockup is announced as a single label to screen readers.
 */
@Composable
fun CitBrandLockup(
    institutionName: String,
    modifier: Modifier = Modifier,
    emblemSize: Dp = 56.dp,
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val spacing = AppTheme.spacing

    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = institutionName
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        CitEmblem(size = emblemSize)

        Text(
            text = institutionName.uppercase(),
            style = typography.overline,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070A)
@Composable
private fun CitBrandLockupPreview() {
    CitTheme {
        CitBrandLockup(institutionName = "Chennai Institute of Technology")
    }
}

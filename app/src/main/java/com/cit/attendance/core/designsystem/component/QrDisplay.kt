package com.cit.attendance.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.createBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppGradients
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders a server-minted attendance token as a QR code (Section 19).
 *
 * The payload is the opaque token and nothing else: no name, register number,
 * email or any other student detail is encoded, so a photographed code reveals
 * nothing about the person holding it (Section 19).
 *
 * The code is drawn on a white plate because scanners need the quiet zone and
 * the contrast; everything around it stays in the product's dark palette.
 */
@Composable
fun QrDisplay(
    token: String,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    /** Dims and shrinks slightly as the code approaches expiry. */
    freshness: Float = 1f,
) {
    val image = remember(token) { encodeAttendanceQr(token) }

    val scale by animateFloatAsState(
        targetValue = 0.97f + 0.03f * freshness.coerceIn(0f, 1f),
        animationSpec = tween(AppTheme.motion.duration(AppTheme.motion.quick)),
        label = "qrScale",
    )

    val colors = AppTheme.colors
    val shapes = AppTheme.shapes

    Box(
        modifier = modifier.scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        // The halo, drawn behind and slightly larger than the plate.
        Box(
            modifier = Modifier
                .size(size + AppTheme.spacing.xxl)
                .background(AppGradients.glow(alpha = freshness.coerceIn(0f, 1f)), CircleShape),
        )

        Box(
            modifier = Modifier
                .clip(shapes.extraLarge)
                .background(AppGradients.diagonal())
                // The gradient shows only as a border: the padding is the
                // stroke width.
                .padding(2.dp)
                .clip(shapes.extraLarge)
                .background(colors.surface)
                .padding(AppTheme.spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shapes.large)
                    .background(Color.White)
                    .padding(AppTheme.spacing.sm)
                    .semantics { contentDescription = "Your attendance QR code" },
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(size)
                            .alpha(0.45f + 0.55f * freshness.coerceIn(0f, 1f)),
                    )
                }
            }
        }
    }
}

/**
 * Encodes a token to a black-and-white bitmap.
 *
 * Returns null rather than throwing if the payload cannot be encoded: a QR that
 * fails to render is a blank plate the student can refresh, not a crash.
 *
 * Internal rather than private so the instrumented round-trip test can prove
 * that what this produces is what ML Kit reads back.
 */
internal fun encodeAttendanceQr(token: String, sizePx: Int = 512): ImageBitmap? = runCatching {
    val hints = mapOf(
        // High correction tolerates a scuffed or partly obscured screen.
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
        EncodeHintType.MARGIN to 1,
    )
    val matrix = QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

    val bitmap = createBitmap(matrix.width, matrix.height)
    val black = android.graphics.Color.BLACK
    val white = android.graphics.Color.WHITE
    val row = IntArray(matrix.width)

    for (y in 0 until matrix.height) {
        for (x in 0 until matrix.width) {
            row[x] = if (matrix.get(x, y)) black else white
        }
        bitmap.setPixels(row, 0, matrix.width, 0, y, matrix.width, 1)
    }
    bitmap.asImageBitmap()
}.getOrNull()

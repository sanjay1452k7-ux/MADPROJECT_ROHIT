package com.cit.attendance.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cit.attendance.core.designsystem.theme.AppTheme
import kotlin.math.abs

/**
 * A deterministic, generated avatar (Section 26).
 *
 * Section 26 calls for a seeded illustrated-avatar generator such as DiceBear.
 * That is an HTTP service, and Milestone 1 makes no network calls at all, so the
 * same guarantee is met locally: the seed - a register number or employee code -
 * selects a fixed hue pair, so a person always renders identically, and no image
 * of a real identifiable person is ever used.
 *
 * Milestone 2 can swap the painted disc for a real photograph served by the
 * backend without any caller changing, since the seed and size are the only
 * inputs.
 */
@Composable
fun SeededAvatar(
    seed: String,
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    highlighted: Boolean = false,
) {
    val colors = AppTheme.colors

    // Two related hues derived from the seed, kept inside the brand's purple to
    // blue-violet range so avatars never fight the palette.
    val brush = remember(seed) {
        val hash = abs(seed.hashCode())
        val hue = 250f + (hash % 60)
        Brush.linearGradient(
            listOf(
                Color.hsl(hue, 0.55f, 0.42f),
                Color.hsl((hue + 24f) % 360f, 0.62f, 0.58f),
            ),
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) colors.accentBright else colors.outline,
                shape = CircleShape,
            )
            // The surrounding row already names the person; the avatar itself
            // would only repeat it for a screen reader.
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = AppTheme.typography.label.copy(fontSize = (size.value * 0.34f).sp),
            color = colors.onAccent,
        )
    }
}

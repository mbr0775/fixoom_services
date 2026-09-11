package com.fixoom.customer.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.fixoom.customer.ui.theme.FixoomMint
import com.fixoom.customer.ui.theme.FixoomTeal

/**
 * Decorative layers behind the full-width header, within its measured bounds.
 * Paths are cached until size/theme changes. They don't add touch targets,
 * ongoing animation, or extra work to the video carousel.
 */
internal fun Modifier.homeHeaderWaves(darkTheme: Boolean): Modifier = drawWithCache {
    val width = size.width
    val bottom = size.height
    val waveDepth = 52.dp.toPx().coerceAtMost(bottom * 0.36f)

    val backWave = Path().apply {
        moveTo(0f, bottom - waveDepth * 0.80f)
        cubicTo(
            width * 0.28f, bottom - waveDepth * 0.15f,
            width * 0.66f, bottom - waveDepth * 1.30f,
            width, bottom - waveDepth * 0.70f
        )
        lineTo(width, bottom)
        lineTo(0f, bottom)
        close()
    }

    val frontWave = Path().apply {
        moveTo(0f, bottom - waveDepth * 0.25f)
        cubicTo(
            width * 0.34f, bottom - waveDepth * 0.70f,
            width * 0.68f, bottom + waveDepth * 0.08f,
            width, bottom - waveDepth * 0.42f
        )
        lineTo(width, bottom)
        lineTo(0f, bottom)
        close()
    }
    // Just teal and soft green: two broad fills, without an outline or glow.
    val backColor = if (darkTheme) {
        lerp(FixoomTeal, FixoomMint, 0.12f)
    } else {
        FixoomTeal.copy(alpha = 0.14f)
    }
    val frontColor = if (darkTheme) {
        FixoomMint.copy(alpha = 0.14f)
    } else {
        FixoomMint.copy(alpha = 0.88f)
    }

    onDrawBehind {
        drawPath(backWave, backColor)
        drawPath(frontWave, frontColor)
    }
}

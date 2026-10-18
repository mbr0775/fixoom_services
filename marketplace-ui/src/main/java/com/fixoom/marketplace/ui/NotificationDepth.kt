package com.fixoom.marketplace.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val InboxTeal = Color(0xFF143F45)
internal val InboxLime = Color(0xFFC8F43D)

@Composable
internal fun NotificationHero(
    summary: String,
    motion: Boolean,
    floating: Boolean,
    scrollOffset: () -> Float,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = InboxTeal,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Canvas(
                Modifier.matchParentSize()
                    .clearAndSetSemantics {}
                    .graphicsLayer {
                        translationY =
                            if (motion) (scrollOffset() * .12f).coerceIn(0f, 20.dp.toPx()) else 0f
                    }
            ) {
                drawCircle(
                    Color.White.copy(alpha = .035f),
                    size.width * .55f,
                    Offset(size.width * .95f, size.height * .7f),
                )
                drawArc(
                    InboxLime.copy(alpha = .1f),
                    210f,
                    245f,
                    false,
                    Offset(size.width * .48f, -size.height * .2f),
                    Size(size.width * .8f, size.height * 1.4f),
                    style = Stroke(1.dp.toPx()),
                )
            }
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "YOUR FIXOOM INBOX",
                    color = Color(0xFFB5D3CD),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold,
                )
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val largeText =
                        androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.25f
                    if (maxWidth < 260.dp || largeText) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NotificationBell(Modifier.size(88.dp), motion, floating, scrollOffset)
                            HeroWords(summary)
                        }
                    } else
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(Modifier.weight(1f)) { HeroWords(summary) }
                            NotificationBell(Modifier.size(112.dp), motion, floating, scrollOffset)
                        }
                }
                Text(
                    "Requests, replies and every step of your service.",
                    color = Color(0xFFD3E6E1),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HeroWords(summary: String) {
    Text(
        "Stay in\nthe loop.",
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.headlineLarge,
    )
    Spacer(Modifier.height(12.dp))
    Surface(shape = RoundedCornerShape(50), color = InboxLime) {
        Text(
            summary,
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = InboxTeal,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Cached vector surfaces; float and perspective are compositor transforms. */
@Composable
private fun NotificationBell(
    modifier: Modifier,
    motion: Boolean,
    floating: Boolean,
    scrollOffset: () -> Float,
) {
    val float =
        if (floating) {
            val loop = rememberInfiniteTransition(label = "inbox_bell_float")
            loop.animateFloat(
                -1f,
                1f,
                infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "inbox_bell_lift",
            )
        } else rememberUpdatedState(0f)
    Box(modifier.clearAndSetSemantics {}) {
        Canvas(Modifier.matchParentSize()) {
            drawOval(
                Color.Black.copy(alpha = .18f),
                Offset(size.width * .22f, size.height * .83f),
                Size(size.width * .65f, size.height * .13f),
            )
            drawOval(
                Color.White.copy(alpha = .12f),
                Offset(size.width * .05f, size.height * .7f),
                Size(size.width * .9f, size.height * .24f),
                style = Stroke(1.dp.toPx()),
            )
        }
        Box(
            Modifier.matchParentSize()
                .graphicsLayer {
                    translationY =
                        if (motion)
                            float.value * 4.dp.toPx() -
                                (scrollOffset() * .08f).coerceIn(0f, 10.dp.toPx())
                        else 0f
                    rotationX = if (motion) 9f + float.value * 2f else 7f
                    rotationY =
                        if (motion) -15f + (scrollOffset() / density * .06f).coerceIn(0f, 9f)
                        else -12f
                    rotationZ = -10f
                    cameraDistance = 18 * density
                }
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val bell =
                        Path().apply {
                            moveTo(w * .2f, h * .65f)
                            cubicTo(w * .32f, h * .57f, w * .23f, h * .38f, w * .36f, h * .27f)
                            cubicTo(w * .44f, h * .19f, w * .61f, h * .2f, w * .68f, h * .29f)
                            cubicTo(w * .79f, h * .42f, w * .7f, h * .55f, w * .82f, h * .65f)
                            cubicTo(w * .7f, h * .74f, w * .33f, h * .74f, w * .2f, h * .65f)
                            close()
                        }
                    val highlight =
                        Path().apply {
                            moveTo(w * .37f, h * .48f)
                            cubicTo(w * .37f, h * .36f, w * .4f, h * .3f, w * .48f, h * .29f)
                        }
                    val material =
                        Brush.linearGradient(
                            listOf(Color(0xFFF0FFB0), InboxLime, Color(0xFF8DB524)),
                            Offset(w * .25f, h * .2f),
                            Offset(w * .85f, h * .75f),
                        )
                    onDrawBehind {
                        drawCircle(Color(0xFF719425), w * .061f, Offset(w * .55f, h * .78f))
                        drawCircle(InboxLime, w * .06f, Offset(w * .51f, h * .75f))
                        drawRoundRect(
                            Color(0xFF8DB524),
                            Offset(w * .47f, h * .13f),
                            Size(w * .1f, h * .13f),
                            androidx.compose.ui.geometry.CornerRadius(w * .05f),
                        )
                        translate(w * .035f, h * .045f) { drawPath(bell, Color(0xFF6D8F22)) }
                        drawPath(bell, material)
                        drawOval(
                            Color(0xFFDFFF72),
                            Offset(w * .22f, h * .62f),
                            Size(w * .59f, h * .1f),
                        )
                        drawPath(
                            highlight,
                            Color.White.copy(alpha = .65f),
                            style = Stroke(w * .035f, cap = StrokeCap.Round),
                        )
                        drawCircle(
                            Color.White.copy(alpha = .8f),
                            w * .025f,
                            Offset(w * .82f, h * .3f),
                        )
                        drawCircle(
                            InboxLime.copy(alpha = .6f),
                            w * .018f,
                            Offset(w * .18f, h * .38f),
                        )
                    }
                }
        )
    }
}

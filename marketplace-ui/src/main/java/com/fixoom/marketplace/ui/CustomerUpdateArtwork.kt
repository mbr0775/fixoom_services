package com.fixoom.marketplace.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Isometric shapes use cached display lists; idle float and scroll depth only transform layers. */
@Composable
internal fun CustomerUpdateArtwork(
    kind: String,
    accent: Color,
    modifier: Modifier,
    motion: Boolean,
    floating: Boolean,
    depth: () -> Float,
    pressure: () -> Float,
) {
    val lift =
        if (floating) {
            val loop = rememberInfiniteTransition(label = "update_art_float")
            loop.animateFloat(
                -1f,
                1f,
                infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "update_art_lift",
            )
        } else rememberUpdatedState(0f)
    Box(modifier.clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawOval(
                Color.Black.copy(alpha = .17f),
                Offset(size.width * .15f, size.height * .84f),
                Size(size.width * .7f, size.height * .12f),
            )
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationY =
                    if (motion)
                        lift.value * 3.dp.toPx() - pressure() * 4.dp.toPx() - depth() * 4.dp.toPx()
                    else 0f
                rotationY = if (motion) -10f + depth() * 13f else -10f
                rotationX = if (motion) 8f + lift.value * 2f else 8f
                rotationZ = if (motion) -7f + lift.value * 3f else -7f
                cameraDistance = 24 * density
            },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                when (kind) {
                    "requested" -> drawToolCase(accent)
                    "message" -> drawReplyBubble(accent)
                    else -> drawSeal(accent)
                }
            }
            if (kind != "requested" && kind != "message")
                Icon(
                    notificationArtwork(kind).first,
                    null,
                    Modifier.offset(y = (-5).dp).size(34.dp),
                    tint = InboxTeal,
                )
        }
    }
}

private fun DrawScope.polygon(color: Color, vararg points: Pair<Float, Float>) {
    val path =
        Path().apply {
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) moveTo(x * size.width, y * size.height)
                else lineTo(x * size.width, y * size.height)
            }
            close()
        }
    drawPath(path, color)
}

private fun DrawScope.drawToolCase(accent: Color) {
    val w = size.width
    val h = size.height
    val metal = Color(0xFFE2EEEA)
    drawLine(
        metal,
        Offset(w * .23f, h * .42f),
        Offset(w * .17f, h * .19f),
        w * .065f,
        StrokeCap.Round,
    )
    drawArc(
        metal,
        40f,
        270f,
        false,
        Offset(w * .06f, h * .05f),
        Size(w * .23f, h * .23f),
        style = Stroke(w * .065f, cap = StrokeCap.Round),
    )
    drawLine(
        Color(0xFFC3B1ED),
        Offset(w * .76f, h * .45f),
        Offset(w * .82f, h * .15f),
        w * .06f,
        StrokeCap.Round,
    )
    drawLine(
        metal,
        Offset(w * .82f, h * .15f),
        Offset(w * .84f, h * .07f),
        w * .027f,
        StrokeCap.Round,
    )
    val handle =
        Path().apply {
            moveTo(w * .36f, h * .37f)
            lineTo(w * .36f, h * .24f)
            quadraticTo(w * .36f, h * .16f, w * .45f, h * .18f)
            lineTo(w * .61f, h * .24f)
            quadraticTo(w * .65f, h * .26f, w * .65f, h * .32f)
            lineTo(w * .65f, h * .43f)
        }
    drawPath(
        handle,
        lerp(accent, Color.White, .5f),
        style = Stroke(w * .06f, cap = StrokeCap.Round),
    )
    polygon(lerp(accent, Color.White, .4f), .08f to .45f, .57f to .34f, .92f to .52f, .43f to .65f)
    polygon(lerp(accent, InboxTeal, .25f), .43f to .65f, .92f to .52f, .92f to .83f, .43f to .96f)
    polygon(accent, .08f to .45f, .43f to .65f, .43f to .96f, .08f to .77f)
    drawLine(
        Color.White.copy(alpha = .7f),
        Offset(w * .08f, h * .45f),
        Offset(w * .43f, h * .65f),
        1.dp.toPx(),
    )
    drawLine(
        InboxTeal.copy(alpha = .18f),
        Offset(w * .46f, h * .73f),
        Offset(w * .89f, h * .62f),
        2.dp.toPx(),
    )
    polygon(Color(0xFFF0D5A5), .22f to .59f, .3f to .63f, .3f to .73f, .22f to .69f)
}

private fun DrawScope.drawSeal(accent: Color) {
    val w = size.width
    val h = size.height
    drawCircle(lerp(accent, InboxTeal, .3f), w * .34f, Offset(w * .54f, h * .49f))
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White, accent, lerp(accent, InboxTeal, .1f)),
            center = Offset(w * .3f, h * .2f),
            radius = w * .72f,
        ),
        w * .34f,
        Offset(w * .5f, h * .43f),
    )
    drawCircle(
        Color.White.copy(alpha = .45f),
        w * .29f,
        Offset(w * .5f, h * .43f),
        style = Stroke(1.dp.toPx()),
    )
    drawCircle(Color.White.copy(alpha = .7f), w * .025f, Offset(w * .27f, h * .21f))
}

private fun DrawScope.drawReplyBubble(accent: Color) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(w * .14f)
    drawRoundRect(
        lerp(accent, Color.White, .3f),
        Offset(w * .29f, h * .13f),
        Size(w * .62f, h * .43f),
        corner,
    )
    drawRoundRect(
        lerp(accent, InboxTeal, .3f),
        Offset(w * .13f, h * .37f),
        Size(w * .7f, h * .43f),
        corner,
    )
    polygon(lerp(accent, InboxTeal, .3f), .3f to .72f, .34f to .94f, .52f to .74f)
    polygon(accent, .28f to .68f, .31f to .88f, .5f to .72f)
    drawRoundRect(
        Brush.linearGradient(listOf(Color.White, accent)),
        Offset(w * .1f, h * .31f),
        Size(w * .7f, h * .43f),
        corner,
    )
    drawRoundRect(
        Color.White.copy(alpha = .6f),
        Offset(w * .1f, h * .31f),
        Size(w * .7f, h * .43f),
        corner,
        style = Stroke(1.dp.toPx()),
    )
    for (dot in 0..2) drawCircle(InboxTeal, w * .035f, Offset(w * (.28f + dot * .17f), h * .53f))
}

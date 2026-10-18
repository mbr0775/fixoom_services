package com.fixoom.marketplace.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Three independent perspective planes and a lit bell orb; no bitmap/video decoder or sensors. */
@Composable
internal fun CustomerInboxSculpture(
    modifier: Modifier,
    motion: Boolean,
    floating: Boolean,
    scrollOffset: () -> Float,
) {
    val lift =
        if (floating) {
            val loop = rememberInfiniteTransition(label = "customer_inbox_float")
            loop.animateFloat(
                -1f,
                1f,
                infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "customer_inbox_lift",
            )
        } else rememberUpdatedState(0f)
    Box(modifier.clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        Color(0xFFD9EDDA).copy(alpha = .75f),
                        Color(0xFFD9EDDA).copy(alpha = 0f),
                    ),
                    radius = size.minDimension * .5f,
                ),
                size.minDimension * .5f,
                Offset(size.width * .52f, size.height * .54f),
            )
            drawOval(
                InboxTeal.copy(alpha = .09f),
                Offset(size.width * .15f, size.height * .83f),
                Size(size.width * .75f, size.height * .09f),
            )
        }
        // The back planes reveal solid edges as the scroll perspective changes.
        for (layer in 2 downTo 0) {
            val shade =
                when (layer) {
                    2 -> Color(0xFFB3A6E0)
                    1 -> Color(0xFFCAE4D3)
                    else -> Color(0xFFF7FBF6)
                }
            Box(
                Modifier.size(102.dp, 124.dp)
                    .graphicsLayer {
                        val scroll =
                            if (motion) (scrollOffset() / density * .08f).coerceIn(0f, 14f) else 0f
                        translationX = (layer * 9 - 9).dp.toPx()
                        translationY =
                            (layer * -7 + 3).dp.toPx() +
                                if (motion)
                                    lift.value * (3 + layer).dp.toPx() - scroll * (layer + 1)
                                else 0f
                        rotationX = 34f + scroll * .3f
                        rotationY = -22f + scroll
                        rotationZ = -19f + layer * 3f
                        cameraDistance = 22 * density
                        shadowElevation = if (layer == 0) 2.dp.toPx() else 0f
                        ambientShadowColor = InboxTeal.copy(alpha = .12f)
                        spotShadowColor = InboxTeal.copy(alpha = .16f)
                        shape = RoundedCornerShape(17.dp)
                    }
                    .drawWithCache {
                        val corner = CornerRadius(17.dp.toPx())
                        val face =
                            Brush.linearGradient(
                                listOf(Color.White, shade, shade.copy(alpha = .95f))
                            )
                        onDrawBehind {
                            drawRoundRect(
                                if (layer == 2) Color(0xFF8576B2) else Color(0xFF8DB4A7),
                                topLeft = Offset(4.dp.toPx(), 5.dp.toPx()),
                                cornerRadius = corner,
                            )
                            drawRoundRect(face, cornerRadius = corner)
                            drawRoundRect(
                                Color.White.copy(alpha = .8f),
                                cornerRadius = corner,
                                style = Stroke(1.dp.toPx()),
                            )
                            if (layer == 0) {
                                drawCircle(
                                    InboxTeal,
                                    size.width * .16f,
                                    Offset(size.width * .3f, size.height * .29f),
                                )
                                drawLine(
                                    Color.White,
                                    Offset(size.width * .23f, size.height * .29f),
                                    Offset(size.width * .29f, size.height * .34f),
                                    3.dp.toPx(),
                                    StrokeCap.Round,
                                )
                                drawLine(
                                    Color.White,
                                    Offset(size.width * .29f, size.height * .34f),
                                    Offset(size.width * .39f, size.height * .23f),
                                    3.dp.toPx(),
                                    StrokeCap.Round,
                                )
                                for (line in 0..2) drawRoundRect(
                                    InboxTeal.copy(alpha = if (line == 0) .23f else .1f),
                                    Offset(size.width * .17f, size.height * (.57f + line * .105f)),
                                    Size(size.width * (if (line == 2) .42f else .65f), 4.dp.toPx()),
                                    CornerRadius(2.dp.toPx()),
                                )
                            }
                        }
                    }
            )
        }
        // A separate front glass-like message plane travels faster than the stack behind it.
        Box(
            Modifier.align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 16.dp)
                .size(100.dp, 47.dp)
                .graphicsLayer {
                    translationY =
                        if (motion)
                            -lift.value * 4.dp.toPx() -
                                (scrollOffset() * .11f).coerceIn(0f, 15.dp.toPx())
                        else 0f
                    rotationZ = 9f
                    rotationY = -12f
                    rotationX = 8f
                    shadowElevation = 3.dp.toPx()
                    ambientShadowColor = InboxTeal.copy(alpha = .12f)
                    spotShadowColor = InboxTeal.copy(alpha = .16f)
                    shape = RoundedCornerShape(14.dp)
                    clip = true
                }
                .background(Brush.linearGradient(listOf(Color.White, Color(0xFFE6ECE9))))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Color(0xFFCEED58),
                    9.dp.toPx(),
                    Offset(size.width * .2f, size.height * .5f),
                )
                drawRoundRect(
                    InboxTeal.copy(alpha = .7f),
                    Offset(size.width * .38f, size.height * .34f),
                    Size(size.width * .41f, 4.dp.toPx()),
                    CornerRadius(2.dp.toPx()),
                )
                drawRoundRect(
                    InboxTeal.copy(alpha = .15f),
                    Offset(size.width * .38f, size.height * .58f),
                    Size(size.width * .28f, 3.dp.toPx()),
                    CornerRadius(2.dp.toPx()),
                )
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 4.dp)
                .size(64.dp)
                .graphicsLayer {
                    translationY =
                        if (motion)
                            -lift.value * 6.dp.toPx() -
                                (scrollOffset() * .05f).coerceIn(0f, 8.dp.toPx())
                        else 0f
                    rotationZ = 14f + if (motion) lift.value * 3f else 0f
                    rotationY = -15f
                    rotationX = 12f
                    shadowElevation = 4.dp.toPx()
                    ambientShadowColor = InboxTeal.copy(alpha = .12f)
                    spotShadowColor = InboxTeal.copy(alpha = .16f)
                    shape = CircleShape
                    clip = true
                }
                .drawWithCache {
                    val orb =
                        Brush.radialGradient(
                            listOf(Color(0xFFF2FFB7), InboxLime, Color(0xFF8CA92C)),
                            center = Offset(size.width * .3f, size.height * .2f),
                            radius = size.width * .9f,
                        )
                    onDrawBehind { drawCircle(orb) }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(30.dp), tint = InboxTeal)
        }
    }
}

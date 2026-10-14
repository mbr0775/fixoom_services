package com.fixoom.provider.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixoom.provider.ui.theme.FixoomLime
import com.fixoom.provider.ui.theme.FixoomTealDark

/** Scroll is read in graphics layers, so parallax does not recompose the screen per frame. */
@Composable
fun ProviderDepthHero(
    title: String,
    eyebrow: String,
    caption: String,
    scrollOffset: () -> Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    motionEnabled: Boolean = rememberProviderMotionAllowed()
) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(FixoomTealDark)) {
        Column(Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp)) {
            Text(eyebrow, color = FixoomLime, style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(title, color = Color.White,
                style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold)
        }
        ToolboxScene(scrollOffset, motionEnabled, Modifier.fillMaxWidth().height(if (compact) 164.dp else 206.dp))
        Text(caption, Modifier.padding(start = 24.dp, end = 24.dp, bottom = 22.dp),
            color = Color(0xFFCCDCDA), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ToolboxScene(scrollOffset: () -> Float, enabled: Boolean, modifier: Modifier) {
    val view = LocalView.current
    var inViewport by remember { mutableStateOf(true) }
    val moving = enabled && inViewport
    val float = providerFloat(moving)
    Box(modifier.onGloballyPositioned {
        val bounds = it.boundsInWindow()
        inViewport = bounds.bottom > 0f && bounds.top < view.rootView.height && bounds.height > 0f
    }.clearAndSetSemantics { }) {
        Canvas(Modifier.fillMaxSize().graphicsLayer {
            translationY = if (moving) (scrollOffset() * .24f).coerceAtMost(20.dp.toPx()) else 0f
        }) {
            val center = Offset(size.width * .53f, size.height * .56f)
            drawOval(Color.White.copy(alpha = .055f), Offset(center.x - size.width * .35f, center.y - size.height * .42f),
                Size(size.width * .7f, size.height * .8f), style = Stroke(1.dp.toPx()))
            drawOval(Color.White.copy(alpha = .035f), Offset(center.x - size.width * .44f, center.y - size.height * .48f),
                Size(size.width * .88f, size.height * .95f), style = Stroke(1.dp.toPx()))
        }
        Canvas(Modifier.align(Alignment.Center).fillMaxWidth(.84f).fillMaxHeight(.98f).graphicsLayer {
            translationY = if (moving) float.value * 5.dp.toPx() + (scrollOffset() * .1f).coerceAtMost(10.dp.toPx()) else 0f
            rotationX = if (moving) float.value * 2.5f else 0f
            rotationY = if (moving) float.value * 4f else 0f
            rotationZ = if (moving) float.value * 1.4f else 0f
            cameraDistance = 18 * density
        }) {
            val factor = minOf(size.width / 320f, size.height / 230f)
            translate((size.width - 320 * factor) / 2, (size.height - 230 * factor) / 2) {
                scale(factor, factor, Offset.Zero) {
                    drawOval(Color.Black.copy(alpha = .18f), Offset(65f, 194f), Size(212f, 25f))
                    // Tools have their own bevels and shadows behind the box.
                    rotate(-22f, Offset(115f, 105f)) {
                        drawLine(Color(0xFF324D52), Offset(110f, 131f), Offset(110f, 49f), 19f, StrokeCap.Round)
                        drawLine(Brush.linearGradient(listOf(Color(0xFFE2EEEB), Color(0xFF91AEAF))),
                            Offset(106f, 128f), Offset(106f, 49f), 14f, StrokeCap.Round)
                        drawArc(Color(0xFFE2EEEB), 15f, 290f, false, Offset(85f, 19f), Size(42f, 43f), style = Stroke(13f, cap = StrokeCap.Round))
                        drawCircle(Color(0xFF637F83), 3f, Offset(106f, 122f))
                    }
                    rotate(24f, Offset(205f, 90f)) {
                        drawLine(Color(0xFF92AFAE), Offset(204f, 109f), Offset(204f, 25f), 6f, StrokeCap.Round)
                        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFDDFC76), Color(0xFFA2C92A))),
                            Offset(191f, 67f), Size(25f, 58f), CornerRadius(10f))
                        drawLine(Color.White.copy(alpha = .3f), Offset(197f, 78f), Offset(197f, 113f), 3f, StrokeCap.Round)
                    }
                    fun face(points: List<Offset>, brush: Brush) {
                        val path = Path().apply { moveTo(points[0].x, points[0].y); points.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                        drawPath(path, brush)
                    }
                    face(listOf(Offset(63f, 106f), Offset(174f, 144f), Offset(174f, 205f), Offset(63f, 166f)),
                        Brush.linearGradient(listOf(Color(0xFFF2FBF4), Color(0xFFC0DDCA)), Offset(70f, 105f), Offset(150f, 210f)))
                    face(listOf(Offset(174f, 144f), Offset(263f, 102f), Offset(263f, 163f), Offset(174f, 205f)),
                        Brush.linearGradient(listOf(Color(0xFFB7A5E5), Color(0xFF8676B5)), Offset(190f, 120f), Offset(267f, 188f)))
                    face(listOf(Offset(63f, 106f), Offset(151f, 64f), Offset(263f, 102f), Offset(174f, 144f)),
                        Brush.linearGradient(listOf(Color(0xFFEEF8E9), Color(0xFFDCECC9)), Offset(130f, 70f), Offset(170f, 140f)))
                    drawLine(Color(0xFF6A927F).copy(alpha = .5f), Offset(64f, 121f), Offset(174f, 159f), 2f)
                    drawLine(Color(0xFF615487).copy(alpha = .5f), Offset(174f, 159f), Offset(262f, 118f), 2f)
                    val handle = Path().apply {
                        moveTo(126f, 98f); lineTo(126f, 74f); quadraticTo(126f, 65f, 134f, 68f)
                        lineTo(192f, 87f); quadraticTo(199f, 90f, 199f, 98f); lineTo(199f, 119f)
                    }
                    drawPath(handle, Color(0xFF355E5A), style = Stroke(8f, cap = StrokeCap.Round))
                    drawLine(Color(0xFF6D9189), Offset(134f, 72f), Offset(189f, 90f), 2f, StrokeCap.Round)
                    // A raised latch and the small Fixoom mark finish the front face.
                    face(listOf(Offset(109f, 137f), Offset(128f, 144f), Offset(128f, 163f), Offset(109f, 156f)),
                        Brush.verticalGradient(listOf(Color(0xFFF0C489), Color(0xFFC19864))))
                    drawLine(Color(0xFFFFE4B9), Offset(113f, 143f), Offset(123f, 147f), 2f)
                    drawLine(Color(0xFF255451), Offset(79f, 135f), Offset(79f, 151f), 4f, StrokeCap.Round)
                    drawLine(Color(0xFF255451), Offset(79f, 135f), Offset(92f, 139f), 4f, StrokeCap.Round)
                    drawLine(Color(0xFF255451), Offset(79f, 143f), Offset(88f, 146f), 3f, StrokeCap.Round)
                }
            }
        }
        FloatingBadge(Icons.Rounded.Check, Color(0xFFCDEED8), Modifier.align(Alignment.CenterStart)
            .padding(start = 25.dp).graphicsLayer {
                translationY = if (moving) -float.value * 7.dp.toPx() + (scrollOffset() * .18f).coerceAtMost(14.dp.toPx()) else 0f
                rotationZ = if (moving) -8f + float.value * 3f else -8f
            })
        FloatingBadge(Icons.Rounded.Bolt, Color(0xFFD9CCFF), Modifier.align(Alignment.TopEnd)
            .padding(end = 24.dp, top = 12.dp).graphicsLayer {
                translationY = if (moving) float.value * 8.dp.toPx() + (scrollOffset() * .3f).coerceAtMost(22.dp.toPx()) else 0f
                rotationZ = if (moving) 12f - float.value * 3f else 12f
            })
    }
}

@Composable
private fun FloatingBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier.size(41.dp), shape = RoundedCornerShape(14.dp), color = color,
        shadowElevation = 6.dp) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = FixoomTealDark, modifier = Modifier.size(22.dp))
        }
    }
}

package com.fixoom.provider.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixoom.provider.R
import com.fixoom.provider.ui.theme.FixoomLime
import com.fixoom.provider.ui.theme.FixoomTealDark

private enum class WorkGlyph {
    Requests,
    Active,
    Schedule,
    History,
    Earnings,
}

private data class WorkShortcut(
    val glyph: WorkGlyph,
    val title: Int,
    val detail: Int,
    val color: Color,
    val action: () -> Unit,
)

/** Real navigation actions only. No operational counts or decorative business data. */
@Composable
fun ProviderWorkHub(
    onRequests: () -> Unit,
    onActive: () -> Unit,
    onSchedule: () -> Unit,
    onEarnings: () -> Unit,
    onHistory: (() -> Unit)?,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = rememberProviderMotionAllowed(),
) {
    val view = LocalView.current
    var inViewport by remember { mutableStateOf(false) }
    val reveal = remember { Animatable(if (motionEnabled) 0f else 1f) }
    LaunchedEffect(inViewport, motionEnabled) {
        if (!motionEnabled) reveal.snapTo(1f)
        else if (inViewport) reveal.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    val actions =
        listOfNotNull(
            WorkShortcut(
                WorkGlyph.Active,
                R.string.provider_active_jobs,
                R.string.provider_active_jobs_detail,
                Color(0xFFEAE4FB),
                onActive,
            ),
            WorkShortcut(
                WorkGlyph.Schedule,
                R.string.provider_schedule,
                R.string.provider_schedule_detail,
                Color(0xFFE2F0EB),
                onSchedule,
            ),
            onHistory?.let {
                WorkShortcut(
                    WorkGlyph.History,
                    R.string.provider_request_history,
                    R.string.provider_request_history_detail,
                    Color(0xFFF1F3EE),
                    it,
                )
            },
            WorkShortcut(
                WorkGlyph.Earnings,
                R.string.provider_earnings,
                R.string.provider_earnings_detail,
                Color(0xFFF9EDE2),
                onEarnings,
            ),
        )
    Column(
        modifier.fillMaxWidth().testTag("provider_work_hub").onGloballyPositioned {
            val bounds = it.boundsInWindow()
            inViewport = bounds.bottom > 0 && bounds.top < view.rootView.height * .9f
        },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                stringResource(R.string.provider_work_hub_eyebrow),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
            )
            Text(
                stringResource(R.string.provider_manage_work),
                style = MaterialTheme.typography.titleLarge,
                letterSpacing = (-.5).sp,
            )
        }
        WorkCard(
            WorkShortcut(
                WorkGlyph.Requests,
                R.string.provider_job_requests,
                R.string.provider_job_requests_detail,
                FixoomTealDark,
                onRequests,
            ),
            featured = true,
            motionEnabled = motionEnabled,
            reveal = { reveal.value },
            index = 0,
            modifier = Modifier.fillMaxWidth(),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.3f
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                actions.chunked(if (stacked) 1 else 2).forEachIndexed { row, group ->
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        group.forEachIndexed { column, action ->
                            WorkCard(
                                action,
                                false,
                                motionEnabled,
                                { reveal.value },
                                1 + row * 2 + column,
                                Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkCard(
    shortcut: WorkShortcut,
    featured: Boolean,
    motionEnabled: Boolean,
    reveal: () -> Float,
    index: Int,
    modifier: Modifier,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val pressure by
        animateFloatAsState(
            if (pressed && motionEnabled) 1f else 0f,
            if (motionEnabled) spring(dampingRatio = .72f, stiffness = 650f) else snap(),
            label = "work_card_press",
        )
    val ink = if (featured) Color.White else FixoomTealDark
    Surface(
        onClick = shortcut.action,
        interactionSource = interactions,
        shape = RoundedCornerShape(if (featured) 28.dp else 24.dp),
        color = shortcut.color,
        contentColor = ink,
        border =
            BorderStroke(
                1.dp,
                if (featured) Color.White.copy(alpha = .1f) else FixoomTealDark.copy(alpha = .055f),
            ),
        modifier =
            modifier.testTag("work_card_${shortcut.glyph.name.lowercase()}").graphicsLayer {
                val progress =
                    if (motionEnabled) (reveal() * 1.48f - index * .1f).coerceIn(0f, 1f) else 1f
                alpha = .55f + .45f * progress
                translationY = (1f - progress) * 14.dp.toPx()
                rotationX = (1f - progress) * 5f - pressure * 1.6f
                scaleX = 1f - pressure * .025f
                scaleY = 1f - pressure * .025f
                cameraDistance = 18 * density
            },
    ) {
        if (featured) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 174.dp).padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.provider_work_hub_start),
                        color = FixoomLime,
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(shortcut.title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(shortcut.detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD6E7E4),
                    )
                    Row(
                        Modifier.padding(top = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            stringResource(R.string.provider_work_hub_open),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = FixoomLime,
                        )
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            null,
                            Modifier.size(18.dp),
                            tint = FixoomLime,
                        )
                    }
                }
                RequestSculpture(
                    Modifier.padding(start = 12.dp).size(92.dp).graphicsLayer {
                        translationY = -pressure * 5.dp.toPx()
                        rotationZ = -4f + pressure * 7f
                    }
                )
            }
        } else {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 158.dp).padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        Modifier.size(44.dp)
                            .graphicsLayer {
                                rotationZ = -5f + pressure * 8f
                                translationY = -pressure * 3.dp.toPx()
                                shadowElevation = 5.dp.toPx()
                                shape = RoundedCornerShape(15.dp)
                            }
                            .background(Color.White.copy(alpha = .9f), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        WorkLineIcon(shortcut.glyph, Modifier.size(24.dp), FixoomTealDark)
                    }
                    Box(
                        Modifier.size(27.dp).background(Color.White.copy(alpha = .5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            null,
                            Modifier.size(14.dp),
                            tint = FixoomTealDark,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        stringResource(shortcut.title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(shortcut.detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4F6665),
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkLineIcon(glyph: WorkGlyph, modifier: Modifier, color: Color) {
    Canvas(modifier.clearAndSetSemantics {}) {
        scale(size.width / 24f, size.height / 24f, Offset.Zero) {
            val stroke = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(points: List<Offset>) {
                drawPath(
                    Path().apply {
                        points.forEachIndexed { i, p ->
                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                    },
                    color,
                    style = stroke,
                )
            }
            when (glyph) {
                WorkGlyph.Requests -> {
                    drawRoundRect(
                        color,
                        Offset(4f, 5f),
                        Size(16f, 15f),
                        CornerRadius(3f),
                        style = stroke,
                    )
                    line(
                        listOf(
                            Offset(4f, 13f),
                            Offset(8f, 13f),
                            Offset(10f, 16f),
                            Offset(14f, 16f),
                            Offset(16f, 13f),
                            Offset(20f, 13f),
                        )
                    )
                }
                WorkGlyph.Active -> {
                    drawRoundRect(
                        color,
                        Offset(3f, 7f),
                        Size(18f, 14f),
                        CornerRadius(3f),
                        style = stroke,
                    )
                    drawRoundRect(
                        color,
                        Offset(8f, 3f),
                        Size(8f, 4f),
                        CornerRadius(1.5f),
                        style = stroke,
                    )
                    line(
                        listOf(
                            Offset(3f, 12f),
                            Offset(10f, 14f),
                            Offset(14f, 14f),
                            Offset(21f, 12f),
                        )
                    )
                    line(
                        listOf(
                            Offset(10f, 12f),
                            Offset(10f, 16f),
                            Offset(14f, 16f),
                            Offset(14f, 12f),
                        )
                    )
                }
                WorkGlyph.Schedule -> {
                    drawRoundRect(
                        color,
                        Offset(3f, 5f),
                        Size(18f, 16f),
                        CornerRadius(3f),
                        style = stroke,
                    )
                    line(listOf(Offset(7f, 3f), Offset(7f, 7f)))
                    line(listOf(Offset(17f, 3f), Offset(17f, 7f)))
                    line(listOf(Offset(3f, 10f), Offset(21f, 10f)))
                    listOf(Offset(8f, 14f), Offset(13f, 14f), Offset(8f, 18f)).forEach {
                        drawCircle(color, 1f, it)
                    }
                    line(listOf(Offset(15f, 17f), Offset(17f, 19f), Offset(20f, 15f)))
                }
                WorkGlyph.History -> {
                    drawArc(
                        color,
                        -100f,
                        295f,
                        false,
                        Offset(4f, 4f),
                        Size(16f, 16f),
                        style = stroke,
                    )
                    line(listOf(Offset(3f, 5f), Offset(3f, 10f), Offset(8f, 10f)))
                    line(listOf(Offset(12f, 7f), Offset(12f, 12f), Offset(16f, 14f)))
                }
                WorkGlyph.Earnings -> {
                    drawRoundRect(
                        color,
                        Offset(3f, 6f),
                        Size(18f, 15f),
                        CornerRadius(3f),
                        style = stroke,
                    )
                    line(listOf(Offset(4f, 7f), Offset(4f, 4f), Offset(17f, 3f), Offset(17f, 6f)))
                    drawRoundRect(
                        color,
                        Offset(14f, 11f),
                        Size(8f, 6f),
                        CornerRadius(2f),
                        style = stroke,
                    )
                    drawCircle(color, 1f, Offset(17f, 14f))
                }
            }
        }
    }
}

@Composable
private fun RequestSculpture(modifier: Modifier) {
    Canvas(modifier.clearAndSetSemantics {}) {
        scale(size.width / 100f, size.height / 100f, Offset.Zero) {
            fun face(color: Color, points: List<Offset>) {
                drawPath(
                    Path().apply {
                        points.forEachIndexed { i, p ->
                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                        close()
                    },
                    color,
                )
            }
            drawOval(Color.Black.copy(alpha = .2f), Offset(8f, 76f), Size(86f, 17f))
            face(
                Color(0xFF5D8C78),
                listOf(
                    Offset(6f, 51f),
                    Offset(50f, 33f),
                    Offset(94f, 51f),
                    Offset(50f, 71f),
                ),
            )
            face(
                Color(0xFF89AA72),
                listOf(
                    Offset(6f, 51f),
                    Offset(50f, 71f),
                    Offset(50f, 88f),
                    Offset(6f, 68f),
                ),
            )
            face(
                Color(0xFFB4D37D),
                listOf(
                    Offset(50f, 71f),
                    Offset(94f, 51f),
                    Offset(94f, 68f),
                    Offset(50f, 88f),
                ),
            )
            drawRoundRect(Color(0xFFAC9FD4), Offset(27f, 14f), Size(49f, 51f), CornerRadius(7f))
            drawRoundRect(Color(0xFFE4DDF9), Offset(21f, 9f), Size(49f, 51f), CornerRadius(7f))
            drawLine(Color(0xFF6B6290), Offset(32f, 23f), Offset(58f, 23f), 3f, StrokeCap.Round)
            drawLine(Color(0xFFB7AACA), Offset(32f, 33f), Offset(55f, 33f), 3f, StrokeCap.Round)
            drawLine(Color(0xFFB7AACA), Offset(32f, 43f), Offset(48f, 43f), 3f, StrokeCap.Round)
            face(
                FixoomLime,
                listOf(
                    Offset(6f, 51f),
                    Offset(29f, 61f),
                    Offset(38f, 71f),
                    Offset(59f, 79f),
                    Offset(72f, 67f),
                    Offset(94f, 58f),
                    Offset(94f, 73f),
                    Offset(50f, 94f),
                    Offset(6f, 74f),
                ),
            )
            drawCircle(Color(0xFF163F40), 13f, Offset(80f, 26f))
            drawLine(FixoomLime, Offset(74f, 26f), Offset(78f, 30f), 2.5f, StrokeCap.Round)
            drawLine(FixoomLime, Offset(78f, 30f), Offset(87f, 21f), 2.5f, StrokeCap.Round)
        }
    }
}

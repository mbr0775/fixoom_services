package com.fixoom.marketplace.ui

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fixoom.core.model.BookingNotification
import com.fixoom.core.model.serviceTime

/** A readable ticket face over two solid layers. Only decorative artwork floats at rest. */
@Composable
internal fun CustomerNotificationTile(
    alert: BookingNotification,
    index: Int,
    enabled: Boolean,
    motion: Boolean,
    depth: () -> Float,
    open: () -> Unit,
    floating: Boolean = motion,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressure =
        animateFloatAsState(
            if (motion && pressed) 1f else 0f,
            spring(dampingRatio = .8f, stiffness = Spring.StiffnessMedium),
            label = "update_ticket_press",
        )
    val entrance = remember(alert.id) { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(alert.id, motion) {
        if (motion)
            entrance.animateTo(1f, tween(360, index.coerceAtMost(3) * 40, FastOutSlowInEasing))
        else entrance.snapTo(1f)
    }
    val unread = alert.readAt == null
    val accent =
        when (alert.kind) {
            "requested",
            "completed",
            "direct_payment_received" -> InboxLime
            "accepted",
            "message" -> Color(0xFFD7C9FC)
            "cancelled",
            "rejected" -> Color(0xFFFFD6BB)
            else -> Color(0xFFBCE4E8)
        }
    val face = if (alert.kind == "message") Color(0xFF293B48) else InboxTeal
    Box(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp).graphicsLayer {
            val progress = if (motion) entrance.value else 1f
            alpha = .6f + progress * .4f
            translationY = (1 - progress) * 18.dp.toPx()
            rotationX = if (motion) (1 - progress) * 9f + depth() * -2.5f else 0f
            cameraDistance = 28 * density
        }
    ) {
        Box(
            Modifier.matchParentSize()
                .padding(horizontal = 8.dp)
                .graphicsLayer {
                    translationY = (9f - if (motion) pressure.value * 5f else 0f).dp.toPx()
                    rotationZ = -1.5f
                    shape = RoundedCornerShape(27.dp)
                    clip = true
                }
                .background(accent)
        )
        Box(
            Modifier.matchParentSize()
                .padding(horizontal = 3.dp)
                .graphicsLayer {
                    translationY = (4f - if (motion) pressure.value * 2f else 0f).dp.toPx()
                    rotationZ = .6f
                    shape = RoundedCornerShape(27.dp)
                    clip = true
                }
                .background(lerp(face, accent, .3f))
        )
        Card(
            onClick = open,
            enabled = enabled,
            interactionSource = source,
            shape = RoundedCornerShape(27.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = face,
                    contentColor = Color.White,
                    disabledContainerColor = face,
                    disabledContentColor = Color.White.copy(alpha = .6f),
                ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .16f)),
            modifier =
                Modifier.fillMaxWidth()
                    .testTag("notification-${alert.id}")
                    .semantics { stateDescription = if (unread) "Unread" else "Read" }
                    .graphicsLayer {
                        val press = if (motion) pressure.value else 0f
                        translationY = press * 4.dp.toPx()
                        scaleX = 1f - press * .018f
                        scaleY = scaleX
                        rotationY = press * -2.5f
                        cameraDistance = 28 * density
                    },
        ) {
            Box {
                Canvas(Modifier.matchParentSize().clearAndSetSemantics {}) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = .10f), Color.Transparent),
                            center = Offset(size.width, 0f),
                            radius = size.width * .8f,
                        ),
                        size.width * .8f,
                        Offset(size.width, 0f),
                    )
                }
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            ticketCategory(alert.kind),
                            color = accent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (unread) accent else Color.White.copy(alpha = .09f),
                        ) {
                            Text(
                                if (unread) "NEW" else "READ",
                                Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                color = if (unread) InboxTeal else Color(0xFFD4E2DE),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (maxWidth < 270.dp || LocalDensity.current.fontScale > 1.25f) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CustomerUpdateArtwork(
                                    alert.kind,
                                    accent,
                                    Modifier.size(88.dp).align(Alignment.End),
                                    motion,
                                    floating,
                                    depth,
                                    { pressure.value },
                                )
                                TicketWords(alert)
                            }
                        } else
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(Modifier.weight(1f)) { TicketWords(alert) }
                                CustomerUpdateArtwork(
                                    alert.kind,
                                    accent,
                                    Modifier.size(88.dp),
                                    motion,
                                    floating,
                                    depth,
                                    { pressure.value },
                                )
                            }
                    }
                    TicketPerforation(accent)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "View booking",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(shape = CircleShape, color = accent) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                null,
                                Modifier.padding(9.dp).size(18.dp),
                                tint = InboxTeal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketWords(alert: BookingNotification) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            alert.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(alert.body, color = Color(0xFFD9E7E3), style = MaterialTheme.typography.bodyMedium)
        Text(
            serviceTime(alert.createdAt),
            color = Color(0xFFB2CCC7),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun TicketPerforation(accent: Color) {
    Canvas(Modifier.fillMaxWidth().height(8.dp).clearAndSetSemantics {}) {
        drawLine(
            Color.White.copy(alpha = .22f),
            Offset(0f, size.height / 2),
            Offset(size.width, size.height / 2),
            1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx())),
        )
        drawCircle(accent, 7.dp.toPx(), Offset(-20.dp.toPx(), size.height / 2))
        drawCircle(accent, 7.dp.toPx(), Offset(size.width + 20.dp.toPx(), size.height / 2))
    }
}

private fun ticketCategory(kind: String): String =
    when (kind) {
        "requested" -> "SERVICE REQUEST"
        "accepted" -> "BOOKING ACCEPTED"
        "message" -> "CONVERSATION"
        "completed" -> "SERVICE COMPLETE"
        "in_progress" -> "WORK IN PROGRESS"
        "cancelled",
        "rejected" -> "BOOKING UPDATE"
        "direct_payment_received" -> "PAYMENT RECEIPT"
        else -> "SERVICE UPDATE"
    }

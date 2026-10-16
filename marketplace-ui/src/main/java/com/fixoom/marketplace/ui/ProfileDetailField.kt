package com.fixoom.marketplace.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Read-only saved values reveal once per section opening, respecting motion preferences.
 */
@Composable
internal fun ProfileDetailField(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    motionEnabled: Boolean,
    order: Int = 0,
) {
    var revealed by remember { mutableStateOf(!motionEnabled) }
    LaunchedEffect(Unit) { revealed = true }
    val reveal =
        animateFloatAsState(
            if (revealed || !motionEnabled) 1f else 0f,
            animationSpec =
                if (motionEnabled)
                    tween(300, delayMillis = order * 45, easing = FastOutSlowInEasing)
                else snap(),
            label = "profile_field_reveal",
        )
    Box(
        Modifier.fillMaxWidth().padding(bottom = 4.dp).graphicsLayer {
            val progress = if (motionEnabled) reveal.value else 1f
            translationY = (1f - progress) * 9.dp.toPx()
            rotationX = (1f - progress) * -6f
            cameraDistance = 24 * density
            alpha = .65f + progress * .35f
        }
    ) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer {
                    translationY = 4.dp.toPx()
                    rotationZ = .7f
                }
                .clip(RoundedCornerShape(17.dp))
                .background(accent)
        )
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(17.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE5EDEB)),
            shadowElevation = 2.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.linearGradient(listOf(accent, accent.copy(alpha = .45f)))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, Modifier.size(20.dp), tint = Color(0xFF194850))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = .2.sp,
                        color = Color(0xFF647675),
                    )
                    SelectionContainer {
                        Text(
                            value,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF10363C),
                        )
                    }
                }
            }
        }
    }
}

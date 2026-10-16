package com.fixoom.marketplace.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WorkspaceDepthHeader(
    title: String,
    caption: String,
    icon: ImageVector,
    scrollOffset: () -> Float = { 0f },
) {
    val motion = rememberMarketplaceMotionAllowed()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val progress =
        animateFloatAsState(
            if (entered || !motion) 1f else 0f,
            if (motion) tween(350) else snap(),
            label = "workspace_depth_header",
        )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF10363C),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
    ) {
        Box {
            Canvas(
                Modifier.matchParentSize()
                    .clearAndSetSemantics {}
                    .graphicsLayer {
                        translationY =
                            if (motion) (scrollOffset() * .2f).coerceAtMost(20.dp.toPx()) else 0f
                    }
            ) {
                drawCircle(
                    Color.White.copy(alpha = .05f),
                    size.width * .5f,
                    Offset(size.width, size.height * .1f),
                )
                drawCircle(
                    Color(0xFFC8F43D).copy(alpha = .06f),
                    size.width * .3f,
                    Offset(0f, size.height),
                )
            }
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        title,
                        Modifier.weight(1f),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        Modifier.size(48.dp).graphicsLayer {
                            rotationY =
                                if (motion)
                                    (1 - progress.value) * -14f +
                                        (scrollOffset() / density * .035f).coerceAtMost(6f)
                                else 0f
                            translationY = if (motion) (1 - progress.value) * 10.dp.toPx() else 0f
                            rotationZ = -8f
                            cameraDistance = 24 * density
                        },
                        shape = RoundedCornerShape(15.dp),
                        color = Color(0xFFC8F43D),
                        shadowElevation = 5.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = Color(0xFF10363C))
                        }
                    }
                }
                Text(
                    caption,
                    color = Color(0xFFD0E3DF),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

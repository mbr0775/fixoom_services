package com.fixoom.marketplace.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The displayed region and the encoded region use this same clamped crop. */
fun providerPhotoCropRect(
    width: Int,
    height: Int,
    zoom: Float,
    centerX: Float,
    centerY: Float,
): Rect {
    val side = (min(width, height) / zoom.coerceIn(1f, 4f)).roundToInt().coerceAtLeast(1)
    val left = (centerX * width - side / 2f).roundToInt().coerceIn(0, width - side)
    val top = (centerY * height - side / 2f).roundToInt().coerceIn(0, height - side)
    return Rect(left, top, left + side, top + side)
}

@Composable
fun ProviderPhotoCropper(source: Bitmap, onDismiss: () -> Unit, onUse: (ByteArray) -> Unit) {
    var image by remember(source) { mutableStateOf(source) }
    var zoom by remember(image) { mutableFloatStateOf(1f) }
    var centerX by remember(image) { mutableFloatStateOf(.5f) }
    var centerY by remember(image) { mutableFloatStateOf(.5f) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bitmap = remember(image) { image.asImageBitmap() }
    val crop = providerPhotoCropRect(image.width, image.height, zoom, centerX, centerY)
    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties =
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onDismiss, enabled = !saving) { Text("Cancel") }
                    Text(
                        "Edit profile photo",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    TextButton(
                        {
                            saving = true
                            error = false
                            scope.launch {
                                try {
                                    val bytes =
                                        withContext(Dispatchers.IO) {
                                            val region =
                                                Bitmap.createBitmap(
                                                    image,
                                                    crop.left,
                                                    crop.top,
                                                    crop.width(),
                                                    crop.height(),
                                                )
                                            try {
                                                encodeProviderPhoto(region)
                                            } finally {
                                                if (region !== image) region.recycle()
                                            }
                                        }
                                    onUse(bytes)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    error = true
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = !saving,
                    ) {
                        Text(if (saving) "Preparing…" else "Use photo")
                    }
                }
                Column(
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Text(
                        "Drag to position. Pinch or use the slider to zoom.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Canvas(
                        Modifier.widthIn(max = 440.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .semantics { contentDescription = "Profile photo crop preview" }
                            .pointerInput(image) {
                                detectTransformGestures { _, pan, scale, _ ->
                                    if (!saving) {
                                        zoom = (zoom * scale).coerceIn(1f, 4f)
                                        val side = min(image.width, image.height) / zoom
                                        centerX =
                                            (centerX - pan.x / size.width * side / image.width)
                                                .coerceIn(
                                                    side / (2 * image.width),
                                                    1 - side / (2 * image.width),
                                                )
                                        centerY =
                                            (centerY - pan.y / size.height * side / image.height)
                                                .coerceIn(
                                                    side / (2 * image.height),
                                                    1 - side / (2 * image.height),
                                                )
                                    }
                                }
                            }
                    ) {
                        drawImage(
                            bitmap,
                            IntOffset(crop.left, crop.top),
                            IntSize(crop.width(), crop.height()),
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        )
                        val mask =
                            Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(ComposeRect(Offset.Zero, size))
                                addOval(ComposeRect(Offset.Zero, size))
                            }
                        drawPath(mask, Color.Black.copy(alpha = .52f))
                        drawCircle(
                            Color.White,
                            radius = size.minDimension / 2 - 2.dp.toPx(),
                            style = Stroke(2.dp.toPx()),
                        )
                    }
                    Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                        Text("Zoom", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            zoom,
                            { zoom = it },
                            valueRange = 1f..4f,
                            enabled = !saving,
                            modifier = Modifier.semantics { contentDescription = "Photo zoom" },
                        )
                    }
                    OutlinedButton(
                        {
                            image =
                                Bitmap.createBitmap(
                                    image,
                                    0,
                                    0,
                                    image.width,
                                    image.height,
                                    Matrix().apply { postRotate(90f) },
                                    true,
                                )
                        },
                        enabled = !saving,
                    ) {
                        Icon(Icons.Rounded.RotateRight, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Rotate photo")
                    }
                    Text(
                        "Only the area inside the circle is shown on your profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (error)
                        Text(
                            "This photo could not be prepared. Try again or choose another image.",
                            color = MaterialTheme.colorScheme.error,
                        )
                }
            }
        }
    }
}

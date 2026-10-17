package com.fixoom.marketplace.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.fixoom.core.data.repository.ProviderPhotoRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private val photoCache =
    object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

@Composable
fun ProviderPhoto(
    path: String?,
    name: String,
    modifier: Modifier = Modifier,
    preview: ByteArray? = null,
    customer: Boolean = false,
) {
    var retry by remember(path) { mutableIntStateOf(0) }
    var failed by remember(path, preview, retry) { mutableStateOf(false) }
    var loading by remember(path, preview, retry) { mutableStateOf(false) }
    val image by
        produceState<ImageBitmap?>(null, path, preview, retry, customer) {
            value = null
            failed = false
            if (preview != null || path != null) {
                loading = true
                try {
                    value =
                        withContext(Dispatchers.IO) {
                            if (preview == null && path != null)
                                photoCache.get("$customer/$path")?.let {
                                    return@withContext it
                                }
                            val bytes =
                                preview
                                    ?: withTimeout(15_000) {
                                        ProviderPhotoRepository(customer = customer).download(path!!)
                                    }
                            require(bytes.size <= ProviderPhotoRepository.MAX_BYTES)
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048)
                            val bitmap =
                                requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                                    .asImageBitmap()
                            if (preview == null && path != null) photoCache.put("$customer/$path", bitmap)
                            bitmap
                        }
                } catch (cancelled: CancellationException) {
                    if (cancelled !is kotlinx.coroutines.TimeoutCancellationException)
                        throw cancelled
                    failed = true
                } catch (_: Exception) {
                    failed = true
                } finally {
                    loading = false
                }
            }
        }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = image
            if (bitmap != null)
                Image(
                    bitmap,
                    "$name profile photo",
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            else if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else
                Text(
                    name.trim().take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.titleLarge,
                )
        }
        if (failed)
            TextButton({ retry++ }, contentPadding = PaddingValues(0.dp)) { Text("Retry photo") }
    }
}

/** Bounded decode, orientation correction and fresh JPEG encoding remove location/EXIF metadata. */
fun decodeProviderPhoto(context: Context, uri: Uri): Bitmap {
    val bytes =
        context.contentResolver.openInputStream(uri)?.use { source ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            while (true) {
                val count = source.read(chunk)
                if (count < 0) break
                require(output.size() + count <= 15 * 1024 * 1024)
                output.write(chunk, 0, count)
            }
            output.toByteArray()
        } ?: error("Photo unavailable")
    return decodeProviderPhoto(bytes)
}

fun decodeProviderPhoto(bytes: ByteArray): Bitmap {
    require(bytes.size <= 15 * 1024 * 1024)
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    require(options.outWidth > 0 && options.outHeight > 0)
    var sample = 1
    while (max(options.outWidth, options.outHeight) / sample > 1024) sample *= 2
    options.inJustDecodeBounds = false
    options.inSampleSize = sample
    val original = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
        .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix =
        Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(270f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
    val oriented =
        Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
    if (oriented !== original) original.recycle()
    return oriented
}

fun prepareProviderPhoto(context: Context, uri: Uri): ByteArray {
    val image = decodeProviderPhoto(context, uri)
    return try {
        encodeProviderPhoto(image)
    } finally {
        image.recycle()
    }
}

fun prepareProviderPhoto(bytes: ByteArray): ByteArray {
    val image = decodeProviderPhoto(bytes)
    return try {
        encodeProviderPhoto(image)
    } finally {
        image.recycle()
    }
}

/** Encode a previewed crop without recycling the bitmap still displayed by Compose. */
fun encodeProviderPhoto(oriented: Bitmap): ByteArray {
    val side = min(oriented.width, oriented.height)
    val cropped =
        Bitmap.createBitmap(
            oriented,
            (oriented.width - side) / 2,
            (oriented.height - side) / 2,
            side,
            side,
        )
    val resized = Bitmap.createScaledBitmap(cropped, min(side, 512), min(side, 512), true)
    val opaque = Bitmap.createBitmap(resized.width, resized.height, Bitmap.Config.ARGB_8888)
    Canvas(opaque).apply {
        drawColor(android.graphics.Color.WHITE)
        drawBitmap(resized, 0f, 0f, null)
    }
    val output = ByteArrayOutputStream()
    check(opaque.compress(Bitmap.CompressFormat.JPEG, 90, output))
    listOf(cropped, resized, opaque).distinct().filter { it !== oriented }.forEach { it.recycle() }
    return output.toByteArray().also { require(ProviderPhotoRepository.isValidUpload(it)) }
}

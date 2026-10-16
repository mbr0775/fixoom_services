package com.fixoom.provider.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import com.fixoom.marketplace.ui.ProviderPhotoCropper
import com.fixoom.marketplace.ui.ProviderPhotoEditor
import com.fixoom.marketplace.ui.prepareProviderPhoto
import com.fixoom.marketplace.ui.providerPhotoCropRect
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProviderPhotoTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun photoPreparationProducesABoundedJpegWithoutLocationMetadata() {
        val source = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888)
        source.eraseColor(android.graphics.Color.BLUE)
        val encoded =
            ByteArrayOutputStream()
                .apply { source.compress(Bitmap.CompressFormat.PNG, 100, this) }
                .toByteArray()
        source.recycle()
        val jpeg = prepareProviderPhoto(encoded)
        assertTrue(jpeg.size <= 1_048_576)
        val result = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        assertEquals(512, result.width)
        assertEquals(512, result.height)
        assertFalse(result.hasAlpha())
        val exif = ExifInterface(ByteArrayInputStream(jpeg))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
        result.recycle()
        try {
            prepareProviderPhoto(byteArrayOf(1, 2, 3))
            fail("Invalid image accepted")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun emptyPhotoEditorShowsPickerWithoutInventingAPortrait() {
        compose.setContent {
            Fixoom_ServicesTheme {
                ProviderPhotoEditor(null, "Photo test", false) { _, _ ->
                    fail("No photo should be saved without an explicit selection")
                }
            }
        }
        compose.onNodeWithText("Add photo").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Save photo").assertDoesNotExist()
        compose.onNodeWithText("Remove photo").assertDoesNotExist()
    }

    @Test
    fun avatarOpensGalleryAndCameraActionsAndCancelDoesNotSave() {
        var saves = 0
        compose.setContent {
            Fixoom_ServicesTheme {
                ProviderPhotoEditor(null, "Photo test", false) { _, _ -> saves++ }
            }
        }
        compose.onNodeWithContentDescription("Edit profile photo").performClick()
        compose.onNodeWithText("Choose from gallery").assertIsDisplayed()
        compose.onNodeWithText("Take photo").assertIsDisplayed()
        compose.onNodeWithText("View photo").assertDoesNotExist()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Choose from gallery").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, saves) }
    }

    @Test
    fun cropClampsToTheSelectedImageAtAllZoomLevels() {
        val center = providerPhotoCropRect(1200, 600, 1f, .5f, .5f)
        assertEquals(android.graphics.Rect(300, 0, 900, 600), center)
        val edge = providerPhotoCropRect(1200, 600, 2f, 0f, 1f)
        assertEquals(android.graphics.Rect(0, 300, 300, 600), edge)
        val extreme = providerPhotoCropRect(1200, 600, 100f, -5f, 5f)
        assertEquals(150, extreme.width())
        assertEquals(0, extreme.left)
        assertEquals(600, extreme.bottom)
    }

    @Test
    fun cropPreviewSupportsZoomRotateAndProducesARealJpeg() {
        val source = Bitmap.createBitmap(1000, 600, Bitmap.Config.ARGB_8888)
        source.eraseColor(android.graphics.Color.rgb(25, 72, 80))
        val open = mutableStateOf(true)
        var selected: ByteArray? = null
        compose.setContent {
            Fixoom_ServicesTheme {
                if (open.value)
                    ProviderPhotoCropper(source, { open.value = false }) { bytes ->
                        selected = bytes
                        open.value = false
                    }
            }
        }
        compose.onNodeWithContentDescription("Profile photo crop preview").assertIsDisplayed()
        compose.onNodeWithContentDescription("Photo zoom").performScrollTo().performSemanticsAction(
            SemanticsActions.SetProgress
        ) {
            it(2f)
        }
        compose.onNodeWithText("Rotate photo").performScrollTo().performClick()
        compose.waitForIdle()
        val automation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand(
                    "screencap -p /data/local/tmp/fixoom-profile-crop-fixture.png"
                )
            )
            .use { it.readBytes() }
        compose.onNodeWithText("Use photo").performClick()
        compose.waitUntil(10_000) { selected != null }
        compose.runOnIdle {
            val bytes = requireNotNull(selected)
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertEquals(512, decoded.width)
            assertEquals(512, decoded.height)
            decoded.recycle()
            source.recycle()
        }
    }

    @Test
    fun gallerySelectionRequiresCropAndCanBeAdjustedAgainBeforeUpload() {
        var saved: ByteArray? = null
        var saves = 0
        val fixture = showEditorWithPhotoResult { bytes, done ->
            saved = bytes
            saves++
            done()
        }
        try {
            compose.onNodeWithText("Add photo").performClick()
            compose.onNodeWithText("Choose from gallery").performClick()
            compose.waitUntil(10_000) {
                compose
                    .onAllNodesWithContentDescription("Photo zoom")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            compose.onNodeWithContentDescription("Profile photo crop preview").assertIsDisplayed()
            compose.onNodeWithText("Save photo").assertDoesNotExist()
            compose.runOnIdle { assertEquals(0, saves) }
            compose.onNodeWithText("Use photo").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Adjust crop").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Adjust crop").performScrollTo()
            compose.waitForIdle()
            val automation =
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                    automation.executeShellCommand(
                        "screencap -p /data/local/tmp/fixoom-adjust-crop-preview.png"
                    )
                )
                .use { it.readBytes() }
            compose.onNodeWithText("Adjust crop").performClick()
            compose
                .onNodeWithContentDescription("Photo zoom")
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
            compose
                .onNodeWithContentDescription("Profile photo crop preview")
                .performScrollTo()
                .performTouchInput {
                    swipe(
                        Offset(width * .85f, center.y),
                        Offset(width * .15f, center.y),
                        400,
                    )
                }
            compose.onNodeWithText("Use photo").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Save photo").fetchSemanticsNodes().isNotEmpty()
            }
            compose.runOnIdle { assertEquals(0, saves) }
            compose.onNodeWithText("Save photo").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(1, saves)
                val bytes = requireNotNull(saved)
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val pixel = image.getPixel(image.width / 2, image.height / 2)
                assertTrue(
                    "Saved image must use the adjusted right-hand crop",
                    android.graphics.Color.green(pixel) > 200,
                )
                assertTrue(android.graphics.Color.red(pixel) < 50)
                image.recycle()
            }
            compose.onNodeWithText("Adjust crop").assertDoesNotExist()
        } finally {
            fixture.delete()
        }
    }

    @Test
    fun cameraSelectionRequiresCropAndCancellingDoesNotUpload() {
        var saves = 0
        val fixture = showEditorWithPhotoResult { _, _ -> saves++ }
        try {
            compose.onNodeWithText("Add photo").performClick()
            compose.onNodeWithText("Take photo").performClick()
            compose.waitUntil(10_000) {
                compose
                    .onAllNodesWithContentDescription("Photo zoom")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            compose.onNodeWithContentDescription("Profile photo crop preview").assertIsDisplayed()
            compose.onNodeWithText("Save photo").assertDoesNotExist()
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithText("Save photo").assertDoesNotExist()
            compose.onNodeWithText("Adjust crop").assertDoesNotExist()
            compose.runOnIdle { assertEquals(0, saves) }
        } finally {
            fixture.delete()
        }
    }

    // Activity results and save callbacks are isolated fixtures; no real gallery, camera or backend
    // writes.
    private fun showEditorWithPhotoResult(onSave: (ByteArray?, () -> Unit) -> Unit): java.io.File {
        val context =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val folder = java.io.File(context.cacheDir, "profile-camera").apply { mkdirs() }
        val file = java.io.File(folder, "fixture-${java.util.UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bitmap).apply {
            drawColor(android.graphics.Color.RED)
            drawRect(
                600f,
                0f,
                1200f,
                600f,
                android.graphics.Paint().apply { color = android.graphics.Color.GREEN },
            )
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry =
                    object : ActivityResultRegistry() {
                        override fun <I, O> onLaunch(
                            requestCode: Int,
                            contract: ActivityResultContract<I, O>,
                            input: I,
                            options: ActivityOptionsCompat?,
                        ) {
                            if (contract is ActivityResultContracts.TakePicture) {
                                context.contentResolver
                                    .openOutputStream(input as android.net.Uri)!!
                                    .use { output ->
                                        file.inputStream().use { it.copyTo(output) }
                                    }
                                dispatchResult(requestCode, true)
                            } else {
                                dispatchResult(
                                    requestCode,
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.profilephotos",
                                        file,
                                    ),
                                )
                            }
                        }
                    }
            }
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                Fixoom_ServicesTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        ProviderPhotoEditor(null, "Photo test", false, onSave)
                    }
                }
            }
        }
        return file
    }

    @Test
    fun cameraFileProviderExposesOnlyItsDedicatedCacheFolder() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val folder = java.io.File(context.cacheDir, "profile-camera").apply { mkdirs() }
        val file = java.io.File(folder, "${java.util.UUID.randomUUID()}.jpg")
        try {
            val uri =
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.profilephotos",
                    file,
                )
            context.contentResolver.openOutputStream(uri)!!.use { it.write(byteArrayOf(1, 2, 3)) }
            assertTrue(file.exists())
            try {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.profilephotos",
                    java.io.File(context.cacheDir, "unshared-private-file"),
                )
                fail("Unrelated private files must not be shareable")
            } catch (_: IllegalArgumentException) {}
        } finally {
            file.delete()
        }
    }
}

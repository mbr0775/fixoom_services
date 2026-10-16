package com.fixoom.provider.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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

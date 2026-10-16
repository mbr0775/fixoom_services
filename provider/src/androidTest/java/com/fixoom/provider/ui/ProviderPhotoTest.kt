package com.fixoom.provider.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.fixoom.marketplace.ui.ProviderPhotoEditor
import com.fixoom.marketplace.ui.prepareProviderPhoto
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
}

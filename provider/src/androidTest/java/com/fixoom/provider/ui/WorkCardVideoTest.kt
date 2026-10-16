package com.fixoom.provider.ui

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.fixoom.provider.ui.components.ProviderWorkHub
import com.fixoom.provider.ui.components.WorkCardVideoView
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Actual packaged video/decoder checks. No account, booking, or backend writes. */
class WorkCardVideoTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun videoDecodesNavigatesAndReleasesOffscreenAndWhenMotionStops() {
        val motion = mutableStateOf(true)
        var opened = false
        compose.setContent {
            Fixoom_ServicesTheme {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                ) {
                    ProviderWorkHub({}, { opened = true }, {}, {}, {}, motionEnabled = motion.value)
                    Spacer(Modifier.height(900.dp))
                    Text("End", Modifier.testTag("outside_work_hub"))
                }
            }
        }
        compose.onNodeWithTag("work_card_active").performScrollTo().assertIsDisplayed()
        var surface: WorkCardVideoView? = null
        compose.waitUntil(10_000) {
            compose.runOnIdle { surface = videoIn(compose.activity.window.decorView) }
            surface?.alpha == 1f
        }
        lateinit var before: Bitmap
        compose.runOnIdle { before = requireNotNull(surface?.bitmap) }
        SystemClock.sleep(1000) // Native video follows wall time, independently of Compose's clock.
        compose.runOnIdle {
            val after = requireNotNull(surface?.bitmap)
            assertFalse(
                "The bundled video must produce changing decoded frames",
                before.sameAs(after),
            )
            before.recycle()
            after.recycle()
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "active-jobs-video-card.png")
            .outputStream()
            .use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
        compose.onNodeWithTag("work_card_active").performClick()
        compose.runOnIdle { assertTrue("Video must not intercept the card's action", opened) }

        compose.onNodeWithTag("outside_work_hub").performScrollTo()
        compose.runOnIdle {
            assertNull(
                "Offscreen video must release its native view/decoder",
                videoIn(compose.activity.window.decorView),
            )
        }
        compose.onNodeWithTag("work_card_active").performScrollTo()
        compose.waitUntil(10_000) {
            compose.runOnIdle { surface = videoIn(compose.activity.window.decorView) }
            surface?.alpha == 1f
        }
        compose.runOnIdle { motion.value = false }
        compose.waitForIdle()
        compose.runOnIdle {
            assertNull(
                "Reduced motion must remove the decoder",
                videoIn(compose.activity.window.decorView),
            )
        }
        compose.onNodeWithTag("active_jobs_poster", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Active jobs").assertIsDisplayed()
    }

    private fun videoIn(view: View): WorkCardVideoView? {
        if (view is WorkCardVideoView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) videoIn(view.getChildAt(i))?.let {
                return it
            }
        }
        return null
    }
}

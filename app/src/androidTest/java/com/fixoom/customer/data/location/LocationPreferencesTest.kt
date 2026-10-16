package com.fixoom.customer.data.location

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

// Android DataStore's FileStorage uses POSIX replacement semantics, so exercise
// real persistence on Android rather than File.renameTo on a Windows host.
@RunWith(AndroidJUnit4::class)
class LocationPreferencesTest {
    @get:Rule
    val temporaryFolder =
        TemporaryFolder(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)
    private var storeJob = SupervisorJob()
    private lateinit var preferences: LocationPreferences
    private lateinit var file: File

    @Before
    fun setup() {
        file = File(temporaryFolder.root, "location.preferences_pb")
        preferences = newPreferences()
    }

    private fun newPreferences() =
        LocationPreferences(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + storeJob),
                produceFile = { file },
            )
        )

    @After fun closeStore() = runBlocking { storeJob.cancelAndJoin() }

    @Test
    fun freshInstallDoesNotInventALocationOrEnableGps() = runBlocking {
        val saved = preferences.savedLocation.first()
        assertNull(saved.selection)
        assertFalse(saved.automatic)
    }

    @Test
    fun manualChoiceIsNormalizedAndDisablesAutomaticMode() = runBlocking {
        preferences.enableAutomatic()
        preferences.selectManual("  Kandy, \n  Sri Lanka  ")
        val saved = preferences.savedLocation.first()
        assertEquals(ServiceLocation("Kandy, Sri Lanka"), saved.selection)
        assertFalse(saved.automatic)
    }

    @Test
    fun manualChoiceSurvivesStoreRecreation() = runBlocking {
        preferences.selectManual("Galle, Sri Lanka")
        storeJob.cancelAndJoin()
        storeJob = SupervisorJob()
        preferences = newPreferences()
        assertEquals("Galle, Sri Lanka", preferences.savedLocation.first().selection?.label)
    }

    @Test
    fun gpsModeAndAreaSurviveStoreRecreation() = runBlocking {
        preferences.enableAutomatic()
        preferences.saveDetected("Negombo, Sri Lanka")
        storeJob.cancelAndJoin()
        storeJob = SupervisorJob()
        preferences = newPreferences()
        val saved = preferences.savedLocation.first()
        assertTrue(saved.automatic)
        assertEquals(ServiceLocation("Negombo, Sri Lanka", fromGps = true), saved.selection)
    }

    @Test
    fun gpsResultRequiresExplicitOptIn() = runBlocking {
        preferences.saveDetected("Colombo, Sri Lanka")
        assertNull(preferences.savedLocation.first().selection)
    }

    @Test
    fun lateGpsResultCannotOverwriteANewerManualChoice() = runBlocking {
        preferences.enableAutomatic()
        preferences.saveDetected("Colombo, Sri Lanka")
        preferences.selectManual("Matara, Sri Lanka")
        preferences.saveDetected("Colombo, Sri Lanka")
        val saved = preferences.savedLocation.first()
        assertEquals(ServiceLocation("Matara, Sri Lanka"), saved.selection)
        assertFalse(saved.automatic)
    }

    @Test
    fun enablingGpsKeepsSavedAreaUntilDetectionSucceeds() = runBlocking {
        preferences.selectManual("Jaffna, Sri Lanka")
        preferences.enableAutomatic()
        val saved = preferences.savedLocation.first()
        assertTrue(saved.automatic)
        assertEquals(ServiceLocation("Jaffna, Sri Lanka"), saved.selection)
    }

    @Test
    fun invalidInputDoesNotOverwriteSavedArea() = runBlocking {
        preferences.selectManual("Kandy, Sri Lanka")
        val result = runCatching { preferences.selectManual("   ") }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Kandy, Sri Lanka", preferences.savedLocation.first().selection?.label)
    }

    @Test
    fun coordinatesRequireOptInPersistLocallyAndAreClearedByManualChoice() = runBlocking {
        preferences.saveDetected("Test area", 6.9271, 79.8612)
        assertNull(preferences.savedLocation.first().selection)
        preferences.enableAutomatic()
        preferences.saveDetected("Test area", 6.9271, 79.8612)
        storeJob.cancelAndJoin()
        storeJob = SupervisorJob()
        preferences = newPreferences()
        assertEquals(6.9271, preferences.savedLocation.first().selection!!.latitude!!, 0.000001)
        preferences.selectManual("Manual area")
        assertNull(preferences.savedLocation.first().selection!!.latitude)
        assertNull(preferences.savedLocation.first().selection!!.longitude)
    }

    @Test
    fun validatesCustomNamesIncludingNonLatinText() {
        assertFalse(isValidLocationLabel(""))
        assertFalse(isValidLocationLabel("A"))
        assertFalse(isValidLocationLabel("12345"))
        assertFalse(isValidLocationLabel("a".repeat(81)))
        assertTrue(isValidLocationLabel("කොළඹ"))
        assertTrue(isValidLocationLabel("Jaffna, Sri Lanka"))
    }
}

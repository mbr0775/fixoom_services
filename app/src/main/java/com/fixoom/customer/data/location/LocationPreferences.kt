package com.fixoom.customer.data.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.locationDataStore by preferencesDataStore(name = "service_location")

data class ServiceLocation(
    val label: String,
    val fromGps: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class SavedLocation(
    val selection: ServiceLocation? = null,
    val automatic: Boolean = false,
)

fun normalizeLocationLabel(value: String): String = value.trim().replace(Regex("\\s+"), " ")

fun isValidLocationLabel(value: String): Boolean {
    val normalized = normalizeLocationLabel(value)
    return normalized.length in 2..80 && normalized.any(Char::isLetter)
}

/**
 * Stores the chosen area and optional GPS discovery point locally on this device. A manual
 * selection always disables automatic updates.
 */
class LocationPreferences internal constructor(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.locationDataStore)

    val savedLocation =
        store.data.map { preferences ->
            SavedLocation(
                selection =
                    preferences[LABEL]?.takeIf(String::isNotBlank)?.let {
                        ServiceLocation(
                            it,
                            preferences[FROM_GPS] ?: false,
                            preferences[LATITUDE],
                            preferences[LONGITUDE],
                        )
                    },
                automatic = preferences[AUTOMATIC] ?: false,
            )
        }

    suspend fun selectManual(label: String) {
        require(isValidLocationLabel(label))
        store.edit {
            it[LABEL] = normalizeLocationLabel(label)
            it[FROM_GPS] = false
            it[AUTOMATIC] = false
            it.remove(LATITUDE)
            it.remove(LONGITUDE)
        }
    }

    suspend fun enableAutomatic() {
        store.edit { it[AUTOMATIC] = true }
    }

    suspend fun saveDetected(label: String, latitude: Double? = null, longitude: Double? = null) {
        require(label.isNotBlank())
        store.edit {
            // Protect a manual choice from a GPS result that arrived late.
            if (it[AUTOMATIC] == true) {
                it[LABEL] = label
                it[FROM_GPS] = true
                if (latitude != null && longitude != null) {
                    it[LATITUDE] = latitude
                    it[LONGITUDE] = longitude
                }
            }
        }
    }

    private companion object {
        val LATITUDE = doublePreferencesKey("discovery_latitude")
        val LONGITUDE = doublePreferencesKey("discovery_longitude")
        val LABEL = stringPreferencesKey("area_label")
        val FROM_GPS = booleanPreferencesKey("from_gps")
        val AUTOMATIC = booleanPreferencesKey("automatic")
    }
}

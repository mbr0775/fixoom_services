package com.fixoom.customer.data.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.locationDataStore by preferencesDataStore(
    name = "service_location"
)

data class ServiceLocation(val label: String, val fromGps: Boolean = false)

data class SavedLocation(
    val selection: ServiceLocation? = null,
    val automatic: Boolean = false
)

fun normalizeLocationLabel(value: String): String =
    value.trim().replace(Regex("\\s+"), " ")

fun isValidLocationLabel(value: String): Boolean {
    val normalized = normalizeLocationLabel(value)
    return normalized.length in 2..80 && normalized.any(Char::isLetter)
}

/**
 * Stores an area label and the user's choice of mode, not precise coordinates.
 * A manual selection always disables automatic updates.
 */
class LocationPreferences internal constructor(
    private val store: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.locationDataStore)

    val savedLocation = store.data.map { preferences ->
        SavedLocation(
            selection = preferences[LABEL]?.takeIf(String::isNotBlank)?.let {
                ServiceLocation(it, preferences[FROM_GPS] ?: false)
            },
            automatic = preferences[AUTOMATIC] ?: false
        )
    }

    suspend fun selectManual(label: String) {
        require(isValidLocationLabel(label))
        store.edit {
            it[LABEL] = normalizeLocationLabel(label)
            it[FROM_GPS] = false
            it[AUTOMATIC] = false
        }
    }

    suspend fun enableAutomatic() {
        store.edit { it[AUTOMATIC] = true }
    }

    suspend fun saveDetected(label: String) {
        require(label.isNotBlank())
        store.edit {
            // Protect a manual choice from a GPS result that arrived late.
            if (it[AUTOMATIC] == true) {
                it[LABEL] = label
                it[FROM_GPS] = true
            }
        }
    }

    private companion object {
        val LABEL = stringPreferencesKey("area_label")
        val FROM_GPS = booleanPreferencesKey("from_gps")
        val AUTOMATIC = booleanPreferencesKey("automatic")
    }
}

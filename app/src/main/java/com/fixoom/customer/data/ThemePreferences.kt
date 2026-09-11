package com.fixoom.customer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(
    context: Context
) {
    private val appContext = context.applicationContext

    val isDarkMode: Flow<Boolean> =
        appContext.themeDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[DARK_MODE_KEY] ?: false
            }

    suspend fun setDarkMode(enabled: Boolean) {
        appContext.themeDataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    private companion object {
        val DARK_MODE_KEY =
            booleanPreferencesKey("dark_mode_enabled")
    }
}
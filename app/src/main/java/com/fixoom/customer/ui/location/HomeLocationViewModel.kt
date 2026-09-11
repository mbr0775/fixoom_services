package com.fixoom.customer.ui.location

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fixoom.customer.R
import com.fixoom.customer.data.location.DeviceLocationResolver
import com.fixoom.customer.data.location.LocationPreferences
import com.fixoom.customer.data.location.SavedLocation
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

enum class LocationRecovery { NONE, APP_SETTINGS, LOCATION_SETTINGS }

data class HomeLocationUiState(
    val saved: SavedLocation = SavedLocation(),
    val ready: Boolean = false,
    val locating: Boolean = false,
    val saving: Boolean = false,
    val message: Int? = null,
    val recovery: LocationRecovery = LocationRecovery.NONE
)

class HomeLocationViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = LocationPreferences(application)
    private val resolver = DeviceLocationResolver(application)
    private val mutableState = MutableStateFlow(HomeLocationUiState())
    val state = mutableState.asStateFlow()
    private var foreground = false
    private var locationJob: Job? = null
    private var fixGeneration = 0

    init {
        viewModelScope.launch {
            preferences.savedLocation
                .retryWhen { error, _ ->
                    if (error is IOException) {
                        report(R.string.location_load_failed)
                        delay(2_000)
                        true
                    } else {
                        false
                    }
                }
                .catch {
                    report(R.string.location_load_failed)
                    mutableState.update { it.copy(ready = true) }
                }
                .collect { saved ->
                    val previous = mutableState.value
                    mutableState.update {
                        it.copy(
                            saved = saved,
                            ready = true,
                            message = it.message.takeUnless { message ->
                                message == R.string.location_load_failed
                            }
                        )
                    }
                    if (!saved.automatic) cancelFix()
                    if (!previous.ready || (!previous.saved.automatic && saved.automatic)) {
                        refresh()
                    }
                }
        }
    }

    fun hasPermission(): Boolean = resolver.hasPermission()
    fun onForegroundChanged(active: Boolean) {
        foreground = active
        if (active) refresh() else cancelFix()
    }

    fun onDeviceLocationChanged() {
        if (!foreground || !state.value.saved.automatic) return
        if (!resolver.isEnabled()) cancelFix()
        refresh()
    }

    fun permissionDenied() {
        report(R.string.location_permission_denied, LocationRecovery.APP_SETTINGS)
    }

    fun settingsUnavailable() {
        report(R.string.location_settings_unavailable)
    }

    fun enableAutomatic() {
        if (state.value.saving) return
        if (!resolver.hasPermission()) {
            permissionDenied()
            return
        }
        mutableState.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            try {
                preferences.enableAutomatic()
                // Refresh also handles retrying an already-selected GPS mode.
                mutableState.update {
                    it.copy(saved = it.saved.copy(automatic = true), saving = false)
                }
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                report(R.string.location_save_failed)
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun selectManual(label: String, onSaved: () -> Unit) {
        if (state.value.saving) return
        cancelFix()
        mutableState.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            try {
                preferences.selectManual(label)
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                report(R.string.location_save_failed)
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun refresh() {
        val current = state.value
        if (!foreground || !current.ready || !current.saved.automatic ||
            current.saving || locationJob?.isActive == true) return
        if (!resolver.hasPermission()) {
            permissionDenied()
            return
        }
        if (!resolver.isEnabled()) {
            report(R.string.location_gps_disabled, LocationRecovery.LOCATION_SETTINGS)
            return
        }
        mutableState.update {
            it.copy(locating = true, message = null, recovery = LocationRecovery.NONE)
        }
        val generation = ++fixGeneration
        locationJob = viewModelScope.launch {
            try {
                val fix = resolver.currentLocation()
                if (fix == null) {
                    when {
                        !resolver.hasPermission() -> permissionDenied()
                        !resolver.isEnabled() -> report(
                            R.string.location_gps_disabled, LocationRecovery.LOCATION_SETTINGS
                        )
                        else -> report(R.string.location_fix_failed)
                    }
                    return@launch
                }
                val areaName = resolver.areaName(fix)
                ensureActive()
                try {
                    preferences.saveDetected(
                        areaName ?: getApplication<Application>()
                            .getString(R.string.location_device_fallback)
                    )
                } catch (_: IOException) {
                    report(R.string.location_save_failed)
                    return@launch
                }
                if (areaName == null) report(R.string.location_name_unavailable)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                permissionDenied()
            } catch (_: Exception) {
                report(R.string.location_fix_failed)
            } finally {
                if (generation == fixGeneration) {
                    mutableState.update { it.copy(locating = false) }
                }
            }
        }
    }

    private fun cancelFix() {
        fixGeneration++
        locationJob?.cancel()
        locationJob = null
        mutableState.update { it.copy(locating = false) }
    }

    private fun report(message: Int, recovery: LocationRecovery = LocationRecovery.NONE) {
        mutableState.update { it.copy(message = message, recovery = recovery) }
    }
}

package com.fixoom.core.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** One foreground fix. No continuous requests and no background service. */
class ForegroundLocation(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED

    fun isEnabled(): Boolean = manager != null && LocationManagerCompat.isLocationEnabled(manager)

    suspend fun currentLocation(): Location? {
        if (!hasPermission() || !isEnabled()) return null
        val locationManager = manager ?: return null
        val providers = buildList {
            // The platform fused provider is optional; Google Play Services is not required.
            add("fused")
            add(LocationManager.NETWORK_PROVIDER)
            if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                add(LocationManager.GPS_PROVIDER)
            }
        }
            .filter { provider ->
                runCatching {
                        locationManager.isProviderEnabled(provider)
                    }
                    .getOrDefault(false)
            }
        if (providers.isEmpty()) return null

        return withTimeoutOrNull(20_000) {
            awaitFirstFix(locationManager, providers)
        }
    }

    @SuppressLint("MissingPermission") // Checked above; revocation is caught per request.
    private suspend fun awaitFirstFix(
        locationManager: LocationManager,
        providers: List<String>,
    ): Location? = suspendCancellableCoroutine { continuation ->
        val signals = providers.map { CancellationSignal() }
        continuation.invokeOnCancellation { signals.forEach { it.cancel() } }
        var remaining = providers.size

        fun finish(location: Location?) {
            // All callbacks run on the main executor.
            if (!continuation.isActive) return
            remaining--
            if (location != null || remaining == 0) {
                signals.forEach { it.cancel() }
                continuation.resume(location)
            }
        }

        providers.forEachIndexed { index, provider ->
            if (continuation.isActive) {
                try {
                    LocationManagerCompat.getCurrentLocation(
                        locationManager,
                        provider,
                        signals[index],
                        ContextCompat.getMainExecutor(appContext),
                        ::finish,
                    )
                } catch (_: SecurityException) {
                    finish(null)
                } catch (_: IllegalArgumentException) {
                    finish(null)
                } catch (_: IllegalStateException) {
                    finish(null)
                }
            }
        }
    }
}

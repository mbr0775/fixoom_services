package com.fixoom.customer.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** One foreground fix. No continuous requests and no background service. */
class DeviceLocationResolver(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) ==
            PackageManager.PERMISSION_GRANTED

    fun isEnabled(): Boolean =
        manager != null && LocationManagerCompat.isLocationEnabled(manager)

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
        }.filter { provider ->
            runCatching {
                locationManager.isProviderEnabled(provider)
            }.getOrDefault(false)
        }
        if (providers.isEmpty()) return null

        return withTimeoutOrNull(20_000) {
            awaitFirstFix(locationManager, providers)
        }
    }

    @SuppressLint("MissingPermission") // Checked above; revocation is caught per request.
    private suspend fun awaitFirstFix(
        locationManager: LocationManager,
        providers: List<String>
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
                        ::finish
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

    /** Best effort only: GPS can succeed even when reverse geocoding is offline. */
    suspend fun areaName(location: Location): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(appContext, Locale.getDefault())
        return try {
            val address = withTimeoutOrNull(6_000) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (continuation.isActive) {
                                        continuation.resume(addresses.firstOrNull())
                                    }
                                }

                                override fun onError(errorMessage: String?) {
                                    if (continuation.isActive) continuation.resume(null)
                                }
                            }
                        )
                    }
                } else {
                    // Never run the pre-Android 13 blocking API on the UI thread.
                    @Suppress("DEPRECATION")
                    runInterruptible(Dispatchers.IO) {
                        geocoder.getFromLocation(
                            location.latitude, location.longitude, 1
                        )?.firstOrNull()
                    }
                }
            } ?: return null
            val area = address.locality ?: address.subAdminArea ?: address.adminArea
            listOfNotNull(area, address.countryName)
                .filter(String::isNotBlank)
                .distinct()
                .joinToString(", ")
                .takeIf(String::isNotBlank)
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

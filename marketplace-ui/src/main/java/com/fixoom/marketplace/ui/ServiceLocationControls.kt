package com.fixoom.marketplace.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fixoom.core.data.location.ForegroundLocation
import com.fixoom.core.data.location.JobLocationSharing
import com.fixoom.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ServiceCoverageControls(
    saved: ServiceLocation?,
    saving: Boolean,
    onSave: (Double?, Double?, Int) -> Unit,
) {
    val context = LocalContext.current
    val resolver = remember(context) { ForegroundLocation(context) }
    val scope = rememberCoroutineScope()
    var radius by remember(saved?.radiusKm) { mutableIntStateOf(saved?.radiusKm ?: 25) }
    var locating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun publish() {
        if (locating || saving) return
        locating = true
        error = null
        scope.launch {
            try {
                val fix = resolver.currentLocation()
                if (fix == null)
                    error = "A fresh location could not be found. Enable Location and try again."
                else onSave(fix.latitude, fix.longitude, radius)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error = "Location is unavailable. Check permission and try again."
            } finally {
                locating = false
            }
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (resolver.hasPermission()) publish()
            else error = "Location permission is needed to set your service base."
        }
    InfoCard(
        "Where you offer services",
        "Use your business/service base here. Customers nearby see an approximate distance, not your exact coordinates. This is separate from voluntary live job tracking.",
    ) {
        Text(
            if (saved != null) "Service location saved / ${saved.radiusKm} km coverage"
            else "No service location saved. You cannot appear in GPS nearby results yet."
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 25, 50, 100).forEach { km ->
                FilterChip(
                    radius == km,
                    { radius = km },
                    label = { Text("$km km") },
                    enabled = !saving && !locating,
                )
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            {
                if (resolver.hasPermission()) publish()
                else
                    permission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                    )
            },
            enabled = !saving && !locating,
        ) {
            Text(if (locating) "Finding location..." else "Use current location as service base")
        }
        if (saved != null)
            TextButton({ onSave(null, null, radius) }, enabled = !saving && !locating) {
                Text("Remove service location")
            }
    }
}

@Composable
fun BookingTrackingCard(
    booking: ServiceBooking,
    location: JobLocation?,
    provider: Boolean,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val sharing by JobLocationSharing.state.collectAsStateWithLifecycle()
    val active = booking.status in listOf("accepted", "in_progress")
    var error by remember { mutableStateOf<String?>(null) }
    fun start() {
        JobLocationSharing.start(context, booking.id)
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (ForegroundLocation(context).hasPermission()) start()
            else error = "Allow location to share this job's position."
        }
    InfoCard(
        "Service tracking",
        when (booking.status) {
            "requested" -> "Request saved. Waiting for your provider to accept."
            "accepted" -> "Your provider accepted the booking. Follow progress and messages here."
            "in_progress" -> "The provider has started your service."
            "completed" -> "Service completed. Your job history and receipt remain available."
            "cancelled" -> "This booking was cancelled."
            else -> "This request is closed."
        },
    ) {
        if (active && provider) {
            Text(
                "Optional: share your live position only with this booking's customer and the authorized administrator. Sharing continues in the background with a visible phone notification. Stop it at any time; it ends when the job closes.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (sharing.bookingId == booking.id)
                OutlinedButton({
                    JobLocationSharing.stop(context)
                    onRefresh()
                }) {
                    Text("Stop live location")
                }
            else
                Button({
                    if (ForegroundLocation(context).hasPermission()) start()
                    else
                        permission.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            )
                        )
                }) {
                    Text("Share live location for this job")
                }
            sharing.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (active && !provider) {
            if (location == null)
                Text("Live location is not currently shared, or the last position has expired.")
            else {
                Text("Last position: ${serviceTime(location.updatedAt)}")
                Text(
                    "Accuracy about ${location.accuracyM.toInt()} m. This is a reported position, not an arrival estimate.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton({
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
                                ),
                            )
                        )
                    }
                        .onFailure { error = "No maps application is available on this phone." }
                }) {
                    Text("View provider position on map")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

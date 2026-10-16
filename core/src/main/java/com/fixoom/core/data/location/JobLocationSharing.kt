package com.fixoom.core.data.location

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.core.data.repository.MarketplaceRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SharingState(val bookingId: String? = null, val error: String? = null)

object JobLocationSharing {
    internal val mutable = MutableStateFlow(SharingState())
    val state = mutable.asStateFlow()

    fun start(context: Context, bookingId: String) {
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, JobLocationService::class.java).putExtra("booking_id", bookingId),
            )
        } catch (_: Exception) {
            mutable.value =
                SharingState(
                    error = "Location sharing could not start. Keep the app open and try again."
                )
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, JobLocationService::class.java))
    }
}

/** A visible, explicitly started foreground service. No background-location permission. */
class JobLocationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeJob: Job? = null
    private var bookingId: String? = null
    private var ownerId: String? = null
    private val repo by lazy { MarketplaceRepository() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        val id =
            intent?.getStringExtra("booking_id")
                ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }
        if (bookingId == id && activeJob?.isActive == true) return START_NOT_STICKY
        if (activeJob?.isActive == true) {
            JobLocationSharing.mutable.value =
                SharingState(bookingId, "Stop sharing the current job before starting another.")
            return START_NOT_STICKY
        }
        ownerId = FixoomSupabase.client.auth.currentUserOrNull()?.id
        bookingId = id
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                "fixoom_job_location",
                "Live job location",
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        val stop =
            PendingIntent.getService(
                this,
                72,
                Intent(this, JobLocationService::class.java).setAction("STOP"),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val open =
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                PendingIntent.getActivity(
                    this,
                    73,
                    it,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }
        val notification =
            NotificationCompat.Builder(this, "fixoom_job_location")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Fixoom live job location")
                .setContentText("Sharing with your assigned customer. Tap Stop to end.")
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(0, "Stop sharing", stop)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()
        try {
            ServiceCompat.startForeground(
                this,
                72,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } catch (_: Exception) {
            JobLocationSharing.mutable.value =
                SharingState(error = "Location permission is required to share in the background.")
            stopSelf()
            return START_NOT_STICKY
        }
        JobLocationSharing.mutable.value = SharingState(id)
        activeJob = scope.launch {
            try {
                val resolver = ForegroundLocation(this@JobLocationService)
                while (isActive) {
                    if (FixoomSupabase.client.auth.currentUserOrNull()?.id != ownerId) break
                    val booking = withTimeout(15_000) { repo.booking(id) }
                    if (
                        booking == null ||
                            booking.providerId != ownerId ||
                            booking.status !in listOf("accepted", "in_progress")
                    )
                        break
                    val fix = resolver.currentLocation()
                    ensureActive()
                    if (fix != null) {
                        withTimeout(15_000) {
                            repo.shareLocation(
                                id,
                                fix.latitude,
                                fix.longitude,
                                fix.accuracy.toDouble(),
                            )
                        }
                        JobLocationSharing.mutable.value = SharingState(id)
                    } else
                        JobLocationSharing.mutable.value =
                            SharingState(
                                id,
                                "Waiting for a fresh position. Check that Location is enabled.",
                            )
                    delay(15_000)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                JobLocationSharing.mutable.value =
                    SharingState(
                        error =
                            "Location sharing stopped after a connection or permission error. The last position expires within five minutes."
                    )
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        val id = bookingId
        val owner = ownerId
        if (JobLocationSharing.mutable.value.bookingId == id)
            JobLocationSharing.mutable.value = SharingState()
        if (id != null && owner != null)
            cleanupScope.launch {
                if (FixoomSupabase.client.auth.currentUserOrNull()?.id == owner)
                    runCatching { withTimeout(10_000) { repo.shareLocation(id, null, null) } }
            }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

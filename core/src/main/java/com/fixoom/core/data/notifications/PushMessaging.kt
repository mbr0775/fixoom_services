package com.fixoom.core.data.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.core.data.repository.AuthRepository
import com.fixoom.core.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class PushState(
    val configured: Boolean = false,
    val registered: Boolean = false,
    val error: String? = null,
)

object PushMessaging {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutable = MutableStateFlow(PushState())
    val state = mutable.asStateFlow()
    private var appContext: Context? = null
    private var registrationJob: Job? = null
    @Volatile private var signingOut = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val firebase = runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
        if (firebase == null) return
        context
            .getSystemService(android.app.NotificationManager::class.java)
            .createNotificationChannel(
                android.app.NotificationChannel(
                    "fixoom_bookings",
                    "Booking requests and replies",
                    android.app.NotificationManager.IMPORTANCE_HIGH,
                )
            )
        mutable.value = PushState(configured = true)
        scope.launch {
            FixoomSupabase.client.auth.sessionStatus.collectLatest {
                if (FixoomSupabase.client.auth.currentSessionOrNull() == null) {
                    registrationJob?.cancel()
                    signingOut = false
                    mutable.value = PushState(configured = true)
                } else refresh(context)
            }
        }
    }

    fun refresh(context: Context) {
        if (!mutable.value.configured || signingOut) return
        registrationJob?.cancel()
        registrationJob = scope.launch {
            if (
                Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
            )
                return@launch
            try {
                withTimeout(15_000) {
                    val profile = AuthRepository().getCurrentProfile() ?: return@withTimeout
                    if (profile.accountStatus != AccountStatus.ACTIVE) return@withTimeout
                    val role = context.packageName.substringAfterLast('.')
                    if (role != profile.role.name.lowercase()) return@withTimeout
                    val token = FirebaseMessaging.getInstance().token.await()
                    if (signingOut) return@withTimeout
                    FixoomSupabase.client.postgrest.rpc(
                        "register_push_device",
                        buildJsonObject {
                            put("p_installation_id", installation(context))
                            put("p_token", token)
                            put("p_app_role", role)
                        },
                    )
                    mutable.value = PushState(true, true)
                }
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                mutable.value =
                    PushState(
                        true,
                        false,
                        "Phone alerts could not connect. Open this screen to retry.",
                    )
            } catch (_: Exception) {
                mutable.value =
                    PushState(
                        true,
                        false,
                        "Phone alerts could not connect. Check your connection and retry.",
                    )
            }
        }
    }

    private fun installation(context: Context): String {
        val prefs = context.getSharedPreferences("fixoom_push", Context.MODE_PRIVATE)
        return prefs.getString("installation", null)
            ?: UUID.randomUUID().toString().also {
                prefs.edit().putString("installation", it).apply()
            }
    }

    suspend fun beforeSignOut() {
        val context = appContext ?: return
        if (!mutable.value.configured) return
        signingOut = true
        try {
            withTimeoutOrNull(4_000) {
                registrationJob?.cancelAndJoin()
                runCatching {
                    FixoomSupabase.client.postgrest.rpc(
                        "unregister_push_device",
                        buildJsonObject { put("p_installation_id", installation(context)) },
                    )
                }
                runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
            }
        } finally {
            mutable.value = PushState(true, false)
            context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
        }
    }
}

class FixoomMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushMessaging.refresh(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val uid = FixoomSupabase.client.auth.currentUserOrNull()?.id ?: return
        if (data["recipient_id"] != uid) return
        val id = data["notification_id"] ?: return
        val booking = data["fixoom_booking_id"] ?: return
        BookingAlerts.show(
            this,
            BookingNotification(
                id,
                booking,
                "push",
                message.notification?.title ?: "Fixoom booking update",
                message.notification?.body ?: "Open your booking for the latest update.",
                data["created_at"].orEmpty(),
            ),
        )
    }
}

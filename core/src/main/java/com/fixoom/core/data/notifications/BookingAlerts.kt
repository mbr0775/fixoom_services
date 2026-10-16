package com.fixoom.core.data.notifications

import android.Manifest
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fixoom.core.model.BookingNotification

object BookingAlerts {
    fun show(context: Context, notification: BookingNotification) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        )
            return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                "fixoom_bookings",
                "Booking requests and replies",
                NotificationManager.IMPORTANCE_HIGH,
            )
        )
        val launch =
            context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.putExtra("fixoom_booking_id", notification.bookingId) ?: return
        val pending =
            PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val visible =
            NotificationCompat.Builder(context, "fixoom_bookings")
                .setSmallIcon(com.fixoom.core.R.drawable.ic_fixoom_notification)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notification.id.hashCode(), visible)
        }
    }
}

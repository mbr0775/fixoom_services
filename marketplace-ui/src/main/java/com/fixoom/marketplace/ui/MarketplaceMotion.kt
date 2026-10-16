package com.fixoom.marketplace.ui

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Decorative motion stops in the background, battery saver and system reduced-motion mode. */
@Composable
fun rememberMarketplaceMotionAllowed(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(false) }
    DisposableEffect(context, owner) {
        val power = context.getSystemService(PowerManager::class.java)
        fun refresh() {
            allowed =
                owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    ValueAnimator.areAnimatorsEnabled() &&
                    power?.isPowerSaveMode != true
        }
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = refresh()
            }
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) = refresh()
            }
        val lifecycle = LifecycleEventObserver { _, _ -> refresh() }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        owner.lifecycle.addObserver(lifecycle)
        refresh()
        onDispose {
            owner.lifecycle.removeObserver(lifecycle)
            context.contentResolver.unregisterContentObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    return allowed
}

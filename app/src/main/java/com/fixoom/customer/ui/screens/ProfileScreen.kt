package com.fixoom.customer.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fixoom.customer.BuildConfig
import com.fixoom.customer.R
import com.fixoom.customer.ui.location.HomeLocationPicker
import com.fixoom.customer.ui.location.HomeLocationViewModel
import com.fixoom.customer.ui.profile.CustomerProfileViewModel
import com.fixoom.customer.ui.profile.ProfileContent
import com.fixoom.customer.ui.profile.ProfileDetailSheet
import com.fixoom.customer.ui.profile.ProfilePanel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onBookingsClick: () -> Unit,
    onExploreClick: () -> Unit
) {
    val accountViewModel: CustomerProfileViewModel = viewModel()
    val account by accountViewModel.state.collectAsStateWithLifecycle()
    val locationViewModel: HomeLocationViewModel = viewModel()
    val location by locationViewModel.state.collectAsStateWithLifecycle()
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf(ProfilePanel.NONE) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val settingsUnavailableMessage = stringResource(R.string.profile_settings_unavailable)

    DisposableEffect(lifecycleOwner, accountViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) accountViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            accountViewModel.refresh()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Same persisted location as Home; permission requests only follow a GPS tap.
    HomeLocationPicker(
        viewModel = locationViewModel,
        state = location,
        expanded = showLocationPicker,
        onDismiss = { showLocationPicker = false }
    )

    val showSettings: (Boolean) -> Unit = { notifications ->
        if (!openProfileSettings(context, notifications)) {
            scope.launch { snackbar.showSnackbar(settingsUnavailableMessage) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ProfileContent(
            account = account,
            locationLabel = location.saved.selection?.label,
            isDarkMode = isDarkMode,
            versionName = BuildConfig.VERSION_NAME,
            onDarkModeChange = onDarkModeChange,
            onBookings = onBookingsClick,
            onExplore = onExploreClick,
            onLocation = { showLocationPicker = true },
            onNotifications = { showSettings(true) },
            onPermissions = { showSettings(false) },
            onAccountDetails = { panel = ProfilePanel.ACCOUNT },
            onHelp = { panel = ProfilePanel.HELP },
            onAbout = { panel = ProfilePanel.ABOUT },
            onRetry = accountViewModel::refresh
        )
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (panel != ProfilePanel.NONE) {
        ProfileDetailSheet(
            panel = panel,
            account = account,
            versionName = BuildConfig.VERSION_NAME,
            onRetry = accountViewModel::refresh,
            onDismiss = { panel = ProfilePanel.NONE }
        )
    }
}

private fun openProfileSettings(context: Context, notifications: Boolean): Boolean {
    val details = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    val requested = if (notifications) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else details

    fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    return launch(requested) || (notifications && launch(details))
}

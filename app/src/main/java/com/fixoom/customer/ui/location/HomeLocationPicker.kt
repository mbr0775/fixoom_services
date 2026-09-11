package com.fixoom.customer.ui.location

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fixoom.customer.R
import com.fixoom.customer.data.location.isValidLocationLabel
import com.fixoom.customer.data.location.normalizeLocationLabel

/**
 * Keep this composed outside the header's lazy item so scrolling does not reset
 * permission launchers or cancel lifecycle handling.
 */
@Composable
fun HomeLocationPicker(
    viewModel: HomeLocationViewModel,
    state: HomeLocationUiState,
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (viewModel.hasPermission()) {
            viewModel.enableAutomatic()
        } else {
            viewModel.permissionDenied()
        }
    }
    val settings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onDeviceLocationChanged()
    }

    DisposableEffect(lifecycleOwner, viewModel, appContext) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onForegroundChanged(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.onForegroundChanged(false)
                else -> Unit
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.onDeviceLocationChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION).apply {
                addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        viewModel.onForegroundChanged(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            appContext.unregisterReceiver(receiver)
            viewModel.onForegroundChanged(false)
        }
    }

    if (expanded) {
        LocationPickerSheet(
            state = state,
            onDismiss = onDismiss,
            onManualSelection = { viewModel.selectManual(it, onDismiss) },
            onUseGps = {
                if (viewModel.hasPermission()) {
                    viewModel.enableAutomatic()
                } else {
                    // Android 12+ supports choosing approximate access here.
                    permissions.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            },
            onRecovery = {
                val intent = when (state.recovery) {
                    LocationRecovery.LOCATION_SETTINGS ->
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    LocationRecovery.APP_SETTINGS ->
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    LocationRecovery.NONE -> null
                }
                if (intent != null) {
                    try {
                        settings.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        viewModel.settingsUnavailable()
                    }
                } else {
                    viewModel.refresh()
                }
            }
        )
    }
}

// Convenience choices, not a claim of provider coverage in these areas.
private val sriLankanAreas = listOf(
    "Colombo", "Dehiwala", "Mount Lavinia", "Nugegoda", "Maharagama",
    "Kotte", "Moratuwa", "Negombo", "Gampaha", "Kalutara", "Kandy",
    "Galle", "Matara", "Kurunegala", "Ratnapura", "Anuradhapura",
    "Jaffna", "Trincomalee", "Batticaloa", "Badulla", "Nuwara Eliya"
).map { "$it, Sri Lanka" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPickerSheet(
    state: HomeLocationUiState,
    onDismiss: () -> Unit,
    onManualSelection: (String) -> Unit,
    onUseGps: () -> Unit,
    onRecovery: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    val cleanSearch = normalizeLocationLabel(search)
    val areas = remember(cleanSearch) {
        sriLankanAreas.filter { it.contains(cleanSearch, ignoreCase = true) }
    }
    val colors = MaterialTheme.colorScheme
    val enabled = state.ready && !state.saving

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .testTag("location_picker"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.location_sheet_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.location_sheet_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.location_close)
                        )
                    }
                }
            }
            item {
                Surface(
                    onClick = onUseGps,
                    enabled = enabled && !state.locating,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .testTag("use_current_location"),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.locating || state.saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.MyLocation, contentDescription = null)
                        }
                        Text(
                            stringResource(
                                if (state.locating) R.string.location_finding
                                else R.string.location_use_device
                            ),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.saved.automatic && !state.locating) {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.location_privacy),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            state.saved.selection?.let { selected ->
                item {
                    Text(
                        stringResource(R.string.location_saved, selected.label),
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            state.message?.let { message ->
                item {
                    Surface(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.errorContainer,
                        contentColor = colors.onErrorContainer
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                stringResource(message),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.semantics {
                                    liveRegion = LiveRegionMode.Polite
                                }
                            )
                            if (state.recovery != LocationRecovery.NONE ||
                                state.saved.automatic) {
                                TextButton(
                                    onClick = onRecovery,
                                    enabled = enabled && !state.locating
                                ) {
                                    Text(
                                        stringResource(
                                            when (state.recovery) {
                                                LocationRecovery.APP_SETTINGS ->
                                                    R.string.location_open_app_settings
                                                LocationRecovery.LOCATION_SETTINGS ->
                                                    R.string.location_open_gps_settings
                                                LocationRecovery.NONE -> R.string.location_retry
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { if (it.length <= 80) search = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("location_search"),
                    enabled = enabled,
                    label = { Text(stringResource(R.string.location_search_label)) },
                    placeholder = { Text(stringResource(R.string.location_search_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    isError = cleanSearch.isNotEmpty() && !isValidLocationLabel(cleanSearch),
                    supportingText = {
                        Text(
                            stringResource(
                                if (cleanSearch.isNotEmpty() &&
                                    !isValidLocationLabel(cleanSearch)) R.string.location_invalid
                                else R.string.location_manual_help
                            )
                        )
                    }
                )
            }
            if (isValidLocationLabel(cleanSearch)) {
                item {
                    Button(
                        onClick = { onManualSelection(cleanSearch) },
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .heightIn(min = 48.dp)
                            .testTag("save_custom_location")
                    ) {
                        Text(stringResource(R.string.location_use_typed, cleanSearch))
                    }
                }
            }
            item {
                Text(
                    stringResource(
                        if (cleanSearch.isBlank()) R.string.location_area_list
                        else R.string.location_area_results
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
            }
            if (areas.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.location_no_matches),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            items(areas, key = { it }) { area ->
                val selected = !state.saved.automatic &&
                    state.saved.selection?.label == area
                Surface(
                    onClick = { onManualSelection(area) },
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) colors.secondaryContainer else colors.surface,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = colors.primary
                        )
                        Text(area, modifier = Modifier.weight(1f))
                        if (selected) Icon(Icons.Rounded.Check, contentDescription = null)
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

package com.fixoom.provider.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixoom.core.model.Profile
import com.fixoom.provider.R
import com.fixoom.provider.ui.components.ProviderBrand
import com.fixoom.provider.ui.components.ProviderDepthHero
import com.fixoom.provider.ui.theme.FixoomLime
import com.fixoom.provider.ui.theme.FixoomTealDark

@Composable
fun ProviderDashboardScreen(
    profile: Profile,
    availability: Boolean?,
    onAvailabilityChange: (Boolean) -> Unit,
    onOpenJobRequests: () -> Unit,
    onOpenActiveJobs: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenEarnings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    val displayName = profile.fullName?.trim()?.takeIf { it.isNotEmpty() }
        ?: stringResource(R.string.provider_name_fallback)
    val profileLabel = stringResource(R.string.provider_profile)
    Scaffold(modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)) {
                ProviderNavItem(true, Icons.Rounded.Home, stringResource(R.string.provider_home), {}, "home")
                ProviderNavItem(false, Icons.Rounded.Handyman, stringResource(R.string.provider_requests), onOpenJobRequests, "requests")
                ProviderNavItem(false, Icons.Rounded.CalendarMonth, stringResource(R.string.provider_schedule), onOpenSchedule, "schedule")
                ProviderNavItem(false, Icons.Rounded.PersonOutline, stringResource(R.string.provider_profile), onOpenProfile, "profile")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag("provider_dashboard_scroll").verticalScroll(scroll)
            .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                ProviderBrand(Modifier.weight(1f))
                IconButton(onClick = onOpenNotifications, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.NotificationsNone, stringResource(R.string.provider_notifications))
                }
                IconButton(onClick = onOpenProfile,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(48.dp)
                        .semantics { contentDescription = profileLabel }) {
                    Text(displayName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(stringResource(R.string.provider_greeting, displayName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProviderDepthHero(
                    title = stringResource(R.string.provider_home_title),
                    eyebrow = stringResource(R.string.provider_workspace),
                    caption = stringResource(R.string.provider_home_caption),
                    compact = true,
                    scrollOffset = { scroll.value.toFloat() }
                )
                Button(onClick = onOpenJobRequests,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FixoomLime, contentColor = FixoomTealDark),
                    shape = RoundedCornerShape(17.dp)) {
                    Text(stringResource(R.string.provider_view_requests), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp))
                }
            }
            AvailabilityPanel(availability, onAvailabilityChange, onOpenProfile)
            Text(stringResource(R.string.provider_manage_work), style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                WorkAction(Icons.Rounded.Handyman, R.string.provider_job_requests, R.string.provider_job_requests_detail, Color(0xFFCDEED8), onOpenJobRequests)
                WorkAction(Icons.Rounded.WorkHistory, R.string.provider_active_jobs, R.string.provider_active_jobs_detail, Color(0xFFD9CCFF), onOpenActiveJobs)
                WorkAction(Icons.Rounded.CalendarMonth, R.string.provider_schedule, R.string.provider_schedule_detail, Color(0xFFDDEFF3), onOpenSchedule)
                WorkAction(Icons.Rounded.AccountBalanceWallet, R.string.provider_earnings, R.string.provider_earnings_detail, Color(0xFFFFE5D6), onOpenEarnings)
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(22.dp)) {
                    Text(stringResource(R.string.provider_profile_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(7.dp))
                    Text(stringResource(R.string.provider_profile_detail), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onOpenProfile, contentPadding = PaddingValues(top = 12.dp, bottom = 4.dp)) {
                        Text(stringResource(R.string.provider_open_profile))
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.provider_sign_out))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RowScope.ProviderNavItem(selected: Boolean, icon: ImageVector, label: String, onClick: () -> Unit, destination: String) {
    NavigationBarItem(selected = selected, onClick = onClick, modifier = Modifier.testTag("provider_nav_$destination"),
        icon = { Icon(icon, null, Modifier.size(22.dp)) },
        label = { Text(label, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
        ))
}

@Composable
private fun AvailabilityPanel(value: Boolean?, onChange: (Boolean) -> Unit, onSetup: () -> Unit) {
    val availabilityLabel = stringResource(R.string.provider_availability)
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(availabilityLabel, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(when (value) {
                    true -> R.string.provider_available
                    false -> R.string.provider_unavailable
                    null -> R.string.provider_availability_unknown
                }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (value == null) {
                TextButton(onClick = onSetup) { Text(stringResource(R.string.provider_setup)) }
            } else {
                Switch(checked = value, onCheckedChange = onChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = FixoomLime,
                        checkedThumbColor = FixoomTealDark,
                        checkedBorderColor = FixoomLime,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                        .semantics { contentDescription = availabilityLabel })
            }
        }
    }
}

@Composable
private fun WorkAction(icon: ImageVector, title: Int, description: Int, accent: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(19.dp), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).background(accent, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = FixoomTealDark, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(description), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

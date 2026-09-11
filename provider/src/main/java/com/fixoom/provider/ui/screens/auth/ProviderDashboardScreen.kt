package com.fixoom.provider.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WorkHistory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fixoom.core.model.Profile

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
    val displayName =
        profile.fullName
            ?.takeIf { it.isNotBlank() }
            ?: "Provider"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                )
        ) {
            DashboardHeader(
                displayName = displayName,
                onOpenProfile = onOpenProfile,
                onOpenNotifications = onOpenNotifications
            )

            Spacer(modifier = Modifier.height(24.dp))

            DashboardHero(displayName = displayName)

            Spacer(modifier = Modifier.height(20.dp))

            AvailabilityCard(
                availability = availability,
                onAvailabilityChange =
                    onAvailabilityChange
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Manage your work",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            DashboardAction(
                icon = Icons.Rounded.Handyman,
                title = "Job requests",
                description =
                    "Review new customer service requests.",
                onClick = onOpenJobRequests
            )

            Spacer(modifier = Modifier.height(12.dp))

            DashboardAction(
                icon = Icons.Rounded.WorkHistory,
                title = "Active jobs",
                description =
                    "Manage accepted and ongoing work.",
                onClick = onOpenActiveJobs
            )

            Spacer(modifier = Modifier.height(12.dp))

            DashboardAction(
                icon = Icons.Rounded.CalendarMonth,
                title = "Schedule",
                description =
                    "Review bookings and working hours.",
                onClick = onOpenSchedule
            )

            Spacer(modifier = Modifier.height(12.dp))

            DashboardAction(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = "Earnings",
                description =
                    "View completed-job payments and earnings.",
                onClick = onOpenEarnings
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Logout,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = "Sign out",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DashboardHeader(
    displayName: String,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello,",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Notifications"
                )
            }

            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier.background(
                    color =
                        MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Provider profile",
                    tint =
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun DashboardHero(
    displayName: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                ),
                shape = RoundedCornerShape(30.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = "  Verified provider",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Ready for your\nnext service job?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(72.dp)
                .background(
                    color =
                        MaterialTheme.colorScheme.onPrimary
                            .copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Handyman,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun AvailabilityCard(
    availability: Boolean?,
    onAvailabilityChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Availability",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when (availability) {
                    true -> "Available for new requests"
                    false -> "Currently unavailable"
                    null -> "Loading availability…"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (availability == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Switch(
                checked = availability,
                onCheckedChange = onAvailabilityChange
            )
        }
    }
}

@Composable
private fun DashboardAction(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    color =
                        MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(17.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 15.dp)
                .fillMaxWidth(0.82f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
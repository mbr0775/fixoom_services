package com.fixoom.marketplace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.fixoom.core.data.notifications.BookingAlerts
import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.core.data.repository.MarketplaceRepository
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.data.repository.MarketplaceViewModel
import com.fixoom.core.model.*
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ObserveMarketplace(vm: MarketplaceViewModel) = ObserveServiceUpdates { vm.refresh() }

@Composable
fun ObserveServiceUpdates(bookingId: String? = null, refresh: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current.applicationContext
    val auth by FixoomSupabase.client.auth.sessionStatus.collectAsStateWithLifecycle()
    val latest by rememberUpdatedState(refresh)
    LaunchedEffect(owner, auth, bookingId) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            supervisorScope {
                launch {
                    try {
                        MarketplaceRepository().updates(bookingId).collect { notification ->
                            latest()
                            notification?.let { BookingAlerts.show(context, it) }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        /* The bounded refresh loop remains the recovery path. */
                    }
                }
                while (isActive) {
                    latest()
                    delay(30_000)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplacePage(
    title: String,
    state: MarketplaceState,
    refresh: () -> Unit,
    onBack: (() -> Unit)? = null,
    headerActions: @Composable RowScope.() -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    if (onBack != null)
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                        }
                },
                actions = {
                    headerActions()
                    IconButton(onClick = refresh, enabled = !state.saving) {
                        Icon(Icons.Rounded.Refresh, "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            state = listState,
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.saving)
                item {
                    Text("Saving…", color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            state.error?.let { item { InfoCard("Unable to update", it, error = true) } }
            state.notice?.let { item { InfoCard("Saved", it) } }
            content()
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    text: String,
    error: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (error) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
            ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (text.isNotBlank())
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (error) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            content()
        }
    }
}

@Composable
fun BookingSummary(booking: ServiceBooking, provider: Boolean, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                booking.reference,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                (if (provider) booking.customerName else booking.providerName).ifBlank {
                    booking.categoryName.ifBlank { serviceStatus(booking.categorySlug) }
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                booking.categoryName.ifBlank { serviceStatus(booking.categorySlug) },
                style = MaterialTheme.typography.labelLarge,
            )
            Text(serviceTime(booking.scheduledAt), style = MaterialTheme.typography.bodyMedium)
            Text(
                "${booking.durationMinutes} minutes reserved",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                serviceStatus(booking.status),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(booking.serviceAddress, maxLines = 2, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ProfessionalSummary(professional: ServiceProfessional, category: String, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            ProviderPhoto(professional.avatarPath, professional.displayName, Modifier.size(56.dp))
            Text(
                category,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                professional.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(professional.serviceArea, style = MaterialTheme.typography.bodyMedium)
            professional.distanceKm?.let {
                Text(
                    "About ${it} km away / straight-line distance",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "Available for requests",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

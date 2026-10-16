package com.fixoom.marketplace.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.AccountStatus
import com.fixoom.core.model.serviceStatus

@Composable
fun ProviderProfileScreens(
    state: MarketplaceState,
    pending: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onSaveProfile: (String, String, String, String, Boolean, () -> Unit) -> Unit,
    onSavePhoto: (ByteArray?, () -> Unit) -> Unit,
    onCheckApproval: () -> Unit,
    onSignOut: () -> Unit,
    checkingApproval: Boolean = false,
    approvalMessage: String? = null,
    approvalError: String? = null,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    BackHandler(editing) { if (!state.saving) editing = false }
    if (editing) {
        ProviderProfileEditor(
            state,
            pending,
            onRefresh,
            { if (!state.saving) editing = false },
            { name, category, area, bio, available ->
                onSaveProfile(name, category, area, bio, available) { editing = false }
            },
            onSavePhoto,
        )
    } else {
        ProviderProfileOverview(
            state,
            pending,
            onRefresh,
            onBack,
            { editing = true },
            onCheckApproval,
            onSignOut,
            checkingApproval,
            approvalMessage,
            approvalError,
        )
    }
}

@Composable
private fun ProviderProfileEditor(
    state: MarketplaceState,
    pending: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onSaveProfile: (String, String, String, String, Boolean) -> Unit,
    onSavePhoto: (ByteArray?, () -> Unit) -> Unit,
) {
    val service = state.data.ownService
    var name by rememberSaveable(service?.id) { mutableStateOf(service?.displayName.orEmpty()) }
    var area by rememberSaveable(service?.id) { mutableStateOf(service?.serviceArea.orEmpty()) }
    var bio by rememberSaveable(service?.id) { mutableStateOf(service?.bio.orEmpty()) }
    var category by
        rememberSaveable(service?.id) { mutableStateOf(service?.categorySlug.orEmpty()) }
    var available by rememberSaveable(service?.id) { mutableStateOf(service?.isAvailable ?: false) }
    var validation by remember { mutableStateOf<String?>(null) }
    MarketplacePage("Edit profile", state, onRefresh, onBack) {
        item {
            ProviderPhotoEditor(
                service?.avatarPath,
                name.ifBlank { state.data.profile?.fullName.orEmpty() },
                state.saving || service == null,
                onSavePhoto,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    name,
                    { if (it.length <= 100) name = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Professional or business name") },
                    enabled = !state.saving,
                )
                Text("Service category", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.data.categories.forEach { c ->
                        FilterChip(
                            category == c.slug,
                            { category = c.slug },
                            label = { Text(c.name) },
                            enabled = !state.saving,
                        )
                    }
                }
                OutlinedTextField(
                    area,
                    { if (it.length <= 150) area = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Service area, e.g. Colombo") },
                    enabled = !state.saving,
                )
                OutlinedTextField(
                    bio,
                    { if (it.length <= 2000) bio = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Experience and services offered") },
                    minLines = 4,
                    enabled = !state.saving,
                )
                if (!pending)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Available for new requests")
                        Switch(available, { available = it }, enabled = !state.saving)
                    }
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    {
                        validation =
                            if (
                                name.trim().length < 2 ||
                                    area.trim().length < 2 ||
                                    category.isBlank()
                            )
                                "Add your professional name, category and service area."
                            else null
                        if (validation == null) onSaveProfile(name, category, area, bio, available)
                    },
                    enabled = service != null && !state.saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                ) {
                    Text("Save professional profile")
                }
            }
        }
    }
}

@Composable
private fun ProviderProfileOverview(
    state: MarketplaceState,
    pending: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCheckApproval: () -> Unit,
    onSignOut: () -> Unit,
    checkingApproval: Boolean,
    approvalMessage: String?,
    approvalError: String?,
) {
    val profile = state.data.profile
    val service = state.data.ownService
    val name = service?.displayName?.takeIf { it.isNotBlank() } ?: profile?.fullName.orEmpty()
    val category =
        state.data.categories.firstOrNull { it.slug == service?.categorySlug }?.name
            ?: service?.categorySlug?.let(::serviceStatus)
    MarketplacePage(
        "My profile",
        state,
        onRefresh,
        onBack,
        headerActions = {
            IconButton(onEdit, enabled = service != null && !state.saving) {
                Icon(Icons.Rounded.Edit, "Edit profile")
            }
        },
    ) {
        if (profile == null) {
            item {
                InfoCard(
                    "Profile unavailable",
                    "Your saved profile details will appear when your account loads. Refresh to try again.",
                )
            }
        } else {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Box(Modifier.padding(5.dp)) {
                                ProviderPhoto(
                                    service?.avatarPath ?: profile.avatarUrl,
                                    name,
                                    Modifier.size(112.dp),
                                )
                            }
                        }
                        Text(
                            name.ifBlank { "Name not added" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            category ?: "Service category not added",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                when (profile.accountStatus) {
                                    AccountStatus.PENDING -> "Awaiting approval"
                                    AccountStatus.ACTIVE -> "Active provider"
                                    AccountStatus.SUSPENDED -> "Account suspended"
                                    AccountStatus.REJECTED -> "Application rejected"
                                    AccountStatus.DELETED -> "Account deleted"
                                },
                                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
            item {
                InfoCard("Service details", "") {
                    ProfileDetail(
                        "Service area",
                        service?.serviceArea.orEmpty().ifBlank { "Not added yet" },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetail(
                        "About your service",
                        service?.bio.orEmpty().ifBlank { "No description added yet" },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetail(
                        "Availability",
                        when {
                            pending -> "Available after approval"
                            service?.categorySlug == null || service.serviceArea.isBlank() ->
                                "Complete your service profile to set availability"
                            service.isAvailable -> "Available for new requests"
                            else -> "Not accepting new requests"
                        },
                    )
                }
            }
            item {
                InfoCard("Account details", "") {
                    ProfileDetail(
                        "Full name",
                        profile.fullName.orEmpty().ifBlank { "Not added yet" },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetail(
                        "Email address",
                        profile.email.orEmpty().ifBlank { "No email supplied" },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileDetail(
                        "Phone number",
                        profile.phone.orEmpty().ifBlank { "No phone supplied" },
                    )
                }
            }
        }
        if (pending) {
            item {
                InfoCard(
                    "Awaiting administrator approval",
                    "Use the edit icon to complete your service profile for review. After approval, turn on availability to receive customer requests.",
                ) {
                    Button(onCheckApproval, enabled = !checkingApproval && !state.saving) {
                        if (checkingApproval) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (checkingApproval) "Checking approval..." else "Check approval status"
                        )
                    }
                    approvalMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    approvalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        item {
            OutlinedButton(onSignOut, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun ProfileDetail(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

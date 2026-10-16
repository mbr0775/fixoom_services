package com.fixoom.marketplace.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fixoom.core.data.repository.MarketplaceState

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
    motionEnabled: Boolean = rememberMarketplaceMotionAllowed(),
    onRequestSkill: ((String, String, () -> Unit) -> Unit)? = null,
    onWithdrawSkill: ((String) -> Unit)? = null,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(page != 0) { if (!state.saving) page = 0 }
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (motionEnabled) {
                (fadeIn(tween(220)) +
                        slideInHorizontally(tween(260)) {
                            if (targetState != 0) it / 12 else -it / 12
                        })
                    .togetherWith(fadeOut(tween(120)))
            } else fadeIn(snap()).togetherWith(fadeOut(snap()))
        },
        label = "profile_edit_navigation",
    ) { destination ->
        if (destination == 2 && onRequestSkill != null && onWithdrawSkill != null) {
            ProviderSkillsScreen(
                state,
                onRefresh,
                { if (!state.saving) page = 0 },
                onRequestSkill,
                onWithdrawSkill,
            )
        } else if (destination == 1) {
            ProviderProfileEditor(
                state,
                pending,
                onRefresh,
                { if (!state.saving) page = 0 },
                { name, category, area, bio, available ->
                    onSaveProfile(name, category, area, bio, available) { page = 0 }
                },
                onSavePhoto,
            )
        } else {
            ProviderProfileOverview(
                state,
                pending,
                onRefresh,
                onBack,
                { page = 1 },
                onCheckApproval,
                onSignOut,
                checkingApproval,
                approvalMessage,
                approvalError,
                motionEnabled,
                if (onRequestSkill != null && onWithdrawSkill != null) ({ page = 2 }) else null,
            )
        }
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
                Text("Primary approved skill", style = MaterialTheme.typography.titleMedium)
                if (service?.approvedCategories.isNullOrEmpty())
                    Text(
                        "Request your service skills from the profile screen. They become bookable after administrator approval.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.data.categories
                        .filter { service?.offers(it.slug) == true }
                        .forEach { c ->
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
                        Switch(
                            available,
                            { available = it },
                            enabled =
                                !state.saving &&
                                    state.data.categories.any { service?.offers(it.slug) == true },
                        )
                    }
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    {
                        validation =
                            if (name.trim().length < 2 || area.trim().length < 2)
                                "Add your professional name and service area."
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

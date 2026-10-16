package com.fixoom.marketplace.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.AccountStatus
import com.fixoom.core.model.serviceStatus

private val ProfileInk = Color(0xFF10363C)
private val ProfileTeal = Color(0xFF194850)
private val ProfileLime = Color(0xFFC8F43D)
private val ProfileMint = Color(0xFFEAF5EE)
private val ProfileLavender = Color(0xFFEEE9FA)
private val ProfilePeach = Color(0xFFFFEBDD)

@Composable
internal fun ProviderProfileOverview(
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
    motionEnabled: Boolean,
) {
    val profile = state.data.profile
    val service = state.data.ownService
    val name = service?.displayName?.takeIf { it.isNotBlank() } ?: profile?.fullName.orEmpty()
    val category =
        state.data.categories.firstOrNull { it.slug == service?.categorySlug }?.name
            ?: service?.categorySlug?.let(::serviceStatus)
    val list = rememberLazyListState()
    val scrollOffset = {
        list.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == "profile_identity" }
            ?.let { (-it.offset).coerceAtLeast(0).toFloat() } ?: 0f
    }
    val photoAdded = !(service?.avatarPath ?: profile?.avatarUrl).isNullOrBlank()
    val personalAdded =
        !profile?.fullName.isNullOrBlank() &&
            !profile?.email.isNullOrBlank() &&
            !profile?.phone.isNullOrBlank()
    val serviceAdded =
        !service?.displayName.isNullOrBlank() &&
            !service?.categorySlug.isNullOrBlank() &&
            !service?.serviceArea.isNullOrBlank()
    val aboutAdded = !service?.bio.isNullOrBlank()
    val essentials = listOf(photoAdded, personalAdded, serviceAdded, aboutAdded)
    MarketplacePage(
        "My profile",
        state,
        onRefresh,
        onBack,
        headerActions = {
            FilledTonalIconButton(
                onEdit,
                enabled = service != null && !state.saving,
                colors =
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = ProfileTeal,
                        contentColor = Color.White,
                    ),
            ) {
                Icon(Icons.Rounded.Edit, "Edit profile", Modifier.size(20.dp))
            }
        },
        listState = list,
    ) {
        if (profile == null) {
            item {
                InfoCard(
                    "Profile unavailable",
                    "Your saved profile details will appear when your account loads. Refresh to try again.",
                )
            }
        } else {
            item(key = "profile_identity") {
                ProfileIdentityHero(
                    name.ifBlank { "Name not added" },
                    category ?: "Service category not added",
                    service?.avatarPath ?: profile.avatarUrl,
                    profile.accountStatus,
                    scrollOffset,
                    motionEnabled,
                )
            }
            item(key = "profile_essentials") { ProfileEssentials(essentials) }
            if (pending) {
                item(key = "profile_approval") {
                    InfoCard(
                        "Awaiting administrator approval",
                        "Complete your service profile using the edit icon so the administrator can review it.",
                    ) {
                        Button(onCheckApproval, enabled = !checkingApproval && !state.saving) {
                            if (checkingApproval) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (checkingApproval) "Checking approval..."
                                else "Check approval status"
                            )
                        }
                        approvalMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        approvalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item(key = "profile_sections_label") {
                Text(
                    "Your details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item(key = "profile_details") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(0xFFE5ECEA)),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        ProfileSection(
                            "Personal details",
                            "Name, email and phone",
                            ProfileDetailIcons.Contact,
                            ProfileMint,
                            personalAdded,
                            motionEnabled,
                        ) {
                            ProfileDetailField(
                                "Full name",
                                profile.fullName.orEmpty().ifBlank { "Not added yet" },
                                ProfileDetailIcons.Person,
                                ProfileMint,
                                motionEnabled,
                            )
                            ProfileDetailField(
                                "Email address",
                                profile.email.orEmpty().ifBlank { "No email supplied" },
                                ProfileDetailIcons.Email,
                                ProfileLavender,
                                motionEnabled,
                                order = 1,
                            )
                            ProfileDetailField(
                                "Phone number",
                                profile.phone.orEmpty().ifBlank { "No phone supplied" },
                                ProfileDetailIcons.Phone,
                                ProfilePeach,
                                motionEnabled,
                                order = 2,
                            )
                        }
                        ProfileSectionDivider()
                        ProfileSection(
                            "Service details",
                            listOfNotNull(
                                    category,
                                    service?.serviceArea?.takeIf { it.isNotBlank() },
                                )
                                .joinToString(" / ")
                                .ifBlank { "Category, service area and availability" },
                            ProfileDetailIcons.Services,
                            ProfileLavender,
                            serviceAdded,
                            motionEnabled,
                        ) {
                            ProfileDetailField(
                                "Service category",
                                category ?: "Not added yet",
                                ProfileDetailIcons.Category,
                                ProfileLavender,
                                motionEnabled,
                            )
                            ProfileDetailField(
                                "Service area",
                                service?.serviceArea.orEmpty().ifBlank { "Not added yet" },
                                ProfileDetailIcons.Location,
                                ProfileMint,
                                motionEnabled,
                                order = 1,
                            )
                            ProfileDetailField(
                                "Availability",
                                when {
                                    pending -> "Available after approval"
                                    !serviceAdded ->
                                        "Complete your service profile to set availability"
                                    service?.isAvailable == true -> "Available for new requests"
                                    else -> "Not accepting new requests"
                                },
                                ProfileDetailIcons.Availability,
                                ProfileMint,
                                motionEnabled,
                                order = 2,
                            )
                        }
                        ProfileSectionDivider()
                        ProfileSection(
                            "About your work",
                            service?.bio?.takeIf { it.isNotBlank() }
                                ?: "Your experience and services",
                            ProfileDetailIcons.Introduction,
                            ProfilePeach,
                            aboutAdded,
                            motionEnabled,
                        ) {
                            ProfileDetailField(
                                "Experience and services",
                                service?.bio.orEmpty().ifBlank { "No description added yet" },
                                ProfileDetailIcons.Introduction,
                                ProfilePeach,
                                motionEnabled,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Use the edit icon to update your photo and profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                )
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
private fun ProfileIdentityHero(
    name: String,
    category: String,
    photo: String?,
    status: AccountStatus,
    scrollOffset: () -> Float,
    motionEnabled: Boolean,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal =
        animateFloatAsState(
            if (entered || !motionEnabled) 1f else 0f,
            animationSpec = if (motionEnabled) tween(400) else snap(),
            label = "profile_portrait_reveal",
        )
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = ProfileInk,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.background(Brush.linearGradient(listOf(ProfileTeal, ProfileInk)))) {
            Canvas(
                Modifier.matchParentSize()
                    .clearAndSetSemantics {}
                    .graphicsLayer {
                        translationY =
                            if (motionEnabled) (scrollOffset() * .22f).coerceAtMost(28.dp.toPx())
                            else 0f
                    }
            ) {
                drawCircle(
                    Color.White.copy(alpha = .055f),
                    size.width * .43f,
                    Offset(size.width * .95f, size.height * .08f),
                    style = Stroke(1.dp.toPx()),
                )
                drawCircle(
                    Color.White.copy(alpha = .04f),
                    size.width * .65f,
                    Offset(size.width * .95f, size.height * .08f),
                    style = Stroke(1.dp.toPx()),
                )
                drawCircle(
                    ProfileLime.copy(alpha = .07f),
                    size.width * .32f,
                    Offset(size.width * .03f, size.height * 1.03f),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(118.dp), contentAlignment = Alignment.Center) {
                    DecorativeProfileTile(
                        Icons.Rounded.Handyman,
                        ProfileLavender,
                        Modifier.align(Alignment.CenterStart).padding(start = 10.dp).graphicsLayer {
                            rotationZ = -12f
                            rotationY =
                                if (motionEnabled)
                                    (scrollOffset() / density * .05f).coerceAtMost(9f)
                                else 0f
                            translationY =
                                if (motionEnabled)
                                    (scrollOffset() * .08f).coerceAtMost(12.dp.toPx())
                                else 0f
                        },
                    )
                    DecorativeProfileTile(
                        Icons.Rounded.Place,
                        ProfilePeach,
                        Modifier.align(Alignment.CenterEnd)
                            .padding(end = 10.dp, top = 24.dp)
                            .graphicsLayer {
                                rotationZ = 12f
                                translationY =
                                    if (motionEnabled)
                                        (scrollOffset() * .3f).coerceAtMost(30.dp.toPx())
                                    else 0f
                            },
                    )
                    Box(
                        Modifier.size(114.dp).graphicsLayer {
                            translationY =
                                if (motionEnabled)
                                    (1 - reveal.value) * 14.dp.toPx() +
                                        (scrollOffset() * .1f).coerceAtMost(14.dp.toPx())
                                else 0f
                            rotationX =
                                if (motionEnabled)
                                    (scrollOffset() / density * .025f).coerceAtMost(4f)
                                else 0f
                            scaleX = .94f + reveal.value * .06f
                            scaleY = scaleX
                            cameraDistance = 24 * density
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                            drawCircle(
                                ProfileLime.copy(alpha = .35f),
                                size.minDimension / 2,
                                style = Stroke(1.dp.toPx()),
                            )
                        }
                        Surface(shape = CircleShape, color = Color.White, shadowElevation = 10.dp) {
                            Box(Modifier.padding(4.dp)) {
                                ProviderPhoto(photo, name, Modifier.size(92.dp))
                            }
                        }
                    }
                }
                Text(
                    name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    category,
                    color = Color(0xFFD0E3DF),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (status == AccountStatus.ACTIVE) ProfileLime else ProfilePeach,
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            if (status == AccountStatus.ACTIVE) Icons.Rounded.CheckCircle
                            else Icons.Rounded.Info,
                            null,
                            tint = ProfileInk,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            when (status) {
                                AccountStatus.ACTIVE -> "Active provider"
                                AccountStatus.PENDING -> "Awaiting approval"
                                AccountStatus.SUSPENDED -> "Account suspended"
                                AccountStatus.REJECTED -> "Application rejected"
                                AccountStatus.DELETED -> "Account deleted"
                            },
                            color = ProfileInk,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecorativeProfileTile(icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(
        modifier.size(34.dp).clearAndSetSemantics {},
        shape = RoundedCornerShape(11.dp),
        color = color,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = ProfileInk, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProfileEssentials(added: List<Boolean>) {
    val count = added.count { it }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Profile essentials",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "$count/4 added",
                style = MaterialTheme.typography.labelMedium,
                color = ProfileTeal,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            Modifier.fillMaxWidth().height(5.dp).semantics {
                contentDescription = "Profile essentials"
                progressBarRangeInfo = ProgressBarRangeInfo(count / 4f, 0f..1f)
            },
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            added.forEach { complete ->
                Box(
                    Modifier.weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (complete) ProfileTeal else Color(0xFFDCE5E5))
                )
            }
        }
        Text(
            when (added.indexOfFirst { !it }) {
                0 -> "Add a profile photo to introduce yourself."
                1 -> "Add your name, email and phone details."
                2 -> "Add your service category and service area."
                3 -> "Tell customers about your experience."
                else -> "Your profile essentials are added. Keep them up to date."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    added: Boolean,
    motionEnabled: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val angle =
        animateFloatAsState(
            if (expanded) 90f else 0f,
            animationSpec = if (motionEnabled) tween(200) else snap(),
            label = "profile_section_chevron",
        )
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics {
                    stateDescription =
                        (if (expanded) "Expanded" else "Collapsed") +
                            if (added) ", details added" else ", details missing"
                }
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileSectionSymbol(icon, accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ProfileInk,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier.size(28.dp)
                    .clip(CircleShape)
                    .background(if (expanded) ProfileTeal else Color(0xFFF1F5F4)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    null,
                    Modifier.size(17.dp).graphicsLayer { rotationZ = angle.value },
                    tint = if (expanded) Color.White else ProfileTeal,
                )
            }
        }
        val details: @Composable () -> Unit = {
            Column(
                Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                content()
            }
        }
        if (motionEnabled) {
            AnimatedVisibility(
                expanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
            ) {
                details()
            }
        } else if (expanded) details()
    }
}

@Composable
private fun ProfileSectionSymbol(icon: ImageVector, accent: Color) {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(37.dp)
                .graphicsLayer {
                    rotationZ = -9f
                    translationY = 3.dp.toPx()
                }
                .clip(RoundedCornerShape(12.dp))
                .background(accent)
        )
        Surface(
            Modifier.size(38.dp),
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = .75f),
            border = BorderStroke(1.dp, Color.White),
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(24.dp), tint = ProfileTeal)
            }
        }
    }
}

@Composable
private fun ProfileSectionDivider() {
    HorizontalDivider(Modifier.padding(start = 76.dp, end = 18.dp), color = Color(0xFFEDF1F0))
}

package com.fixoom.customer.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixoom.customer.R
import com.fixoom.customer.ui.theme.FixoomMint
import com.fixoom.customer.ui.theme.FixoomTeal
import com.fixoom.customer.ui.theme.FixoomTealDark
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme
import kotlinx.coroutines.launch

internal enum class ProfilePanel { NONE, ACCOUNT, HELP, ABOUT }

@Composable
internal fun ProfileContent(
    account: ProfileAccountState,
    locationLabel: String?,
    isDarkMode: Boolean,
    versionName: String,
    onDarkModeChange: (Boolean) -> Unit,
    onBookings: () -> Unit,
    onExplore: () -> Unit,
    onLocation: () -> Unit,
    onNotifications: () -> Unit,
    onPermissions: () -> Unit,
    onAccountDetails: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("profile_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "identity") {
            ProfileEntrance(0) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.profile_eyebrow),
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.profile_title),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 4.dp).semantics { heading() }
                            )
                        }
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                            IconButton(onClick = onHelp, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.profile_quick_help))
                            }
                        }
                    }
                    IdentityCard(account, isDarkMode, onAccountDetails, onExplore, onRetry)
                }
            }
        }
        item(key = "quick_actions") {
            ProfileEntrance(1) {
                Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(
                        Icons.Rounded.CalendarMonth, R.string.profile_bookings, R.string.profile_bookings_hint,
                        Modifier.weight(1f).fillMaxHeight().testTag("profile_bookings"), onBookings
                    )
                    QuickAction(
                        Icons.Rounded.Explore, R.string.profile_explore, R.string.profile_explore_hint,
                        Modifier.weight(1f).fillMaxHeight().testTag("profile_explore"), onExplore
                    )
                }
            }
        }
        item(key = "essentials") {
            ProfileEntrance(2) {
                ProfileSection(R.string.profile_everyday) {
                    ProfileGroup {
                        ProfileSettingRow(
                            Icons.Rounded.LocationOn,
                            stringResource(R.string.profile_location),
                            locationLabel?.takeIf(String::isNotBlank) ?: stringResource(R.string.profile_location_empty),
                            "profile_location", onLocation
                        )
                        GroupDivider()
                        ProfileSettingRow(
                            Icons.Rounded.NotificationsNone,
                            stringResource(R.string.profile_notifications),
                            stringResource(R.string.profile_notifications_hint),
                            "profile_notifications", onNotifications
                        )
                    }
                }
            }
        }
        item(key = "appearance") {
            ProfileEntrance(3) {
                ProfileSection(R.string.profile_appearance) {
                    AppearanceCard(isDarkMode, onDarkModeChange)
                }
            }
        }
        item(key = "support") {
            ProfileEntrance(4) {
                ProfileSection(R.string.profile_help_section) {
                    ProfileGroup {
                        ProfileSettingRow(
                            Icons.AutoMirrored.Rounded.HelpOutline,
                            stringResource(R.string.profile_quick_help),
                            stringResource(R.string.profile_quick_help_hint),
                            "profile_help", onHelp
                        )
                        GroupDivider()
                        ProfileSettingRow(
                            Icons.Rounded.Security,
                            stringResource(R.string.profile_permissions),
                            stringResource(R.string.profile_permissions_hint),
                            "profile_permissions", onPermissions
                        )
                        GroupDivider()
                        ProfileSettingRow(
                            Icons.Rounded.Info,
                            stringResource(R.string.profile_about),
                            stringResource(R.string.profile_version, versionName),
                            "profile_about", onAbout
                        )
                    }
                }
            }
        }
        item(key = "footer") {
            Text(
                stringResource(R.string.profile_footer),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/** Finite, duration-scale-aware Compose animations; nothing runs continuously off-screen. */
@Composable
private fun ProfileEntrance(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val progress by animateFloatAsState(
        if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 380, delayMillis = (index * 40).coerceAtMost(160)),
        label = "profileSectionReveal"
    )
    Box(modifier = Modifier.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 16.dp.toPx()
    }) { content() }
}

@Composable
private fun IdentityCard(
    account: ProfileAccountState,
    dark: Boolean,
    onAccountDetails: () -> Unit,
    onExplore: () -> Unit,
    onRetry: () -> Unit
) {
    val ink = if (dark) Color.White else FixoomTealDark
    val softInk = if (dark) FixoomMint else FixoomTeal
    // Change text and background colors together to avoid a low-contrast theme transition.
    val start = if (dark) FixoomTealDark else lerp(Color.White, FixoomMint, 0.3f)
    val end = if (dark) FixoomTeal else FixoomMint
    val signedIn = account as? ProfileAccountState.SignedIn
    val name = when (account) {
        ProfileAccountState.Loading -> stringResource(R.string.profile_loading)
        ProfileAccountState.Guest -> stringResource(R.string.profile_guest_name)
        ProfileAccountState.Unavailable -> stringResource(R.string.profile_unavailable)
        is ProfileAccountState.SignedIn -> account.fullName ?: stringResource(R.string.profile_customer_name)
    }
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(start, end))).animateContentSize()
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(softInk.copy(alpha = 0.05f), radius = size.width * 0.35f, center = Offset(size.width, 0f))
            val wave = Path().apply {
                moveTo(0f, size.height * 0.9f)
                cubicTo(size.width * 0.3f, size.height * 0.72f, size.width * 0.7f, size.height * 1.12f, size.width, size.height * 0.78f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(wave, softInk.copy(alpha = 0.07f))
        }
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = FixoomTeal,
                    border = BorderStroke(2.dp, FixoomMint)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initials = profileInitials(signedIn?.fullName)
                        if (account == ProfileAccountState.Loading) {
                            CircularProgressIndicator(Modifier.size(26.dp), color = FixoomMint, strokeWidth = 2.dp)
                        } else if (initials.isNotEmpty()) {
                            Text(initials, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = FixoomMint)
                        } else {
                            Icon(Icons.Rounded.PersonOutline, null, Modifier.size(34.dp), FixoomMint)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (account == ProfileAccountState.Guest || signedIn != null) {
                        Text(
                            stringResource(if (signedIn == null) R.string.profile_guest_badge else R.string.profile_customer_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = softInk
                        )
                    }
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            if (account != ProfileAccountState.Loading) {
                Text(
                    stringResource(when (account) {
                        ProfileAccountState.Guest -> R.string.profile_guest_summary
                        ProfileAccountState.Unavailable -> R.string.profile_unavailable_body
                        else -> R.string.profile_customer_summary
                    }),
                    style = MaterialTheme.typography.bodyMedium,
                    color = softInk
                )
                Button(
                    onClick = when (account) {
                        ProfileAccountState.Guest -> onAccountDetails
                        ProfileAccountState.Unavailable -> onRetry
                        else -> onAccountDetails
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FixoomTealDark, contentColor = FixoomMint),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("profile_identity_action"),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(stringResource(when (account) {
                        ProfileAccountState.Guest -> R.string.profile_sign_in
                        ProfileAccountState.Unavailable -> R.string.profile_retry
                        else -> R.string.profile_account_details
                    }), modifier = Modifier.weight(1f, fill = false))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 12.dp).size(30.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, title: Int, subtitle: Int, modifier: Modifier, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "profileQuickPress")
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        interactionSource = interactions,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ProfileIcon(icon)
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProfileIcon(icon: ImageVector) {
    Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Icon(icon, null, Modifier.padding(12.dp).size(22.dp), MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ProfileSection(title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() }
        )
        content()
    }
}

@Composable
private fun ProfileGroup(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(4.dp)) { content() }
    }
}

@Composable
private fun ProfileSettingRow(icon: ImageVector, title: String, subtitle: String, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.heightIn(min = 84.dp).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProfileIcon(icon)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 74.dp, end = 18.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
}

@Composable
private fun AppearanceCard(isDarkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    val background = if (isDarkMode) FixoomTealDark else lerp(Color.White, FixoomMint, 0.35f)
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = background,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .toggleable(isDarkMode, role = Role.Switch, onValueChange = onDarkModeChange)
                    .testTag("profile_dark_mode").padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(color = if (isDarkMode) FixoomTeal else FixoomMint, shape = RoundedCornerShape(18.dp)) {
                    AnimatedContent(
                        targetState = isDarkMode,
                        transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.8f)) togetherWith fadeOut(tween(120)) },
                        label = "appearanceSunMoon"
                    ) { dark ->
                        Icon(if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null, Modifier.padding(14.dp).size(24.dp), if (dark) FixoomMint else FixoomTealDark)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.profile_dark_mode), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(if (isDarkMode) R.string.profile_dark_hint else R.string.profile_light_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = null,
                    thumbContent = {
                        Icon(if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null, Modifier.size(16.dp))
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = FixoomMint,
                        checkedThumbColor = FixoomTealDark,
                        checkedIconColor = FixoomMint,
                        uncheckedTrackColor = Color.White,
                        uncheckedThumbColor = FixoomTeal,
                        uncheckedIconColor = Color.White,
                        uncheckedBorderColor = FixoomTeal.copy(alpha = 0.45f)
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), if (isDarkMode) FixoomMint else FixoomTeal)
                Text(stringResource(R.string.profile_appearance_saved), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileDetailSheet(
    panel: ProfilePanel,
    account: ProfileAccountState,
    versionName: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val title = stringResource(when (panel) {
        ProfilePanel.ACCOUNT -> R.string.profile_account_details
        ProfilePanel.HELP -> R.string.profile_help_title
        else -> R.string.profile_about
    })
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 660.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { scope.launch { sheetState.hide(); onDismiss() } }) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.profile_close))
                }
            }
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (panel) {
                    ProfilePanel.ACCOUNT -> AccountDetails(account, onRetry)
                    ProfilePanel.HELP -> {
                        Text(stringResource(R.string.profile_help_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HelpAnswer(R.string.profile_help_location_question, R.string.profile_help_location_answer)
                        HelpAnswer(R.string.profile_help_theme_question, R.string.profile_help_theme_answer)
                        HelpAnswer(R.string.profile_help_bookings_question, R.string.profile_help_bookings_answer)
                    }
                    ProfilePanel.ABOUT -> {
                        Text("Fixoom", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.profile_about_body), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.profile_version, versionName), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ProfilePanel.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun AccountDetails(account: ProfileAccountState, onRetry: () -> Unit) {
    when (account) {
        is ProfileAccountState.SignedIn -> {
            Text(stringResource(R.string.profile_details_source), style = MaterialTheme.typography.bodyMedium)
            AccountField(R.string.profile_full_name, account.fullName)
            AccountField(R.string.profile_phone, account.phone)
        }
        ProfileAccountState.Loading -> {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.profile_loading))
        }
        ProfileAccountState.Guest -> Text(stringResource(R.string.profile_guest_badge))
        ProfileAccountState.Unavailable -> {
            Text(stringResource(R.string.profile_unavailable_body))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.profile_retry)) }
        }
    }
}

@Composable
private fun AccountField(label: Int, value: String?) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SelectionContainer { Text(value ?: stringResource(R.string.profile_not_added), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
private fun HelpAnswer(question: Int, answer: Int) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val expansion = stringResource(if (expanded) R.string.profile_expanded else R.string.profile_collapsed)
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(200), label = "helpChevron")
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.animateContentSize()) {
            Surface(
                onClick = { expanded = !expanded },
                modifier = Modifier.semantics { stateDescription = expansion },
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(question), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(22.dp).graphicsLayer { rotationZ = rotation })
                }
            }
            AnimatedVisibility(visible = expanded) {
                Text(stringResource(answer), modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(name = "Profile - light", showBackground = true, widthDp = 360, heightDp = 900)
@Preview(name = "Profile - compact text", showBackground = true, widthDp = 320, heightDp = 900, fontScale = 1.5f)
@Composable
private fun ProfileLightPreview() = ProfilePreview(false)

@Preview(name = "Profile - dark", showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun ProfileDarkPreview() = ProfilePreview(true)

@Composable
private fun ProfilePreview(dark: Boolean) {
    Fixoom_ServicesTheme(darkTheme = dark) {
        ProfileContent(
            account = ProfileAccountState.Guest, locationLabel = null, isDarkMode = dark, versionName = "1.0",
            onDarkModeChange = {}, onBookings = {}, onExplore = {}, onLocation = {}, onNotifications = {},
            onPermissions = {}, onAccountDetails = {}, onHelp = {}, onAbout = {}, onRetry = {}
        )
    }
}

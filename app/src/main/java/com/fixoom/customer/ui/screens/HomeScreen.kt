package com.fixoom.customer.ui.screens

import android.net.Uri
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fixoom.customer.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fixoom.customer.ui.location.HomeLocationPicker
import com.fixoom.customer.ui.location.HomeLocationUiState
import com.fixoom.customer.ui.location.HomeLocationViewModel
import com.fixoom.customer.ui.components.homeHeaderWaves
import com.fixoom.customer.ui.discovery.HomeServiceDiscovery
import com.fixoom.customer.ui.discovery.ServiceCategoryUi
import com.fixoom.core.model.ServiceCategory
import com.fixoom.core.model.ServiceProfessional
import com.fixoom.marketplace.ui.ProfessionalSummary
import com.fixoom.marketplace.ui.InfoCard
import com.fixoom.customer.ui.theme.FixoomLavender
import com.fixoom.customer.ui.theme.FixoomLime
import com.fixoom.customer.ui.theme.FixoomMint
import com.fixoom.customer.ui.theme.FixoomPeach
import com.fixoom.customer.ui.theme.FixoomSky
import com.fixoom.customer.ui.theme.FixoomTeal
import com.fixoom.customer.ui.theme.FixoomTealDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max

private data class HomeHeroBanner(
    val category: String,
    val eyebrow: String,
    val title: String,
    val description: String,
    val actionLabel: String,
    val videoRes: Int
)

private val homeHeroBanners = listOf(
    HomeHeroBanner(
        category = "Plumbing",
        eyebrow = "FAST PLUMBING HELP",
        title = "Stop leaks before they become disasters.",
        description = "Request plumbing repairs from approved service providers.",
        actionLabel = "Book a plumber",
        videoRes = R.raw.plumbing_animations
    ),
    HomeHeroBanner(
        category = "Electrical",
        eyebrow = "SAFE ELECTRICAL FIXES",
        title = "Power problems handled by trusted pros.",
        description = "Get dependable help for wiring, switches and electrical faults.",
        actionLabel = "Find an electrician",
        videoRes = R.raw.electrical_fixing
    ),
    HomeHeroBanner(
        category = "Carpentry",
        eyebrow = "PRECISION CARPENTRY",
        title = "Crafted repairs for every room.",
        description = "Connect with skilled carpenters for fitting, fixes and custom work.",
        actionLabel = "Find a carpenter",
        videoRes = R.raw.carpentry_fixing
    ),
    HomeHeroBanner(
        category = "Mechanical",
        eyebrow = "RELIABLE MECHANICAL CARE",
        title = "Keep essential equipment working smoothly.",
        description = "Find experienced professionals for inspection and repair work.",
        actionLabel = "Explore repairs",
        videoRes = R.raw.mechanical_fixing
    )
)

private val homeServices = listOf(
    ServiceCategoryUi(
        title = "Plumbing",
        description = "Leaks and pipe repairs",
        iconRes = R.drawable.ic_service_plumbing,
        accent = FixoomSky
    ),
    ServiceCategoryUi(
        title = "Electrical",
        description = "Wiring and electrical faults",
        iconRes = R.drawable.ic_service_electrical,
        accent = FixoomPeach
    ),
    ServiceCategoryUi(
        title = "Carpentry",
        description = "Furniture and woodwork",
        iconRes = R.drawable.ic_service_carpentry,
        accent = FixoomMint
    ),
    ServiceCategoryUi(
        title = "Painting",
        description = "Interior and exterior painting",
        iconRes = R.drawable.ic_service_painting,
        accent = FixoomLavender
    ),
    ServiceCategoryUi(
        title = "Masonry",
        description = "Walls and cement repairs",
        iconRes = R.drawable.ic_service_masonry,
        accent = Color(0xFFFFE8A3)
    ),
    ServiceCategoryUi(
        title = "Welding",
        description = "Gates and metal fabrication",
        iconRes = R.drawable.ic_service_welding,
        accent = Color(0xFFD7E1E8)
    )
)

@Composable
fun HomeScreen(
    onServiceClick: (String) -> Unit,
    onNotificationsClick: () -> Unit = {},
    categories: List<ServiceCategory> = emptyList(),
    professionals: List<ServiceProfessional> = emptyList(),
    onProviderClick: (String) -> Unit = {},
    loading: Boolean = false,
    dataError: String? = null,
    onRefresh: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val homeScope = rememberCoroutineScope()
    val locationViewModel: HomeLocationViewModel = viewModel()
    val locationState by locationViewModel.state.collectAsStateWithLifecycle()
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }

    HomeLocationPicker(
        viewModel = locationViewModel,
        state = locationState,
        expanded = showLocationPicker,
        onDismiss = { showLocationPicker = false }
    )

    var screenVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        screenVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedVisibility(
            visible = screenVisible,
            enter = fadeIn(
                animationSpec = tween(450)
            ) + slideInVertically(
                animationSpec = tween(450),
                initialOffsetY = { fullHeight ->
                    fullHeight / 12
                }
            )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item(key = "home_top") {
                    // A single full-width section: no list spacing between
                    // the welcome/location controls and the video banner.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HomeHeader(
                            onSearchClick = {
                                homeScope.launch {
                                    listState.animateScrollToItem(1)
                                }
                            },
                            onNotificationsClick = onNotificationsClick,
                            locationState = locationState,
                            onLocationClick = { showLocationPicker = true }
                        )
                        ServiceHeroCarousel(
                            onServiceClick = onServiceClick,
                            enabledCategories = categories.map { it.name }.toSet()
                        )
                    }
                }

                item(key = "service_discovery") {
                    if(loading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal=20.dp))
                    dataError?.let { InfoCard("Unable to refresh services",it,error=true) { androidx.compose.material3.TextButton(onRefresh){Text("Try again")} } }
                    HomeServiceDiscovery(
                        services = categories.map { category ->
                            homeServices.firstOrNull { it.title.equals(category.name,true) }?.copy(description=category.description)
                                ?: ServiceCategoryUi(category.name,category.description,R.drawable.ic_service_cleaning,FixoomMint)
                        },
                        onServiceClick = onServiceClick
                    )
                }

                item(key = "trust_strip") {
                    TrustStrip()
                }

                item(key = "featured_professionals") {
                    Column(Modifier.padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("Available professionals",style=MaterialTheme.typography.titleLarge)
                        if(!loading && dataError == null && professionals.isEmpty()) InfoCard("Your service network","Approved professionals appear here when they are available for requests.")
                        professionals.take(4).forEach { professional -> ProfessionalSummary(professional,categories.find { it.slug==professional.categorySlug }?.name.orEmpty()){onProviderClick(professional.id)} }
                    }
                }

                item(key = "how_it_works") {
                    HowFixoomWorks()
                }
            }
        }
    }

}

@Composable
private fun HomeHeader(
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    locationState: HomeLocationUiState,
    onLocationClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = colorScheme.background.luminance() < 0.5f

    val headerGradient = Brush.linearGradient(
        colors = if (isDarkMode) {
            listOf(FixoomTealDark, FixoomTeal)
        } else {
            listOf(
                colorScheme.surface,
                lerp(colorScheme.surface, FixoomMint, 0.14f)
            )
        }
    )
    val contentColor =
        if (isDarkMode) Color.White else colorScheme.onSurface
    val mutedColor =
        if (isDarkMode) Color.White.copy(alpha = 0.68f)
        else colorScheme.onSurfaceVariant
    val accentColor =
        if (isDarkMode) colorScheme.primary else FixoomTeal
    val actionContainerColor =
        if (isDarkMode) Color.White.copy(alpha = 0.08f)
        else colorScheme.surface.copy(alpha = 0.90f)
    val actionBorderColor =
        if (isDarkMode) Color.White.copy(alpha = 0.13f)
        else colorScheme.outline.copy(alpha = 0.26f)
    val panelColor =
        if (isDarkMode) Color.White.copy(alpha = 0.07f)
        else colorScheme.surface.copy(alpha = 0.78f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // The parent Scaffold already supplies the status-bar inset.
            .background(headerGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .homeHeaderWaves(darkTheme = isDarkMode)
                .padding(
                    start = 24.dp,
                    top = 12.dp,
                    end = 24.dp,
                    bottom = 24.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = colorScheme.secondary,
                        shadowElevation = if (isDarkMode) 0.dp else 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "F",
                                style = MaterialTheme.typography.titleLarge,
                                color = colorScheme.onSecondary,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(15.dp)
                            .background(FixoomLime, CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (isDarkMode) {
                                    FixoomTealDark
                                } else {
                                    colorScheme.surface
                                },
                                shape = CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "WELCOME BACK",
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Welcome to Fixoom",
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HeaderActionButton(
                    icon = Icons.Rounded.Search,
                    contentDescription = "Search services",
                    onClick = onSearchClick,
                    containerColor = actionContainerColor,
                    borderColor = actionBorderColor,
                    contentColor = contentColor
                )

                Spacer(modifier = Modifier.width(9.dp))

                Box {
                    HeaderActionButton(
                        icon = Icons.Rounded.Notifications,
                        contentDescription = "Open notifications",
                        onClick = onNotificationsClick,
                        containerColor = actionContainerColor,
                        borderColor = actionBorderColor,
                        contentColor = contentColor
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(11.dp)
                            .background(FixoomLime, CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (isDarkMode) {
                                    FixoomTeal
                                } else {
                                    colorScheme.surface
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                onClick = onLocationClick,
                enabled = locationState.ready,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = panelColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = actionBorderColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 13.dp,
                        vertical = 12.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = colorScheme.secondary.copy(alpha = 0.24f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(11.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.location_header),
                                modifier = Modifier.align(Alignment.CenterVertically),
                                style = MaterialTheme.typography.labelSmall,
                                color = mutedColor,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                shape = RoundedCornerShape(50),
                                color = colorScheme.secondary.copy(alpha = 0.22f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = accentColor.copy(alpha = 0.18f)
                                )
                            ) {
                                Text(
                                    text = stringResource(
                                        when {
                                            locationState.message != null ->
                                                R.string.location_attention_badge
                                            locationState.saved.automatic ->
                                                R.string.location_gps_badge
                                            locationState.saved.selection != null ->
                                                R.string.location_manual_badge
                                            else -> R.string.location_not_set_badge
                                        }
                                    ),
                                    modifier = Modifier.padding(
                                        horizontal = 7.dp,
                                        vertical = 2.dp
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = when {
                                !locationState.ready ->
                                    stringResource(R.string.location_loading)
                                locationState.locating ->
                                    stringResource(R.string.location_finding)
                                else -> locationState.saved.selection?.label
                                    ?: stringResource(R.string.location_choose)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleSmall,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // The whole location panel remains one accessible click target.
                    // This separated chevron is only its visual affordance.
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = accentColor.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = accentColor.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(
                                R.string.location_change
                            ),
                            modifier = Modifier.size(22.dp),
                            tint = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color,
    borderColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )
        }
    }
}

@Suppress("unused")
@Composable
private fun LegacyHomeHeader() {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = colorScheme.background.luminance() < 0.5f
    val headerShape = RoundedCornerShape(30.dp)

    val headerGradient = Brush.linearGradient(
        colors = if (isDarkMode) {
            listOf(FixoomTealDark, FixoomTeal)
        } else {
            listOf(colorScheme.surface, FixoomSky.copy(alpha = 0.92f))
        }
    )
    val headerContentColor =
        if (isDarkMode) Color.White else colorScheme.onSurface
    val headerMutedColor =
        if (isDarkMode) Color.White.copy(alpha = 0.78f)
        else colorScheme.onSurfaceVariant
    val headerAccentColor =
        if (isDarkMode) colorScheme.primary else FixoomTeal
    val chipContainerColor =
        if (isDarkMode) Color.White.copy(alpha = 0.12f)
        else colorScheme.surface.copy(alpha = 0.88f)
    val chipBorderColor =
        if (isDarkMode) Color.White.copy(alpha = 0.16f)
        else colorScheme.outline.copy(alpha = 0.30f)
    val logoContainerColor =
        if (isDarkMode) colorScheme.primary else colorScheme.secondary
    val logoContentColor =
        if (isDarkMode) colorScheme.onPrimary else colorScheme.onSecondary
    val trustContainerColor =
        if (isDarkMode) headerAccentColor.copy(alpha = 0.18f)
        else colorScheme.secondary.copy(alpha = 0.24f)
    val trustBorderColor =
        if (isDarkMode) headerAccentColor.copy(alpha = 0.36f)
        else FixoomTeal.copy(alpha = 0.16f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = 16.dp,
                top = 10.dp,
                end = 16.dp
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDarkMode) 0.dp else 8.dp,
                    shape = headerShape,
                    clip = false
                )
                .clip(headerShape)
                .background(headerGradient)
                .border(
                    width = 1.dp,
                    color = if (isDarkMode) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        colorScheme.outline.copy(alpha = 0.28f)
                    },
                    shape = headerShape
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = 18.dp,
                        end = 28.dp
                    )
                    .size(76.dp)
                    .background(
                        color = if (isDarkMode) {
                            Color.White.copy(alpha = 0.07f)
                        } else {
                            FixoomTeal.copy(alpha = 0.06f)
                        },
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 84.dp,
                        bottom = 12.dp
                    )
                    .size(34.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondary
                            .copy(alpha = 0.24f),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = chipContainerColor,
                        border = BorderStroke(
                            width = 1.dp,
                            color = chipBorderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Handyman,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = headerAccentColor
                            )

                            Spacer(modifier = Modifier.width(7.dp))

                            Text(
                                text = "FIXOOM",
                                style = MaterialTheme.typography.labelLarge,
                                color = headerContentColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = logoContainerColor,
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "F",
                                style = MaterialTheme.typography.titleLarge,
                                color = logoContentColor,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = currentGreeting(),
                    style = MaterialTheme.typography.labelLarge,
                    color = headerAccentColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "How can we help today?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = headerContentColor,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = "Book reliable local professionals " +
                            "without the hassle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = headerMutedColor
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        color = chipContainerColor,
                        border = BorderStroke(
                            width = 1.dp,
                            color = chipBorderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 8.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = headerAccentColor
                            )

                            Spacer(modifier = Modifier.width(5.dp))

                            Text(
                                text = "Colombo, Sri Lanka",
                                style = MaterialTheme.typography.labelMedium,
                                color = headerContentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = trustContainerColor,
                        border = BorderStroke(
                            width = 1.dp,
                            color = trustBorderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 8.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = headerAccentColor
                            )

                            Spacer(modifier = Modifier.width(5.dp))

                            Text(
                                text = "Trusted pros",
                                style = MaterialTheme.typography.labelMedium,
                                color = headerContentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceHeroCarousel(
    onServiceClick: (String) -> Unit,
    enabledCategories: Set<String>
) {
    val banners = homeHeroBanners.filter { it.category in enabledCategories }
    if(banners.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { banners.size }
    )
    val carouselScope = rememberCoroutineScope()

    LaunchedEffect(pagerState,banners.size) {
        while (true) {
            delay(3_000)

            if (!pagerState.isScrollInProgress) {
                val nextPage =
                    (pagerState.currentPage + 1) % banners.size

                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> banners[page].category }
        ) { page ->
            HomeHeroBannerPage(
                banner = banners[page],
                isActive = pagerState.currentPage == page &&
                        !pagerState.isScrollInProgress,
                onActionClick = {
                    onServiceClick(banners[page].category)
                }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 24.dp,
                    bottom = 18.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            banners.indices.forEach { page ->
                val indicatorWidth by animateDpAsState(
                    targetValue = if (pagerState.currentPage == page) {
                        26.dp
                    } else {
                        8.dp
                    },
                    animationSpec = tween(250),
                    label = "hero_indicator_width"
                )

                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == page) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                Color.White.copy(alpha = 0.42f)
                            }
                        )
                        .clickable {
                            carouselScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun HomeHeroBannerPage(
    banner: HomeHeroBanner,
    isActive: Boolean,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FixoomTealDark)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoopingHeroVideo(
                videoRes = banner.videoRes,
                isActive = isActive,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to FixoomTealDark,
                                0.43f to FixoomTealDark.copy(
                                    alpha = 0.96f
                                ),
                                0.68f to FixoomTeal.copy(
                                    alpha = 0.30f
                                ),
                                1.00f to Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                FixoomTealDark.copy(alpha = 0.04f),
                                FixoomTealDark.copy(alpha = 0.68f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 24.dp,
                        top = 22.dp,
                        end = 20.dp,
                        bottom = 42.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = FixoomLime.copy(alpha = 0.16f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = FixoomLime.copy(alpha = 0.50f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 7.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        color = FixoomLime,
                                        shape = CircleShape
                                    )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "VERIFIED PROS",
                                style = MaterialTheme.typography.labelSmall,
                                color = FixoomLime,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = FixoomTealDark.copy(alpha = 0.72f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = FixoomLime.copy(alpha = 0.42f)
                        )
                    ) {
                        Text(
                            text = banner.category,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(21.dp))

                Text(
                    text = banner.eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = FixoomLime,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = banner.title,
                    modifier = Modifier.fillMaxWidth(0.69f),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(9.dp))

                Text(
                    text = banner.description,
                    modifier = Modifier.fillMaxWidth(0.68f),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = FixoomLime,
                        contentColor = FixoomTealDark
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 18.dp,
                        vertical = 12.dp
                    )
                ) {
                    Text(
                        text = banner.actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = FixoomLime
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "Vetted pros",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = FixoomLime
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "Fixoom warranty",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun LoopingHeroVideo(
    videoRes: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shouldPlay by rememberUpdatedState(isActive)

    var mediaPlayer by remember(videoRes) {
        mutableStateOf<MediaPlayer?>(null)
    }
    var mediaSurface by remember(videoRes) {
        mutableStateOf<Surface?>(null)
    }
    var isPrepared by remember(videoRes) {
        mutableStateOf(false)
    }

    val textureView = remember(context, videoRes) {
        TextureView(context).apply textureViewScope@{
            isOpaque = true

            surfaceTextureListener =
                object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surfaceTexture: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        mediaSurface?.release()

                        val newSurface = Surface(surfaceTexture)
                        mediaSurface = newSurface

                        val descriptor =
                            context.resources.openRawResourceFd(videoRes)

                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(
                                descriptor.fileDescriptor,
                                descriptor.startOffset,
                                descriptor.length
                            )
                            descriptor.close()

                            setSurface(newSurface)
                            isLooping = true
                            setVolume(0f, 0f)

                            setOnVideoSizeChangedListener {
                                    _, videoWidth, videoHeight ->
                                applyCenterCropTransform(
                                    textureView = this@textureViewScope,
                                    videoWidth = videoWidth,
                                    videoHeight = videoHeight
                                )
                            }

                            setOnPreparedListener { player ->
                                isPrepared = true

                                if (shouldPlay) {
                                    player.start()
                                }
                            }

                            setOnErrorListener { _, _, _ ->
                                isPrepared = false
                                true
                            }

                            prepareAsync()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surfaceTexture: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        mediaPlayer?.let { player ->
                            applyCenterCropTransform(
                                textureView = this@textureViewScope,
                                videoWidth = player.videoWidth,
                                videoHeight = player.videoHeight
                            )
                        }
                    }

                    override fun onSurfaceTextureDestroyed(
                        surfaceTexture: SurfaceTexture
                    ): Boolean {
                        isPrepared = false
                        mediaPlayer?.release()
                        mediaPlayer = null
                        mediaSurface?.release()
                        mediaSurface = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(
                        surfaceTexture: SurfaceTexture
                    ) = Unit
                }
        }
    }

    DisposableEffect(textureView) {
        onDispose {
            isPrepared = false
            mediaPlayer?.release()
            mediaPlayer = null
            mediaSurface?.release()
            mediaSurface = null
        }
    }

    AndroidView(
        factory = { textureView },
        modifier = modifier
            .graphicsLayer {
                clip = true
            }
            .semantics {
                contentDescription =
                    "${bannerVideoDescription(videoRes)} animation"
            },
        update = {
            val player = mediaPlayer

            if (isPrepared && player != null) {
                runCatching {
                    if (isActive) {
                        if (!player.isPlaying) {
                            player.start()
                        }
                    } else if (player.isPlaying) {
                        player.pause()
                    }
                }
            }
        }
    )
}

private fun applyCenterCropTransform(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int
) {
    val viewWidth = textureView.width
    val viewHeight = textureView.height

    if (
        viewWidth <= 0 ||
        viewHeight <= 0 ||
        videoWidth <= 0 ||
        videoHeight <= 0
    ) {
        return
    }

    val widthScale = viewWidth.toFloat() / videoWidth.toFloat()
    val heightScale = viewHeight.toFloat() / videoHeight.toFloat()
    val cropScale = max(widthScale, heightScale)

    val scaledWidth = videoWidth * cropScale
    val scaledHeight = videoHeight * cropScale
    val scaleX = scaledWidth / viewWidth
    val scaleY = scaledHeight / viewHeight

    textureView.setTransform(
        Matrix().apply {
            setScale(
                scaleX,
                scaleY,
                viewWidth / 2f,
                viewHeight / 2f
            )
        }
    )
}

private fun bannerVideoDescription(
    videoRes: Int
): String {
    return when (videoRes) {
        R.raw.plumbing_animations -> "Plumbing service"
        R.raw.electrical_fixing -> "Electrical service"
        R.raw.carpentry_fixing -> "Carpentry service"
        R.raw.mechanical_fixing -> "Mechanical service"
        else -> "Home service"
    }
}

@Suppress("unused")
@Composable
private fun AnimatedServiceFinderCard(
    onBrowseServicesClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = colorScheme.background.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(
        label = "service_finder_animation"
    )
    val questionFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "question_float"
    )
    val smallQuestionFloat by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "small_question_float"
    )
    val phonePulse by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phone_pulse"
    )

    val cardShape = RoundedCornerShape(30.dp)
    val cardGradient = Brush.linearGradient(
        colors = if (isDarkMode) {
            listOf(
                colorScheme.surfaceVariant,
                colorScheme.surface
            )
        } else {
            listOf(
                FixoomSky.copy(alpha = 0.78f),
                FixoomMint.copy(alpha = 0.58f)
            )
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onBrowseServicesClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 0.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(
                    start = 20.dp,
                    top = 18.dp,
                    end = 12.dp,
                    bottom = 12.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(0.92f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorScheme.surface.copy(alpha = 0.82f)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(5.dp))

                            Text(
                                text = "SERVICE FINDER",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Not sure which service you need?",
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Browse by problem and find the right professional in seconds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Browse services",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = colorScheme.primary
                        )
                    }
                }

                AnimatedServiceFinderIllustration(
                    modifier = Modifier
                        .weight(1.08f)
                        .height(190.dp),
                    questionFloat = questionFloat,
                    smallQuestionFloat = smallQuestionFloat,
                    phonePulse = phonePulse
                )
            }
        }
    }
}

@Composable
private fun AnimatedServiceFinderIllustration(
    modifier: Modifier = Modifier,
    questionFloat: Float,
    smallQuestionFloat: Float,
    phonePulse: Float
) {
    val colorScheme = MaterialTheme.colorScheme
    val figureColor = colorScheme.primary
    val outlineColor = colorScheme.onSecondaryContainer
    val skinColor = FixoomPeach
    val benchColor = outlineColor.copy(alpha = 0.82f)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val w = size.width
            val h = size.height
            val lineWidth = 4.dp.toPx()

            drawCircle(
                color = colorScheme.secondary.copy(alpha = 0.17f),
                radius = w * 0.38f,
                center = Offset(w * 0.55f, h * 0.48f)
            )

            drawCircle(
                color = colorScheme.surface.copy(alpha = 0.72f),
                radius = w * 0.27f,
                center = Offset(w * 0.55f, h * 0.48f)
            )

            drawLine(
                color = outlineColor.copy(alpha = 0.18f),
                start = Offset(w * 0.08f, h * 0.87f),
                end = Offset(w * 0.94f, h * 0.87f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawRoundRect(
                color = benchColor,
                topLeft = Offset(w * 0.16f, h * 0.66f),
                size = Size(w * 0.69f, h * 0.08f),
                cornerRadius = CornerRadius(8.dp.toPx())
            )

            drawLine(
                color = benchColor,
                start = Offset(w * 0.24f, h * 0.72f),
                end = Offset(w * 0.20f, h * 0.86f),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = benchColor,
                start = Offset(w * 0.77f, h * 0.72f),
                end = Offset(w * 0.81f, h * 0.86f),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )

            drawCircle(
                color = outlineColor,
                radius = w * 0.092f,
                center = Offset(w * 0.48f, h * 0.29f)
            )

            drawCircle(
                color = skinColor,
                radius = w * 0.078f,
                center = Offset(w * 0.49f, h * 0.31f)
            )

            drawRoundRect(
                color = figureColor,
                topLeft = Offset(w * 0.36f, h * 0.39f),
                size = Size(w * 0.25f, h * 0.27f),
                cornerRadius = CornerRadius(16.dp.toPx())
            )

            drawLine(
                color = skinColor,
                start = Offset(w * 0.39f, h * 0.45f),
                end = Offset(w * 0.28f, h * 0.54f),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = skinColor,
                start = Offset(w * 0.28f, h * 0.54f),
                end = Offset(w * 0.38f, h * 0.58f),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = skinColor,
                start = Offset(w * 0.58f, h * 0.45f),
                end = Offset(w * 0.67f, h * 0.53f),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = outlineColor,
                start = Offset(w * 0.42f, h * 0.63f),
                end = Offset(w * 0.39f, h * 0.76f),
                strokeWidth = lineWidth + 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawLine(
                color = outlineColor,
                start = Offset(w * 0.39f, h * 0.76f),
                end = Offset(w * 0.29f, h * 0.82f),
                strokeWidth = lineWidth + 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawLine(
                color = outlineColor,
                start = Offset(w * 0.54f, h * 0.63f),
                end = Offset(w * 0.65f, h * 0.72f),
                strokeWidth = lineWidth + 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawLine(
                color = outlineColor,
                start = Offset(w * 0.65f, h * 0.72f),
                end = Offset(w * 0.71f, h * 0.83f),
                strokeWidth = lineWidth + 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(w * 0.64f, h * 0.43f),
                size = Size(w * 0.12f, h * 0.19f),
                cornerRadius = CornerRadius(5.dp.toPx())
            )

            drawRoundRect(
                color = colorScheme.surface,
                topLeft = Offset(w * 0.66f, h * 0.455f),
                size = Size(w * 0.08f, h * 0.13f),
                cornerRadius = CornerRadius(3.dp.toPx())
            )

            drawCircle(
                color = colorScheme.secondary.copy(alpha = phonePulse),
                radius = 3.dp.toPx() * phonePulse,
                center = Offset(w * 0.70f, h * 0.52f)
            )

            drawCircle(
                color = colorScheme.primary.copy(alpha = 0.14f),
                radius = w * 0.085f,
                center = Offset(w * 0.16f, h * 0.29f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        LoopingPlumbingVideo(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        QuestionBubble(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = 10.dp,
                    end = 8.dp
                )
                .offset(y = questionFloat.dp),
            size = 42.dp,
            textSize = MaterialTheme.typography.titleLarge
        )

        QuestionBubble(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = 34.dp,
                    start = 4.dp
                )
                .offset(y = smallQuestionFloat.dp),
            size = 30.dp,
            textSize = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun LoopingPlumbingVideo(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoView = remember(context) {
        VideoView(context).apply {
            setVideoURI(
                Uri.parse(
                    "android.resource://${context.packageName}/" +
                            R.raw.plumbing_animations
                )
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                mediaPlayer.setVolume(0f, 0f)
                start()
            }
        }
    }

    DisposableEffect(videoView) {
        onDispose {
            videoView.stopPlayback()
        }
    }

    AndroidView(
        factory = { videoView },
        modifier = modifier.semantics {
            contentDescription = "Plumber service animation"
        },
        update = { view ->
            if (!view.isPlaying) {
                view.start()
            }
        }
    )
}

@Composable
private fun QuestionBubble(
    modifier: Modifier,
    size: androidx.compose.ui.unit.Dp,
    textSize: androidx.compose.ui.text.TextStyle
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondary,
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "?",
                style = textSize,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun TrustStrip() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 16.dp
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrustItem(
                icon = Icons.Rounded.Shield,
                title = "Approved",
                subtitle = "professionals"
            )

            TrustDivider()

            TrustItem(
                icon = Icons.Rounded.CalendarMonth,
                title = "Easy",
                subtitle = "booking"
            )

            TrustDivider()

            TrustItem(
                icon = Icons.Rounded.Star,
                title = "Direct",
                subtitle = "payment"
            )
        }
    }
}

@Composable
private fun TrustItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrustDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(48.dp)
            .background(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
            )
    )
}

@Composable
private fun HowFixoomWorks() {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = colorScheme.background.luminance() < 0.5f
    var stepsVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        delay(120)
        stepsVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        SectionTitle(
            title = "Book with confidence",
            subtitle = "Three simple steps from search to service"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                color = colorScheme.outline.copy(alpha = 0.20f)
            ),
            shadowElevation = if (isDarkMode) 0.dp else 5.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isDarkMode) {
                                listOf(
                                    colorScheme.surfaceVariant,
                                    colorScheme.surface
                                )
                            } else {
                                listOf(
                                    FixoomSky.copy(alpha = 0.58f),
                                    colorScheme.surface,
                                    FixoomMint.copy(alpha = 0.48f)
                                )
                            }
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorScheme.surface.copy(alpha = 0.82f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = colorScheme.outline.copy(alpha = 0.18f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 7.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "HOW FIXOOM WORKS",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ModernBookingStep(
                        visible = stepsVisible,
                        animationDelay = 0,
                        number = "01",
                        icon = Icons.Rounded.Search,
                        iconContainerColor = FixoomSky,
                        title = "Choose your service",
                        description =
                            "Search or select exactly what your home needs."
                    )

                    BookingStepConnector(
                        visible = stepsVisible,
                        animationDelay = 100
                    )

                    ModernBookingStep(
                        visible = stepsVisible,
                        animationDelay = 140,
                        number = "02",
                        icon = Icons.Rounded.Star,
                        iconContainerColor = FixoomLavender,
                        title = "Compare trusted pros",
                        description =
                            "Read provider introductions, service areas and availability."
                    )

                    BookingStepConnector(
                        visible = stepsVisible,
                        animationDelay = 240
                    )

                    ModernBookingStep(
                        visible = stepsVisible,
                        animationDelay = 280,
                        number = "03",
                        icon = Icons.Rounded.CalendarMonth,
                        iconContainerColor = FixoomMint,
                        title = "Book and track",
                        description =
                            "Choose a time, confirm securely and follow the job."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = FixoomTeal
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = CircleShape,
                                color = FixoomLime.copy(alpha = 0.18f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(21.dp),
                                        tint = FixoomLime
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(11.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Protected from start to finish",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Text(
                                    text = "Approved providers • Secure booking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }

                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(21.dp),
                                tint = FixoomLime
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernBookingStep(
    visible: Boolean,
    animationDelay: Int,
    number: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconContainerColor: Color,
    title: String,
    description: String
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 320,
                delayMillis = animationDelay
            )
        ) + slideInHorizontally(
            animationSpec = tween(
                durationMillis = 360,
                delayMillis = animationDelay
            ),
            initialOffsetX = { width -> width / 7 }
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = iconContainerColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(25.dp),
                            tint = FixoomTealDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(13.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme
                                .onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingStepConnector(
    visible: Boolean,
    animationDelay: Int
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 250,
                delayMillis = animationDelay
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.28f),
                            shape = CircleShape
                        )
                )

                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun currentGreeting(): String {
    return when (
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

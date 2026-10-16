package com.fixoom.customer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fixoom.customer.R
import com.fixoom.core.model.ServiceCategory
import com.fixoom.core.model.ServiceProfessional
import com.fixoom.marketplace.ui.ProfessionalSummary
import com.fixoom.marketplace.ui.InfoCard
import com.fixoom.customer.ui.theme.FixoomLavender
import com.fixoom.customer.ui.theme.FixoomMint
import com.fixoom.customer.ui.theme.FixoomPeach
import com.fixoom.customer.ui.theme.FixoomSky
import kotlinx.coroutines.delay

private enum class ExploreFilter(
    val label: String
) {
    ALL("All services"),
    REPAIRS("Repairs"),
    FINISHING("Finishing"),
    FABRICATION("Fabrication")
}

private data class ExploreService(
    val title: String,
    val description: String,
    val iconRes: Int,
    val accent: Color,
    val filter: ExploreFilter,
    val professionalCount: Int = 0
)

private val exploreServices = listOf(
    ExploreService(
        title = "Plumbing",
        description = "Leaks, pipes and bathroom fittings",
        iconRes = R.drawable.ic_service_plumbing,
        accent = FixoomSky,
        filter = ExploreFilter.REPAIRS
    ),
    ExploreService(
        title = "Electrical",
        description = "Wiring, lighting and fault inspection",
        iconRes = R.drawable.ic_service_electrical,
        accent = FixoomPeach,
        filter = ExploreFilter.REPAIRS
    ),
    ExploreService(
        title = "Carpentry",
        description = "Furniture, doors and custom woodwork",
        iconRes = R.drawable.ic_service_carpentry,
        accent = FixoomMint,
        filter = ExploreFilter.REPAIRS
    ),
    ExploreService(
        title = "Painting",
        description = "Interior, exterior and decorative finishes",
        iconRes = R.drawable.ic_service_painting,
        accent = FixoomLavender,
        filter = ExploreFilter.FINISHING
    ),
    ExploreService(
        title = "Masonry",
        description = "Brickwork, plastering and concrete repairs",
        iconRes = R.drawable.ic_service_masonry,
        accent = Color(0xFFFFE8A3),
        filter = ExploreFilter.FINISHING
    ),
    ExploreService(
        title = "Welding",
        description = "Gates, grills and metal fabrication",
        iconRes = R.drawable.ic_service_welding,
        accent = Color(0xFFD7E1E8),
        filter = ExploreFilter.FABRICATION
    )
)

@Composable
fun ExploreScreen(
    onServiceClick: (String) -> Unit,
    categories: List<ServiceCategory> = emptyList(),
    professionals: List<ServiceProfessional> = emptyList(),
    onProviderClick: (String) -> Unit = {},
    loading: Boolean = false,
    dataError: String? = null,
    onRefresh: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedFilter by remember {
        mutableStateOf(ExploreFilter.ALL)
    }

    var screenVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        screenVisible = true
    }

    val visibleServices = remember(
        searchText,
        selectedFilter, categories, professionals
    ) {
        categories.map { category ->
            (exploreServices.firstOrNull { it.title.equals(category.name,true) }
                ?: ExploreService(category.name,category.description,R.drawable.ic_service_cleaning,FixoomMint,ExploreFilter.REPAIRS,0))
                .copy(description=category.description,professionalCount=professionals.count { it.offers(category.slug) })
        }.filter { service ->
            val matchesFilter =
                selectedFilter == ExploreFilter.ALL ||
                        service.filter == selectedFilter

            val query = searchText.trim()

            val matchesSearch =
                query.isBlank() ||
                        service.title.contains(
                            other = query,
                            ignoreCase = true
                        ) ||
                        service.description.contains(
                            other = query,
                            ignoreCase = true
                        )

            matchesFilter && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedVisibility(
            visible = screenVisible,
            enter = fadeIn(
                animationSpec = tween(420)
            ) + slideInVertically(
                animationSpec = tween(420),
                initialOffsetY = { height ->
                    height / 14
                }
            )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "explore_header") {
                    ExploreHeader(
                        searchText = searchText,
                        onSearchTextChange = {
                            searchText = it
                        },
                        onSearch = {
                            focusManager.clearFocus()

                            visibleServices
                                .firstOrNull()
                                ?.let { service ->
                                    onServiceClick(service.title)
                                }
                        }
                    )
                }

                item(key = "explore_filters") {
                    ExploreFilterRow(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { filter ->
                            selectedFilter = filter
                        }
                    )
                }

                item(key = "service_results") {
                    if(loading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal=20.dp))
                    dataError?.let { com.fixoom.marketplace.ui.InfoCard("Unable to refresh services",it,error=true){TextButton(onRefresh){Text("Try again")}} }
                    ExploreServiceResults(
                        services = visibleServices,
                        searchText = searchText,
                        selectedFilter = selectedFilter,
                        onServiceClick = onServiceClick,
                        onClearClick = {
                            searchText = ""
                            selectedFilter = ExploreFilter.ALL
                        }
                    )
                }

                item(key = "available_professionals") {
                    val visibleCategories = categories.filter { category ->
                        visibleServices.any { it.title == category.name }
                    }
                    val available = professionals.filter { provider ->
                        visibleCategories.any { provider.offers(it.slug) }
                    }
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ExploreSectionTitle(
                            title = "Available professionals",
                            subtitle = "Approved providers accepting service requests",
                        )
                        if (!loading && dataError == null && available.isEmpty()) {
                            InfoCard(
                                "No available professionals",
                                "Approved providers will appear here when they are available for your selected services.",
                            )
                        }
                        available.take(4).forEach { provider ->
                            ProfessionalSummary(
                                provider,
                                categories
                                    .filter { provider.offers(it.slug) }.joinToString(" / ") { it.name },
                            ) {
                                onProviderClick(provider.id)
                            }
                        }
                    }
                }

                item(key = "explore_information") {
                    ExploreInformationCard()
                }
            }
        }
    }
}

@Composable
private fun ExploreHeader(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            bottomStart = 34.dp,
            bottomEnd = 34.dp
        ),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                top = 24.dp,
                end = 20.dp,
                bottom = 25.dp
            )
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Explore services",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme
                    .colorScheme
                    .onPrimaryContainer,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Find the right professional for every " +
                        "job around your home.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme
                    .colorScheme
                    .onPrimaryContainer
                    .copy(alpha = 0.76f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search services"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onSearchTextChange("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor =
                        MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor =
                        MaterialTheme.colorScheme.surface,
                    focusedTextColor =
                        MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor =
                        MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    unfocusedPlaceholderColor =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    focusedLeadingIconColor =
                        MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    focusedTrailingIconColor =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    unfocusedTrailingIconColor =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    cursorColor =
                        MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun ExploreFilterRow(
    selectedFilter: ExploreFilter,
    onFilterSelected: (ExploreFilter) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Browse by type",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(
                items = ExploreFilter.entries,
                key = { filter ->
                    filter.name
                }
            ) { filter ->
                val selected = filter == selectedFilter

                FilterChip(
                    selected = selected,
                    onClick = {
                        onFilterSelected(filter)
                    },
                    label = {
                        Text(
                            text = filter.label,
                            fontWeight = if (selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            }
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector =
                                    Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface,
                        labelColor =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        selectedContainerColor =
                            MaterialTheme.colorScheme.secondary,
                        selectedLabelColor =
                            MaterialTheme.colorScheme.onSecondary,
                        selectedLeadingIconColor =
                            MaterialTheme.colorScheme.onSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme
                            .colorScheme
                            .outline
                            .copy(alpha = 0.35f),
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
private fun ExploreServiceResults(
    services: List<ExploreService>,
    searchText: String,
    selectedFilter: ExploreFilter,
    onServiceClick: (String) -> Unit,
    onClearClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .animateContentSize(
                animationSpec = tween(280)
            )
    ) {
        ExploreSectionTitle(
            title = "${services.size} ${
                if (services.size == 1) {
                    "service"
                } else {
                    "services"
                }
            }",
            subtitle = if (
                selectedFilter == ExploreFilter.ALL
            ) {
                "All services available in Fixoom"
            } else {
                selectedFilter.label
            }
        )

        Spacer(modifier = Modifier.height(15.dp))

        if (services.isEmpty()) {
            EmptyExploreResults(
                searchText = searchText,
                onClearClick = onClearClick
            )
        } else {
            ExploreServiceGrid(
                services = services,
                onServiceClick = onServiceClick
            )
        }
    }
}

@Composable
private fun ExploreServiceGrid(
    services: List<ExploreService>,
    onServiceClick: (String) -> Unit
) {
    var cardsVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        delay(80)
        cardsVisible = true
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        services
            .chunked(2)
            .forEachIndexed { rowIndex, rowServices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    rowServices.forEachIndexed {
                            columnIndex,
                            service ->

                        val animationIndex =
                            rowIndex * 2 + columnIndex

                        AnimatedVisibility(
                            visible = cardsVisible,
                            modifier = Modifier.weight(1f),
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 320,
                                    delayMillis =
                                        animationIndex * 55
                                )
                            ) + scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(
                                    durationMillis = 320,
                                    delayMillis =
                                        animationIndex * 55
                                )
                            )
                        ) {
                            ExploreServiceCard(
                                service = service,
                                modifier =
                                    Modifier.fillMaxWidth(),
                                onClick = {
                                    onServiceClick(
                                        service.title
                                    )
                                }
                            )
                        }
                    }

                    if (rowServices.size == 1) {
                        Spacer(
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
    }
}

@Composable
private fun ExploreServiceCard(
    service: ExploreService,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val pressed by
    interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) {
            0.96f
        } else {
            1f
        },
        animationSpec = tween(120),
        label = "exploreServiceScale"
    )

    val darkTheme =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    val cardShape = RoundedCornerShape(24.dp)

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "${service.title}: ${service.description}"
            },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme
                .colorScheme
                .outline
                .copy(alpha = 0.24f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 168.dp)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = service.accent.copy(
                        alpha = if (darkTheme) {
                            0.20f
                        } else {
                            0.55f
                        }
                    )
                ) {
                    Image(
                        painter = painterResource(
                            service.iconRes
                        ),
                        contentDescription = null,
                        modifier = Modifier.padding(11.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme
                        .colorScheme
                        .surfaceVariant
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons
                                .Rounded
                                .KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = service.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = service.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${service.professionalCount} professionals",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyExploreResults(
    searchText: String,
    onClearClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(19.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(27.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No services found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = if (searchText.isBlank()) {
                    "No services match this filter."
                } else {
                    "We could not match \"$searchText\". " +
                            "Try another service name."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onClearClick
            ) {
                Text(
                    text = "Show all services",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ExploreInformationCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme
            .colorScheme
            .secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(23.dp),
                tint = MaterialTheme
                    .colorScheme
                    .onSecondaryContainer
            )

            Spacer(modifier = Modifier.width(11.dp))

            Text(
                text = "Read provider profiles and service areas before requesting a service. " +
                        "Discuss the scope and price with the provider before work begins.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme
                    .colorScheme
                    .onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ExploreSectionTitle(
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

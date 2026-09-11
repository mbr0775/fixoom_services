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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkHistory
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import kotlinx.coroutines.delay

data class FixoomProvider(
    val id: Int,
    val name: String,
    val category: String,
    val profession: String,
    val rating: Double,
    val reviewCount: Int,
    val distanceKm: Double,
    val experienceYears: Int,
    val available: Boolean,
    val startingPrice: String,
    val imageRes: Int,
    val description: String
)

val fixoomProviders = listOf(
    FixoomProvider(
        id = 1,
        name = "Apex Plumbing",
        category = "Plumbing",
        profession = "Professional Plumber",
        rating = 4.9,
        reviewCount = 124,
        distanceKm = 1.2,
        experienceYears = 7,
        available = true,
        startingPrice = "From Rs. 2,500",
        imageRes = R.drawable.service_plumbing,
        description = "Specialist in pipe repairs, leaks, bathroom " +
                "fittings and emergency plumbing."
    ),
    FixoomProvider(
        id = 2,
        name = "FlowFix Services",
        category = "Plumbing",
        profession = "Plumbing Technician",
        rating = 4.7,
        reviewCount = 86,
        distanceKm = 2.4,
        experienceYears = 5,
        available = true,
        startingPrice = "From Rs. 2,000",
        imageRes = R.drawable.service_plumbing,
        description = "Reliable plumbing support for residential " +
                "repairs and installations."
    ),
    FixoomProvider(
        id = 3,
        name = "BrightFix Electrical",
        category = "Electrical",
        profession = "Certified Electrician",
        rating = 4.8,
        reviewCount = 118,
        distanceKm = 2.0,
        experienceYears = 6,
        available = true,
        startingPrice = "From Rs. 3,000",
        imageRes = R.drawable.service_electrical,
        description = "Electrical wiring, fault repairs, lighting " +
                "and home electrical installations."
    ),
    FixoomProvider(
        id = 4,
        name = "PowerPro Solutions",
        category = "Electrical",
        profession = "Electrical Technician",
        rating = 4.6,
        reviewCount = 72,
        distanceKm = 3.1,
        experienceYears = 4,
        available = false,
        startingPrice = "From Rs. 2,800",
        imageRes = R.drawable.service_electrical,
        description = "Professional electrical maintenance and " +
                "emergency fault inspection."
    ),
    FixoomProvider(
        id = 5,
        name = "CraftLine Works",
        category = "Carpentry",
        profession = "Carpenter",
        rating = 4.7,
        reviewCount = 95,
        distanceKm = 2.6,
        experienceYears = 8,
        available = true,
        startingPrice = "From Rs. 3,500",
        imageRes = R.drawable.service_carpentry,
        description = "Custom furniture, door repairs, cupboards " +
                "and general woodwork."
    ),
    FixoomProvider(
        id = 6,
        name = "WoodCraft Lanka",
        category = "Carpentry",
        profession = "Furniture Carpenter",
        rating = 4.5,
        reviewCount = 63,
        distanceKm = 4.0,
        experienceYears = 10,
        available = true,
        startingPrice = "From Rs. 4,000",
        imageRes = R.drawable.service_carpentry,
        description = "Experienced in furniture restoration, " +
                "cabinets and interior woodwork."
    ),
    FixoomProvider(
        id = 7,
        name = "Prime Painting",
        category = "Painting",
        profession = "House Painter",
        rating = 4.8,
        reviewCount = 107,
        distanceKm = 1.8,
        experienceYears = 6,
        available = true,
        startingPrice = "From Rs. 5,000",
        imageRes = R.drawable.service_painting,
        description = "Interior and exterior painting with clean " +
                "and professional finishing."
    ),
    FixoomProvider(
        id = 8,
        name = "ColourPro Decor",
        category = "Painting",
        profession = "Painting Specialist",
        rating = 4.6,
        reviewCount = 78,
        distanceKm = 3.5,
        experienceYears = 5,
        available = false,
        startingPrice = "From Rs. 4,500",
        imageRes = R.drawable.service_painting,
        description = "Wall painting, decorative finishes and " +
                "property repainting services."
    ),
    FixoomProvider(
        id = 9,
        name = "Solid Masonry",
        category = "Masonry",
        profession = "Masonry Contractor",
        rating = 4.7,
        reviewCount = 84,
        distanceKm = 2.9,
        experienceYears = 9,
        available = true,
        startingPrice = "From Rs. 5,500",
        imageRes = R.drawable.service_masonry,
        description = "Brickwork, wall repairs, plastering and " +
                "small construction projects."
    ),
    FixoomProvider(
        id = 10,
        name = "BuildRight Services",
        category = "Masonry",
        profession = "Construction Mason",
        rating = 4.5,
        reviewCount = 59,
        distanceKm = 4.4,
        experienceYears = 7,
        available = true,
        startingPrice = "From Rs. 5,000",
        imageRes = R.drawable.service_masonry,
        description = "Residential masonry repairs, cement work " +
                "and wall construction."
    ),
    FixoomProvider(
        id = 11,
        name = "SteelPro Welding",
        category = "Welding",
        profession = "Professional Welder",
        rating = 4.8,
        reviewCount = 91,
        distanceKm = 2.2,
        experienceYears = 8,
        available = true,
        startingPrice = "From Rs. 4,000",
        imageRes = R.drawable.service_masonry,
        description = "Gate repairs, steel fabrication, grills " +
                "and residential welding work."
    ),
    FixoomProvider(
        id = 12,
        name = "IronCraft Lanka",
        category = "Welding",
        profession = "Fabrication Specialist",
        rating = 4.6,
        reviewCount = 68,
        distanceKm = 3.8,
        experienceYears = 6,
        available = false,
        startingPrice = "From Rs. 3,800",
        imageRes = R.drawable.service_masonry,
        description = "Metal fabrication, railing repairs and " +
                "custom steel work."
    )
)

private enum class ProviderFilter(
    val label: String
) {
    NEAREST("Nearest"),
    TOP_RATED("Top rated"),
    AVAILABLE("Available now")
}

@Composable
fun ProviderListScreen(
    category: String,
    onBackClick: () -> Unit,
    onProviderClick: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current

    var searchText by remember(category) {
        mutableStateOf("")
    }

    var selectedFilter by remember(category) {
        mutableStateOf(ProviderFilter.NEAREST)
    }

    var screenVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        screenVisible = true
    }

    val filteredProviders = remember(
        category,
        searchText,
        selectedFilter
    ) {
        val providersInCategory = fixoomProviders.filter { provider ->
            provider.category.equals(
                other = category,
                ignoreCase = true
            )
        }

        val searchedProviders = providersInCategory.filter { provider ->
            val query = searchText.trim()

            query.isBlank() ||
                    provider.name.contains(
                        other = query,
                        ignoreCase = true
                    ) ||
                    provider.profession.contains(
                        other = query,
                        ignoreCase = true
                    ) ||
                    provider.description.contains(
                        other = query,
                        ignoreCase = true
                    )
        }

        when (selectedFilter) {
            ProviderFilter.TOP_RATED -> {
                searchedProviders.sortedWith(
                    compareByDescending<FixoomProvider> {
                        it.rating
                    }.thenByDescending {
                        it.reviewCount
                    }
                )
            }

            ProviderFilter.AVAILABLE -> {
                searchedProviders
                    .filter { provider ->
                        provider.available
                    }
                    .sortedBy { provider ->
                        provider.distanceKm
                    }
            }

            ProviderFilter.NEAREST -> {
                searchedProviders.sortedBy { provider ->
                    provider.distanceKm
                }
            }
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
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item(key = "provider_header") {
                    ProviderListHeader(
                        category = category,
                        resultCount = filteredProviders.size,
                        searchText = searchText,
                        onSearchTextChange = {
                            searchText = it
                        },
                        onSearch = {
                            focusManager.clearFocus()
                        },
                        onBackClick = onBackClick
                    )
                }

                item(key = "provider_filters") {
                    ProviderFilterRow(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { filter ->
                            selectedFilter = filter
                        }
                    )
                }

                if (filteredProviders.isEmpty()) {
                    item(key = "empty_providers") {
                        EmptyProviderState(
                            category = category,
                            searchText = searchText,
                            selectedFilter = selectedFilter,
                            onResetClick = {
                                searchText = ""
                                selectedFilter =
                                    ProviderFilter.NEAREST
                            }
                        )
                    }
                } else {
                    item(key = "results_summary") {
                        ResultsSummary(
                            resultCount = filteredProviders.size,
                            selectedFilter = selectedFilter
                        )
                    }

                    itemsIndexed(
                        items = filteredProviders,
                        key = { _, provider ->
                            provider.id
                        }
                    ) { index, provider ->
                        ProviderCardEntrance(
                            index = index,
                            provider = provider,
                            onClick = {
                                onProviderClick(provider.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderListHeader(
    category: String,
    resultCount: Int,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBackClick: () -> Unit
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
                top = 14.dp,
                end = 20.dp,
                bottom = 24.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme
                        .colorScheme
                        .onPrimaryContainer
                        .copy(alpha = 0.10f)
                ) {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Go back",
                            tint = MaterialTheme
                                .colorScheme
                                .onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$category professionals",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme
                            .colorScheme
                            .onPrimaryContainer,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "$resultCount matching professionals nearby",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                            .copy(alpha = 0.74f)
                    )
                }

                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Handyman,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            ProviderSearchField(
                value = searchText,
                onValueChange = onSearchTextChange,
                onSearch = onSearch
            )
        }
    }
}

@Composable
private fun ProviderSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Search by name or specialty"
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(
                    onClick = {
                        onValueChange("")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear provider search"
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
                MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor =
                MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor =
                MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor =
                MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor =
                MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor =
                MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun ProviderFilterRow(
    selectedFilter: ProviderFilter,
    onFilterSelected: (ProviderFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 18.dp,
            end = 20.dp,
            bottom = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            count = ProviderFilter.entries.size,
            key = { index ->
                ProviderFilter.entries[index].name
            }
        ) { index ->
            val filter = ProviderFilter.entries[index]
            val selected = selectedFilter == filter

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
                            modifier = Modifier.size(18.dp)
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
                        MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun ResultsSummary(
    resultCount: Int,
    selectedFilter: ProviderFilter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$resultCount ${
                    if (resultCount == 1) {
                        "professional"
                    } else {
                        "professionals"
                    }
                }",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Sorted by ${
                    selectedFilter.label.lowercase()
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Nearby",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ProviderCardEntrance(
    index: Int,
    provider: FixoomProvider,
    onClick: () -> Unit
) {
    var visible by remember(provider.id) {
        mutableStateOf(false)
    }

    LaunchedEffect(provider.id) {
        delay((index.coerceAtMost(6) * 55L) + 40L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(320)
        ) + scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(320)
        )
    ) {
        ModernProviderCard(
            provider = provider,
            onClick = onClick
        )
    }
}

@Composable
private fun ModernProviderCard(
    provider: FixoomProvider,
    onClick: () -> Unit
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = tween(120),
        label = "providerCardScale"
    )

    val cardShape = RoundedCornerShape(28.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 8.dp
            )
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "${provider.name}, ${provider.profession}, " +
                            "rated ${provider.rating}, " +
                            "${provider.distanceKm} kilometers away"
            },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
        Column {
            ProviderImageHeader(provider = provider)
            ProviderInformation(provider = provider)
        }
    }
}

@Composable
private fun ProviderImageHeader(
    provider: FixoomProvider
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
    ) {
        Image(
            painter = painterResource(provider.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.52f)
                        )
                    )
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            shape = RoundedCornerShape(50),
            color = if (provider.available) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme
                    .colorScheme
                    .surface
                    .copy(alpha = 0.92f)
            }
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = if (provider.available) {
                                MaterialTheme
                                    .colorScheme
                                    .onSecondary
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .tertiary
                            },
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (provider.available) {
                        "Available now"
                    } else {
                        "Currently busy"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (provider.available) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.58f)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = Color.White
                )

                Spacer(modifier = Modifier.width(3.dp))

                Text(
                    text = "${provider.distanceKm} km",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.58f)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = "Verified professional",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ProviderInformation(
    provider: FixoomProvider
) {
    Column(
        modifier = Modifier.padding(17.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = provider.profession,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(23.dp),
                        tint = MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProviderMetric(
                icon = Icons.Rounded.Star,
                iconTint = Color(0xFFF4B400),
                mainText = provider.rating.toString(),
                supportingText = "${provider.reviewCount} reviews"
            )

            Spacer(modifier = Modifier.width(18.dp))

            ProviderMetric(
                icon = Icons.Rounded.WorkHistory,
                iconTint = MaterialTheme.colorScheme.primary,
                mainText = "${provider.experienceYears} years",
                supportingText = "experience"
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    MaterialTheme
                        .colorScheme
                        .outline
                        .copy(alpha = 0.18f)
                )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Starting price",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                )

                Text(
                    text = provider.startingPrice,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = "View profile",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProviderMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    mainText: String,
    supportingText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = iconTint
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = mainText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyProviderState(
    category: String,
    searchText: String,
    selectedFilter: ProviderFilter,
    onResetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(
                horizontal = 28.dp,
                vertical = 60.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(25.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "No professionals found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(7.dp))

        val message = when {
            searchText.isNotBlank() -> {
                "No $category professionals match " +
                        "\"$searchText\". Try another name or specialty."
            }

            selectedFilter == ProviderFilter.AVAILABLE -> {
                "No $category professionals are available right " +
                        "now. Try viewing all nearby professionals."
            }

            else -> {
                "There are currently no $category professionals " +
                        "in this area."
            }
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onResetClick
        ) {
            Text(
                text = "Clear search and filters",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
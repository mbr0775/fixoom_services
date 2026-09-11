package com.fixoom.customer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ProviderDetailsScreen(
    providerId: Int,
    onBackClick: () -> Unit,
    onBookNowClick: (Int) -> Unit
) {
    val provider = remember(providerId) {
        fixoomProviders.firstOrNull { item ->
            item.id == providerId
        }
    }

    if (provider == null) {
        ProviderNotFoundScreen(
            onBackClick = onBackClick
        )
        return
    }

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
                contentPadding = PaddingValues(
                    bottom = 126.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "provider_hero") {
                    ProviderHeroSection(
                        provider = provider,
                        onBackClick = onBackClick
                    )
                }

                item(key = "provider_information") {
                    ProviderMainInformation(
                        provider = provider
                    )
                }

                item(key = "provider_statistics") {
                    ProviderStatistics(
                        provider = provider
                    )
                }

                item(key = "provider_about") {
                    ProviderAboutSection(
                        provider = provider
                    )
                }

                item(key = "provider_services") {
                    ProviderServicesSection(
                        category = provider.category
                    )
                }

                item(key = "provider_availability") {
                    ProviderAvailabilityCard(
                        provider = provider
                    )
                }

                item(key = "price_information") {
                    PriceInformationCard()
                }
            }
        }

        ProviderBookingBar(
            provider = provider,
            onBookNowClick = {
                onBookNowClick(provider.id)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ProviderHeroSection(
    provider: FixoomProvider,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Image(
            painter = painterResource(provider.imageRes),
            contentDescription = provider.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.34f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.62f)
                        )
                    )
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = 16.dp,
                    top = 16.dp
                )
                .size(48.dp),
            shape = RoundedCornerShape(17.dp),
            color = Color.Black.copy(alpha = 0.48f)
        ) {
            IconButton(
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Go back",
                    tint = Color.White
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    end = 16.dp,
                    top = 16.dp
                ),
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.50f)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 11.dp,
                    vertical = 7.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = "Profile verified",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvailabilityBadge(
                available = provider.available
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.54f)
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
                        tint = Color.White
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${provider.distanceKm} km away",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailabilityBadge(
    available: Boolean
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (available) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.surface.copy(
                alpha = 0.94f
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = if (available) {
                            MaterialTheme.colorScheme.onSecondary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = if (available) {
                    "Available now"
                } else {
                    "Booking for later"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (available) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProviderMainInformation(
    provider: FixoomProvider
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = provider.profession,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme
                    .colorScheme
                    .secondaryContainer
            ) {
                Text(
                    text = provider.category,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme
                        .colorScheme
                        .onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFF4B400)
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = provider.rating.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = " (${provider.reviewCount} reviews)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProviderStatistics(
    provider: FixoomProvider
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProviderStatisticCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.WorkHistory,
            iconTint = MaterialTheme.colorScheme.tertiary,
            value = "${provider.experienceYears}+",
            label = "Years"
        )

        ProviderStatisticCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.CheckCircle,
            iconTint = MaterialTheme.colorScheme.primary,
            value = provider.reviewCount.toString(),
            label = "Reviews"
        )

        ProviderStatisticCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Star,
            iconTint = Color(0xFFF4B400),
            value = provider.rating.toString(),
            label = "Rating"
        )
    }
}

@Composable
private fun ProviderStatisticCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(
                alpha = 0.24f
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 15.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProviderAboutSection(
    provider: FixoomProvider
) {
    DetailsCard {
        DetailsSectionTitle(
            icon = Icons.Rounded.Handyman,
            title = "About this professional"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = provider.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProviderServicesSection(
    category: String
) {
    val services = remember(category) {
        servicesForCategory(category)
    }

    DetailsCard {
        DetailsSectionTitle(
            icon = Icons.Rounded.CheckCircle,
            title = "Services offered"
        )

        Spacer(modifier = Modifier.height(14.dp))

        services.forEachIndexed { index, service ->
            ServiceListItem(service = service)

            if (index < services.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 11.dp),
                    color = MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.18f
                    )
                )
            }
        }
    }
}

@Composable
private fun ServiceListItem(
    service: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(11.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme
                        .colorScheme
                        .onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = service,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProviderAvailabilityCard(
    provider: FixoomProvider
) {
    val containerColor = if (provider.available) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }

    val contentColor = if (provider.available) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.88f
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (provider.available) {
                            Icons.Rounded.CalendarMonth
                        } else {
                            Icons.Rounded.Schedule
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (provider.available) {
                        "Ready for new bookings"
                    } else {
                        "Schedule for another time"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (provider.available) {
                        "Choose a convenient date and time to continue."
                    } else {
                        "This professional is busy now, but future " +
                                "bookings are available."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun PriceInformationCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(11.dp))

            Column {
                Text(
                    text = "Transparent booking information",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "The displayed amount is a starting " +
                            "estimate. The final price must be confirmed " +
                            "before work begins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(
                alpha = 0.22f
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun DetailsSectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(11.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ProviderBookingBar(
    provider: FixoomProvider,
    onBookNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 18.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(
                start = 20.dp,
                top = 15.dp,
                end = 20.dp,
                bottom = 15.dp
            ),
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

            Spacer(modifier = Modifier.width(14.dp))

            Button(
                onClick = onBookNowClick,
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(
                    horizontal = 20.dp,
                    vertical = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (provider.available) {
                        "Book now"
                    } else {
                        "Book a time"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun servicesForCategory(
    category: String
): List<String> {
    return when (category.lowercase()) {
        "plumbing" -> listOf(
            "Pipe and water-leak repairs",
            "Bathroom and kitchen fittings",
            "Drain cleaning and maintenance",
            "Emergency plumbing inspection"
        )

        "electrical" -> listOf(
            "Electrical wiring repairs",
            "Light and socket installation",
            "Electrical fault inspection",
            "Switchboard maintenance"
        )

        "carpentry" -> listOf(
            "Door and window repairs",
            "Cupboard and cabinet work",
            "Furniture restoration",
            "Custom woodwork"
        )

        "painting" -> listOf(
            "Interior wall painting",
            "Exterior property painting",
            "Decorative wall finishing",
            "Repainting and touch-ups"
        )

        "masonry" -> listOf(
            "Brick and block work",
            "Wall and plaster repairs",
            "Cement and concrete work",
            "Small construction projects"
        )

        "welding" -> listOf(
            "Gate and railing repairs",
            "Steel fabrication",
            "Window-grill welding",
            "Custom metalwork"
        )

        else -> listOf(
            "Home repair service",
            "Inspection and maintenance",
            "Installation support"
        )
    }
}

@Composable
private fun ProviderNotFoundScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Handyman,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Professional not found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This professional may no longer be available. " +
                    "Return to the list and select another profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onBackClick,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(
                horizontal = 22.dp,
                vertical = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Go back",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
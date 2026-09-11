package com.fixoom.customer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fixoom.customer.data.BookingRecord
import com.fixoom.customer.data.BookingStatus
import com.fixoom.customer.data.BookingStore
import com.fixoom.customer.navigation.Screen
import com.fixoom.customer.ui.theme.FixoomLime
import com.fixoom.customer.ui.theme.FixoomOrange
import com.fixoom.customer.ui.theme.FixoomRed
import com.fixoom.customer.ui.theme.FixoomTeal
import com.fixoom.customer.ui.theme.FixoomTealDark

private enum class BookingActivityTab(
    val title: String,
    val status: BookingStatus
) {
    UPCOMING(
        title = "Upcoming",
        status = BookingStatus.UPCOMING
    ),
    COMPLETED(
        title = "Completed",
        status = BookingStatus.COMPLETED
    ),
    CANCELLED(
        title = "Cancelled",
        status = BookingStatus.CANCELLED
    )
}

@Composable
fun BookingsScreen(
    navController: NavHostController
) {
    var selectedTab by remember {
        mutableStateOf(BookingActivityTab.UPCOMING)
    }
    var screenVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        screenVisible = true
    }

    val bookings = BookingStore.bookings
    val displayedBookings = bookings.filter { booking ->
        booking.status == selectedTab.status
    }

    val bookingCounts = remember(bookings) {
        BookingActivityTab.entries.associateWith { tab ->
            bookings.count { booking ->
                booking.status == tab.status
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 22.dp,
                end = 20.dp,
                bottom = 34.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "bookings_header") {
                AnimatedVisibility(
                    visible = screenVisible,
                    enter = fadeIn(tween(350)) +
                            slideInVertically(
                                animationSpec = tween(400),
                                initialOffsetY = { -it / 4 }
                            )
                ) {
                    BookingsHeader(
                        onHistoryClick = {
                            selectedTab = BookingActivityTab.COMPLETED
                        }
                    )
                }
            }

            item(key = "booking_tabs") {
                BookingActivityTabs(
                    selectedTab = selectedTab,
                    bookingCounts = bookingCounts,
                    onTabSelected = { tab ->
                        selectedTab = tab
                    }
                )
            }

            if (displayedBookings.isEmpty()) {
                item(key = "empty_${selectedTab.name}") {
                    EmptyBookingActivity(
                        selectedTab = selectedTab,
                        onExploreServices = {
                            navController.navigate(Screen.Explore.route)
                        }
                    )
                }
            } else {
                if (selectedTab == BookingActivityTab.UPCOMING) {
                    item(key = "featured_${displayedBookings.first().id}") {
                        FeaturedUpcomingBooking(
                            booking = displayedBookings.first(),
                            onViewBooking = {
                                navController.navigate(
                                    Screen.BookingDetails.createRoute(
                                        displayedBookings.first().id
                                    )
                                )
                            },
                            onMessage = {
                                navController.navigate(Screen.Messages.route)
                            }
                        )
                    }

                    if (displayedBookings.size > 1) {
                        item(key = "upcoming_heading") {
                            BookingListHeading(
                                title = "Upcoming appointments",
                                count = displayedBookings.size - 1
                            )
                        }

                        itemsIndexed(
                            items = displayedBookings.drop(1),
                            key = { _, booking -> booking.id }
                        ) { index, booking ->
                            AnimatedBookingCard(
                                booking = booking,
                                animationIndex = index,
                                onClick = {
                                    navController.navigate(
                                        Screen.BookingDetails.createRoute(
                                            booking.id
                                        )
                                    )
                                }
                            )
                        }
                    }
                } else {
                    item(key = "${selectedTab.name}_heading") {
                        BookingListHeading(
                            title = when (selectedTab) {
                                BookingActivityTab.COMPLETED ->
                                    "Completed services"

                                BookingActivityTab.CANCELLED ->
                                    "Cancelled bookings"

                                BookingActivityTab.UPCOMING ->
                                    "Upcoming appointments"
                            },
                            count = displayedBookings.size
                        )
                    }

                    itemsIndexed(
                        items = displayedBookings,
                        key = { _, booking -> booking.id }
                    ) { index, booking ->
                        AnimatedBookingCard(
                            booking = booking,
                            animationIndex = index,
                            onClick = {
                                navController.navigate(
                                    Screen.BookingDetails.createRoute(
                                        booking.id
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingsHeader(
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ACTIVITY CENTER",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "My Bookings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
        }

        Surface(
            modifier = Modifier
                .size(52.dp)
                .clickable(onClick = onHistoryClick),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 5.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Show completed bookings",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BookingActivityTabs(
    selectedTab: BookingActivityTab,
    bookingCounts: Map<BookingActivityTab, Int>,
    onTabSelected: (BookingActivityTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BookingActivityTab.entries.forEach { tab ->
                BookingTabItem(
                    modifier = Modifier.weight(1f),
                    tab = tab,
                    count = bookingCounts[tab] ?: 0,
                    selected = selectedTab == tab,
                    onClick = {
                        onTabSelected(tab)
                    }
                )
            }
        }
    }
}

@Composable
private fun BookingTabItem(
    modifier: Modifier,
    tab: BookingActivityTab,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.97f,
        animationSpec = tween(220),
        label = "booking_tab_scale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) {
            FixoomTeal
        } else {
            Color.Transparent
        },
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = 6.dp,
                vertical = 14.dp
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${tab.title} ($count)",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    FixoomLime
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FeaturedUpcomingBooking(
    booking: BookingRecord,
    onViewBooking: () -> Unit,
    onMessage: () -> Unit
) {
    val cardShape = RoundedCornerShape(30.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 7.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            FixoomTealDark,
                            FixoomTeal,
                            Color(0xFF285E50)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = FixoomLime
                    ) {
                        Text(
                            text = if (
                                booking.serviceDate.equals(
                                    other = "Today",
                                    ignoreCase = true
                                )
                            ) {
                                "TODAY"
                            } else {
                                "CONFIRMED"
                            },
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = FixoomTealDark,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "ID ${booking.reference}",
                        style = MaterialTheme.typography.labelLarge,
                        color = FixoomLime,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Image(
                            painter = painterResource(booking.imageRes),
                            contentDescription = booking.providerName,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(
                                    width = 3.dp,
                                    color = FixoomLime,
                                    shape = RoundedCornerShape(22.dp)
                                ),
                            contentScale = ContentScale.Crop
                        )

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(25.dp),
                            shape = CircleShape,
                            color = FixoomLime,
                            border = BorderStroke(
                                width = 2.dp,
                                color = FixoomTeal
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Verified provider",
                                    modifier = Modifier.size(16.dp),
                                    tint = FixoomTealDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = booking.providerName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = booking.category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.74f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(7.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = FixoomLime
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Fixoom Guarantee Covered",
                                style = MaterialTheme.typography.labelMedium,
                                color = FixoomLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = FixoomLime.copy(alpha = 0.34f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = CircleShape,
                            color = FixoomLime.copy(alpha = 0.68f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(25.dp),
                                    tint = FixoomTealDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(13.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SCHEDULED ARRIVAL",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.62f),
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = booking.serviceTime,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = FixoomLime.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = booking.serviceDate,
                                modifier = Modifier.padding(
                                    horizontal = 11.dp,
                                    vertical = 7.dp
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = FixoomLime,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onViewBooking),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 14.dp
                            ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = FixoomTealDark
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "View booking",
                                style = MaterialTheme.typography.labelLarge,
                                color = FixoomTealDark,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onMessage),
                        shape = RoundedCornerShape(18.dp),
                        color = FixoomLime
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 14.dp
                            ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubble,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = FixoomTealDark
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Message",
                                style = MaterialTheme.typography.labelLarge,
                                color = FixoomTealDark,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingListHeading(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AnimatedBookingCard(
    booking: BookingRecord,
    animationIndex: Int,
    onClick: () -> Unit
) {
    var visible by remember(booking.id) {
        mutableStateOf(false)
    }

    LaunchedEffect(booking.id) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 260,
                delayMillis = (animationIndex.coerceAtMost(5) * 45)
            )
        ) + scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(280)
        )
    ) {
        CompactBookingCard(
            booking = booking,
            onClick = onClick
        )
    }
}

@Composable
private fun CompactBookingCard(
    booking: BookingRecord,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = when (booking.status) {
                        BookingStatus.UPCOMING -> FixoomTeal
                        BookingStatus.COMPLETED ->
                            colorScheme.secondaryContainer

                        BookingStatus.CANCELLED ->
                            FixoomRed.copy(alpha = 0.10f)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (booking.status) {
                                BookingStatus.UPCOMING ->
                                    Icons.Rounded.Handyman

                                BookingStatus.COMPLETED ->
                                    Icons.Rounded.CheckCircle

                                BookingStatus.CANCELLED ->
                                    Icons.Rounded.Cancel
                            },
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = when (booking.status) {
                                BookingStatus.UPCOMING -> FixoomLime
                                BookingStatus.COMPLETED ->
                                    colorScheme.onSecondaryContainer

                                BookingStatus.CANCELLED -> FixoomRed
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(13.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.category,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Pro: ${booking.providerName} • ${booking.profession}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BookingStatusPill(status = booking.status)
            }

            Spacer(modifier = Modifier.height(15.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surfaceVariant.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 13.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(9.dp))

                    Text(
                        text = "${booking.serviceDate}, ${booking.serviceTime}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = booking.estimatedPrice,
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingStatusPill(
    status: BookingStatus
) {
    val containerColor = when (status) {
        BookingStatus.UPCOMING ->
            MaterialTheme.colorScheme.surfaceVariant

        BookingStatus.COMPLETED ->
            MaterialTheme.colorScheme.secondaryContainer

        BookingStatus.CANCELLED ->
            FixoomRed.copy(alpha = 0.10f)
    }
    val contentColor = when (status) {
        BookingStatus.UPCOMING -> MaterialTheme.colorScheme.onSurface
        BookingStatus.COMPLETED ->
            MaterialTheme.colorScheme.onSecondaryContainer

        BookingStatus.CANCELLED -> FixoomRed
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = when (status) {
                BookingStatus.UPCOMING -> "Confirmed"
                BookingStatus.COMPLETED -> "Completed"
                BookingStatus.CANCELLED -> "Cancelled"
            },
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyBookingActivity(
    selectedTab: BookingActivityTab,
    onExploreServices: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 52.dp,
                bottom = 36.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(92.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (selectedTab) {
                        BookingActivityTab.UPCOMING ->
                            Icons.Rounded.CalendarMonth

                        BookingActivityTab.COMPLETED ->
                            Icons.Rounded.CheckCircle

                        BookingActivityTab.CANCELLED ->
                            Icons.Rounded.Cancel
                    },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = when (selectedTab) {
                BookingActivityTab.UPCOMING -> "No upcoming bookings"
                BookingActivityTab.COMPLETED -> "No completed bookings"
                BookingActivityTab.CANCELLED -> "No cancelled bookings"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = when (selectedTab) {
                BookingActivityTab.UPCOMING ->
                    "Your next confirmed service will appear here."

                BookingActivityTab.COMPLETED ->
                    "Your finished services will appear here."

                BookingActivityTab.CANCELLED ->
                    "Cancelled booking records will appear here."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (selectedTab == BookingActivityTab.UPCOMING) {
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onExploreServices,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 13.dp
                )
            ) {
                Text(
                    text = "Explore services",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(7.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

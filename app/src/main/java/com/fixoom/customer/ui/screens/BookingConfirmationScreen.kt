package com.fixoom.customer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fixoom.customer.data.BookingRecord
import com.fixoom.customer.data.BookingStore

@Composable
fun BookingConfirmationScreen(
    bookingId: Long,
    onViewBookingsClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val booking = BookingStore.getBookingById(
        bookingId = bookingId
    )

    if (booking == null) {
        ConfirmationBookingNotFound(
            onHomeClick = onHomeClick
        )
        return
    }

    var contentVisible by remember(bookingId) {
        mutableStateOf(false)
    }

    LaunchedEffect(bookingId) {
        contentVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(
                animationSpec = tween(450)
            ) + slideInVertically(
                animationSpec = tween(450),
                initialOffsetY = { height ->
                    height / 12
                }
            )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 30.dp,
                    end = 20.dp,
                    bottom = 30.dp
                ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                item(key = "success_icon") {
                    ConfirmationSuccessIcon()
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item(key = "success_title") {
                    Text(
                        text = "Booking request sent",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item(key = "success_description") {
                    Text(
                        text = "Your request was submitted successfully. " +
                                "You can follow its status from My bookings.",
                        modifier = Modifier.padding(
                            horizontal = 14.dp
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item(key = "booking_reference") {
                    BookingReferenceCard(
                        reference = booking.reference
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item(key = "confirmation_details") {
                    ConfirmationDetailsCard(
                        booking = booking
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item(key = "next_steps") {
                    ConfirmationNextStepsCard()
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item(key = "confirmation_buttons") {
                    ConfirmationButtons(
                        onViewBookingsClick =
                            onViewBookingsClick,
                        onHomeClick = onHomeClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmationSuccessIcon() {
    AnimatedVisibility(
        visible = true,
        enter = scaleIn(
            initialScale = 0.70f,
            animationSpec = tween(500)
        ) + fadeIn(
            animationSpec = tween(400)
        )
    ) {
        Surface(
            modifier = Modifier.size(116.dp),
            shape = CircleShape,
            color = MaterialTheme
                .colorScheme
                .secondary
                .copy(alpha = 0.18f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Filled.CheckCircle,
                            contentDescription =
                                "Booking request sent",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme
                                .colorScheme
                                .onSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingReferenceCard(
    reference: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme
                    .colorScheme
                    .onPrimaryContainer
                    .copy(alpha = 0.11f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons
                            .AutoMirrored
                            .Filled
                            .ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Booking reference",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme
                        .colorScheme
                        .onPrimaryContainer
                        .copy(alpha = 0.72f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = reference,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme
                        .colorScheme
                        .onPrimaryContainer,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = "REQUESTED",
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ConfirmationDetailsCard(
    booking: BookingRecord
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Booking details",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConfirmationDetailRow(
                icon = Icons.Filled.Person,
                label = "Professional",
                value = booking.providerName
            )

            ConfirmationDivider()

            ConfirmationDetailRow(
                icon = Icons.Filled.CalendarMonth,
                label = "Service",
                value = booking.category
            )

            ConfirmationDivider()

            ConfirmationDetailRow(
                icon = Icons.Filled.CalendarMonth,
                label = "Date",
                value = booking.serviceDate
            )

            ConfirmationDivider()

            ConfirmationDetailRow(
                icon = Icons.Filled.Schedule,
                label = "Arrival time",
                value = booking.serviceTime
            )

            ConfirmationDivider()

            ConfirmationDetailRow(
                icon = Icons.Filled.LocationOn,
                label = "Service location",
                value = booking.serviceAddress
            )

            if (booking.attachments.isNotEmpty()) {
                ConfirmationDivider()

                ConfirmationDetailRow(
                    icon = Icons.Filled.AttachFile,
                    label = "Problem attachments",
                    value = "${booking.attachments.size} selected"
                )
            }

            ConfirmationDivider()

            ConfirmationDetailRow(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                label = "Starting estimate",
                value = booking.estimatedPrice,
                highlight = true
            )
        }
    }
}

@Composable
private fun ConfirmationDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (highlight) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (highlight) {
                    FontWeight.ExtraBold
                } else {
                    FontWeight.SemiBold
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConfirmationDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 13.dp),
        color = MaterialTheme
            .colorScheme
            .outline
            .copy(alpha = 0.18f)
    )
}

@Composable
private fun ConfirmationNextStepsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme
            .colorScheme
            .secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme
                    .colorScheme
                    .surface
                    .copy(alpha = 0.86f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme
                            .colorScheme
                            .onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "What happens next?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme
                        .colorScheme
                        .onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "The professional will review your " +
                            "request. Any final price or schedule change " +
                            "must be confirmed before the work begins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme
                        .colorScheme
                        .onSecondaryContainer
                        .copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun ConfirmationButtons(
    onViewBookingsClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onViewBookingsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    MaterialTheme.colorScheme.primary,
                contentColor =
                    MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "View my bookings",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Return to home",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ConfirmationBookingNotFound(
    onHomeClick: () -> Unit
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
            modifier = Modifier.size(82.dp),
            shape = RoundedCornerShape(27.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Booking information unavailable",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We could not find the booking information. " +
                    "Return home and check My bookings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onHomeClick,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Return home",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
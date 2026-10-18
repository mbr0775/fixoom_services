package com.fixoom.marketplace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fixoom.core.data.repository.BookingDetailState
import com.fixoom.core.model.*
import java.util.Locale

@Composable
internal fun BookingDetailSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .5f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Icon(
                        icon,
                        null,
                        Modifier.padding(9.dp).size(19.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    title,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            content()
        }
    }
}

@Composable
internal fun EvidenceCard(
    modern: Boolean,
    title: String,
    text: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (modern)
        BookingDetailSection(title, Icons.Rounded.PermMedia) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    else InfoCard(title, text, content = content)
}

internal fun customerBookingStatus(status: String): String =
    when (status) {
        "requested" -> "Awaiting reply"
        "accepted" -> "Booking accepted"
        "in_progress" -> "Service in progress"
        "completed" -> "Service completed"
        "cancelled" -> "Booking cancelled"
        "rejected" -> "Request declined"
        else -> serviceStatus(status)
    }

internal fun customerTrackingText(status: String): String =
    when (status) {
        "requested" -> "Your request is saved. Waiting for your provider to accept."
        "accepted" -> "Your provider accepted. Use Messages to coordinate your service."
        "in_progress" -> "Your provider has started the service."
        "completed" -> "Your service is complete. The history and payment record stay here."
        "cancelled" -> "This booking was cancelled. Its saved history remains available."
        "rejected" -> "Your provider declined this request. Its saved history remains available."
        else -> "Refresh to check the latest saved booking status."
    }

@Composable
internal fun CustomerBookingHero(
    booking: ServiceBooking,
    motion: Boolean,
    floating: Boolean,
    depth: () -> Float,
) {
    Box(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Box(
            Modifier.matchParentSize()
                .padding(horizontal = 8.dp)
                .offset(y = 7.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(InboxLime)
        )
        Surface(shape = RoundedCornerShape(28.dp), color = InboxTeal) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "YOUR SERVICE",
                    color = InboxLime,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val artwork: @Composable () -> Unit = {
                        CustomerUpdateArtwork(
                            booking.status,
                            InboxLime,
                            Modifier.size(94.dp),
                            motion,
                            floating,
                            depth,
                            { 0f },
                        )
                    }
                    val words: @Composable () -> Unit = {
                        Text(
                            booking.categoryName.ifBlank { serviceStatus(booking.categorySlug) },
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(shape = RoundedCornerShape(50), color = InboxLime) {
                            Text(
                                customerBookingStatus(booking.status),
                                Modifier.testTag("booking-current-status")
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                color = InboxTeal,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (maxWidth < 260.dp || LocalDensity.current.fontScale > 1.25f)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            artwork()
                            words()
                        }
                    else
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f)) { words() }
                            artwork()
                        }
                }
                Text(
                    customerTrackingText(booking.status),
                    color = Color(0xFFD3E4DF),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun CustomerBookingProgress(booking: ServiceBooking) {
    val steps = listOf("Requested", "Accepted", "In progress", "Completed")
    val current =
        listOf("requested", "accepted", "in_progress", "completed").indexOf(booking.status)
    if (current < 0) return
    BookingDetailSection("Service tracking", Icons.Rounded.Route) {
        if (LocalDensity.current.fontScale > 1.25f) {
            steps.forEachIndexed { index, label ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProgressDot(index, current)
                    Text(
                        label +
                            if (index == current) " · Current"
                            else if (index > current) " · Upcoming" else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                steps.forEachIndexed { index, label ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProgressDot(index, current)
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (index <= current) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        Text(
            "Current status from your saved booking. See History for recorded times.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressDot(index: Int, current: Int) {
    Surface(
        shape = CircleShape,
        color = if (index <= current) InboxTeal else MaterialTheme.colorScheme.surfaceVariant,
        border = if (index == current) BorderStroke(2.dp, InboxLime) else null,
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (index < current)
                Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Color.White)
            else
                Text(
                    "${index + 1}",
                    color =
                        if (index == current) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
        }
    }
}

@Composable
internal fun CustomerAppointment(booking: ServiceBooking) {
    BookingDetailSection("Your appointment", Icons.Rounded.CalendarMonth) {
        Text(serviceTime(booking.scheduledAt), style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.Timelapse,
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${booking.durationMinutes} minutes reserved",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun CustomerProfessional(booking: ServiceBooking, photo: String?, chat: () -> Unit) {
    BookingDetailSection("Your professional", Icons.Rounded.PersonOutline) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProviderPhoto(
                photo,
                booking.providerName.ifBlank { "Service provider" },
                Modifier.size(52.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    booking.providerName.ifBlank { "Service provider" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    booking.categoryName.ifBlank { serviceStatus(booking.categorySlug) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            chat,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Chat, null, Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Message provider")
        }
    }
}

@Composable
internal fun CustomerServiceDetails(booking: ServiceBooking) {
    BookingDetailSection("Service details", Icons.Rounded.Handyman) {
        DetailLabel("Service address", booking.serviceAddress, Icons.Rounded.LocationOn)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .45f))
        DetailLabel("What needs doing", booking.problemDescription, Icons.Rounded.Notes)
        Text(
            "Booked by ${booking.customerName.ifBlank { "Customer" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailLabel(label: String, value: String, icon: ImageVector) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            icon,
            null,
            Modifier.padding(top = 2.dp).size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value.ifBlank { "Not supplied" }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
internal fun CustomerBookingPayment(booking: ServiceBooking) {
    BookingDetailSection("Payment", Icons.Rounded.AccountBalanceWallet) {
        Text(
            if (booking.finalAmount == null) "Price to be agreed"
            else "${booking.currency} ${String.format(Locale.US, "%.2f", booking.finalAmount)}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (booking.finalAmount == null)
                "Agree the price with your provider before work starts. Pay the provider directly after service."
            else if (booking.directPaymentReceived) "Provider has recorded payment received."
            else "Payment has not been recorded as received.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Direct payment to your provider. Fixoom does not transfer money.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun CustomerLiveLocation(
    detail: BookingDetailState,
    mapError: String?,
    map: (JobLocation) -> Unit,
) {
    val position = detail.location
    BookingDetailSection("Provider location", Icons.Rounded.MyLocation) {
        when {
            detail.extrasLoading -> Text("Refreshing live location…")
            detail.locationError != null ->
                Text(detail.locationError.orEmpty(), color = MaterialTheme.colorScheme.error)
            position == null ->
                Text(
                    "Live location is not currently shared, or the last position has expired.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            else -> {
                Text(
                    "Last position: ${serviceTime(position.updatedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Accuracy about ${position.accuracyM.toInt()} m. This is a reported position, not an arrival estimate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    { map(position) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Rounded.Map, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View provider position on map")
                }
            }
        }
        mapError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

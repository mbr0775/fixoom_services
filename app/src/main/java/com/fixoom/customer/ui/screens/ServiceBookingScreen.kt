package com.fixoom.customer.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixoom.customer.data.BookingAttachment
import com.fixoom.customer.data.BookingAttachmentType
import com.fixoom.customer.data.BookingStore

private const val MAX_PROBLEM_ATTACHMENTS = 5

@Composable
fun ServiceBookingScreen(
    providerId: Int,
    onBackClick: () -> Unit,
    onBookingConfirmed: (bookingId: Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val provider = remember(providerId) {
        fixoomProviders.firstOrNull { item ->
            item.id == providerId
        }
    }

    var selectedDate by rememberSaveable {
        mutableStateOf("")
    }

    var selectedTime by rememberSaveable {
        mutableStateOf("")
    }

    var serviceAddress by rememberSaveable {
        mutableStateOf("")
    }

    var problemDescription by rememberSaveable {
        mutableStateOf("")
    }

    var isSubmitting by rememberSaveable {
        mutableStateOf(false)
    }

    val selectedAttachments = remember {
        mutableStateListOf<BookingAttachment>()
    }

    var attachmentMessage by remember {
        mutableStateOf<String?>(null)
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = MAX_PROBLEM_ATTACHMENTS
        )
    ) { selectedUris ->
        val remainingSlots =
            MAX_PROBLEM_ATTACHMENTS - selectedAttachments.size

        var unsupportedMediaFound = false

        selectedUris
            .take(remainingSlots)
            .forEach { uri ->
                val alreadySelected =
                    selectedAttachments.any { attachment ->
                        attachment.uri == uri.toString()
                    }

                if (!alreadySelected) {
                    createBookingAttachment(
                        context = context,
                        uri = uri
                    )?.let { attachment ->
                        persistMediaReadAccess(
                            context = context,
                            uri = uri
                        )

                        selectedAttachments.add(attachment)
                    } ?: run {
                        unsupportedMediaFound = true
                    }
                }
            }

        attachmentMessage = when {
            selectedUris.size > remainingSlots -> {
                "Only $MAX_PROBLEM_ATTACHMENTS attachments are allowed."
            }

            unsupportedMediaFound -> {
                "Some selected files were not supported."
            }

            selectedUris.isNotEmpty() -> {
                "${selectedAttachments.size} of " +
                        "$MAX_PROBLEM_ATTACHMENTS attachments selected."
            }

            else -> null
        }
    }

    if (provider == null) {
        BookingProviderNotFound(
            onBackClick = onBackClick
        )
        return
    }

    val bookingIsValid =
        selectedDate.isNotBlank() &&
                selectedTime.isNotBlank() &&
                serviceAddress.isNotBlank() &&
                problemDescription.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            bottom = 30.dp
        )
    ) {
        item(key = "booking_header") {
            BookingHeader(
                onBackClick = onBackClick
            )
        }

        item(key = "provider_card") {
            BookingProviderCard(
                provider = provider
            )
        }

        item(key = "date_title") {
            BookingSectionTitle(
                title = "Select service date",
                subtitle = "Choose when you need the professional"
            )
        }

        item(key = "date_selection") {
            DateSelection(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    selectedDate = date
                }
            )
        }

        item(key = "time_title") {
            BookingSectionTitle(
                title = "Select arrival time",
                subtitle = "The professional will arrive within " +
                        "the selected period"
            )
        }

        item(key = "time_selection") {
            TimeSelection(
                selectedTime = selectedTime,
                onTimeSelected = { time ->
                    selectedTime = time
                }
            )
        }

        item(key = "location_title") {
            BookingSectionTitle(
                title = "Service location",
                subtitle = "Enter the address where the work is required"
            )
        }

        item(key = "location_field") {
            ModernBookingTextField(
                value = serviceAddress,
                onValueChange = { address ->
                    serviceAddress = address
                },
                title = "Address",
                placeholder = "House number, street and city",
                icon = Icons.Filled.LocationOn,
                minLines = 1,
                maxLines = 2
            )
        }

        item(key = "problem_title") {
            BookingSectionTitle(
                title = "Describe the problem",
                subtitle = "Help the professional understand the issue"
            )
        }

        item(key = "problem_field") {
            ModernBookingTextField(
                value = problemDescription,
                onValueChange = { description ->
                    if (description.length <= 500) {
                        problemDescription = description
                    }
                },
                title = "Problem description",
                placeholder = "Example: The kitchen pipe is leaking " +
                        "under the sink.",
                icon = Icons.Filled.WorkOutline,
                minLines = 4,
                maxLines = 7,
                supportingText = "${problemDescription.length}/500"
            )
        }

        item(key = "attachment_title") {
            BookingSectionTitle(
                title = "Add photos or videos",
                subtitle = "Optional: show the problem clearly"
            )
        }

        item(key = "attachment_picker") {
            ProblemAttachmentPicker(
                attachments = selectedAttachments,
                message = attachmentMessage,
                onAddClick = {
                    attachmentMessage = null

                    mediaPicker.launch(
                        PickVisualMediaRequest(
                            mediaType = ActivityResultContracts
                                .PickVisualMedia
                                .ImageAndVideo
                        )
                    )
                },
                onRemoveClick = { attachment ->
                    selectedAttachments.remove(attachment)

                    attachmentMessage =
                        if (selectedAttachments.isEmpty()) {
                            null
                        } else {
                            "${selectedAttachments.size} of " +
                                    "$MAX_PROBLEM_ATTACHMENTS " +
                                    "attachments selected."
                        }
                }
            )
        }

        item(key = "booking_summary") {
            BookingSummaryCard(
                provider = provider,
                selectedDate = selectedDate,
                selectedTime = selectedTime,
                attachmentCount = selectedAttachments.size
            )
        }

        item(key = "confirm_booking") {
            Button(
                onClick = {
                    if (!bookingIsValid || isSubmitting) {
                        return@Button
                    }

                    isSubmitting = true

                    val newBooking = BookingStore.addBooking(
                        providerId = provider.id,
                        providerName = provider.name,
                        profession = provider.profession,
                        category = provider.category,
                        imageRes = provider.imageRes,
                        serviceDate = selectedDate,
                        serviceTime = selectedTime,
                        serviceAddress = serviceAddress,
                        problemDescription = problemDescription,
                        estimatedPrice = provider.startingPrice,
                        attachments = selectedAttachments.toList()
                    )

                    onBookingConfirmed(newBooking.id)
                },
                enabled = bookingIsValid && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 20.dp
                    )
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor =
                        MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isSubmitting) {
                        "Confirming..."
                    } else {
                        "Confirm booking"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BookingHeader(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                start = 8.dp,
                end = 20.dp,
                top = 10.dp,
                bottom = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Column {
            Text(
                text = "Book service",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Complete your booking details",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BookingProviderCard(
    provider: FixoomProvider
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 18.dp
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(
                alpha = 0.25f
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(provider.imageRes),
                contentDescription = provider.name,
                modifier = Modifier
                    .size(82.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = provider.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = provider.profession,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFF4B400)
                    )

                    Text(
                        text = provider.rating.toString(),
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )

                    Text(
                        text = "${provider.distanceKm} km",
                        modifier = Modifier.padding(start = 3.dp),
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = provider.startingPrice,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BookingSectionTitle(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 10.dp
            )
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = subtitle,
            modifier = Modifier.padding(top = 3.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ModernBookingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    title: String,
    placeholder: String,
    icon: ImageVector,
    minLines: Int,
    maxLines: Int,
    supportingText: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.12f
                )
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

            Spacer(modifier = Modifier.width(11.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder
                )
            },
            supportingText = if (supportingText != null) {
                {
                    Text(
                        text = supportingText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            } else {
                null
            },
            minLines = minLines,
            maxLines = maxLines,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
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
                focusedBorderColor =
                    MaterialTheme.colorScheme.primary,
                unfocusedBorderColor =
                    MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.45f
                    ),
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun ProblemAttachmentPicker(
    attachments: List<BookingAttachment>,
    message: String?,
    onAddClick: () -> Unit,
    onRemoveClick: (BookingAttachment) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.35f
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(17.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.12f
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(27.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Show the problem clearly",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Select photos or short videos from your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onAddClick,
                    enabled =
                        attachments.size < MAX_PROBLEM_ATTACHMENTS,
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = if (attachments.isEmpty()) {
                            "Select media"
                        } else {
                            "Add more"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Maximum $MAX_PROBLEM_ATTACHMENTS attachments",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (attachments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                attachments.forEach { attachment ->
                    AttachmentRow(
                        attachment = attachment,
                        onRemoveClick = {
                            onRemoveClick(attachment)
                        }
                    )
                }
            }
        }

        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AttachmentRow(
    attachment: BookingAttachment,
    onRemoveClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(
                start = 12.dp,
                top = 10.dp,
                end = 6.dp,
                bottom = 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            if (
                                attachment.type ==
                                BookingAttachmentType.IMAGE
                            ) {
                                Icons.Filled.PhotoLibrary
                            } else {
                                Icons.Filled.VideoLibrary
                            },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = attachment.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (
                        attachment.type ==
                        BookingAttachmentType.IMAGE
                    ) {
                        "Problem photo"
                    } else {
                        "Problem video"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onRemoveClick
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription =
                        "Remove ${attachment.displayName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DateSelection(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val dates = listOf(
        "Today",
        "Tomorrow",
        "This weekend",
        "Next Monday"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(dates) { date ->
            SelectionChip(
                text = date,
                selected = selectedDate == date,
                icon = Icons.Filled.CalendarMonth,
                onClick = {
                    onDateSelected(date)
                }
            )
        }
    }
}

@Composable
private fun TimeSelection(
    selectedTime: String,
    onTimeSelected: (String) -> Unit
) {
    val times = listOf(
        "8:00 AM",
        "10:00 AM",
        "1:00 PM",
        "3:00 PM",
        "5:00 PM"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(times) { time ->
            SelectionChip(
                text = time,
                selected = selectedTime == time,
                icon = Icons.Filled.Schedule,
                onClick = {
                    onTimeSelected(time)
                }
            )
        }
    }
}

@Composable
private fun SelectionChip(
    text: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(
            onClick = onClick
        ),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (selected) {
            null
        } else {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.35f
                )
            )
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 13.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = text,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BookingSummaryCard(
    provider: FixoomProvider,
    selectedDate: String,
    selectedTime: String,
    attachmentCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 20.dp
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(
                alpha = 0.25f
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Booking summary",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            SummaryRow(
                label = "Professional",
                value = provider.name
            )

            SummaryDivider()

            SummaryRow(
                label = "Service",
                value = provider.category
            )

            SummaryDivider()

            SummaryRow(
                label = "Date",
                value = selectedDate.ifBlank {
                    "Not selected"
                }
            )

            SummaryDivider()

            SummaryRow(
                label = "Arrival time",
                value = selectedTime.ifBlank {
                    "Not selected"
                }
            )

            SummaryDivider()

            SummaryRow(
                label = "Attachments",
                value = if (attachmentCount == 0) {
                    "None"
                } else {
                    "$attachmentCount selected"
                }
            )

            SummaryDivider()

            SummaryRow(
                label = "Estimated price",
                value = provider.startingPrice,
                highlight = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme
                    .colorScheme
                    .secondaryContainer
            ) {
                Text(
                    text = "The final price must be confirmed before " +
                            "the service begins.",
                    modifier = Modifier.padding(13.dp),
                    color = MaterialTheme
                        .colorScheme
                        .onSecondaryContainer,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SummaryDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 11.dp),
        color = MaterialTheme.colorScheme.outline.copy(
            alpha = 0.20f
        )
    )
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )

        Text(
            text = value,
            color = if (highlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 13.sp,
            fontWeight = if (highlight) {
                FontWeight.Bold
            } else {
                FontWeight.SemiBold
            },
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BookingProviderNotFound(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkOutline,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Professional not found",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This professional may no longer be available.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onBackClick,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Go back",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun createBookingAttachment(
    context: Context,
    uri: Uri
): BookingAttachment? {
    val mimeType =
        context.contentResolver.getType(uri).orEmpty()

    val attachmentType = when {
        mimeType.startsWith("image/") -> {
            BookingAttachmentType.IMAGE
        }

        mimeType.startsWith("video/") -> {
            BookingAttachmentType.VIDEO
        }

        else -> return null
    }

    val fallbackName =
        if (attachmentType == BookingAttachmentType.IMAGE) {
            "Problem photo"
        } else {
            "Problem video"
        }

    return BookingAttachment(
        uri = uri.toString(),
        type = attachmentType,
        displayName = getMediaDisplayName(
            context = context,
            uri = uri
        ) ?: fallbackName
    )
}

private fun getMediaDisplayName(
    context: Context,
    uri: Uri
): String? {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameColumn =
                cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

            if (
                nameColumn >= 0 &&
                cursor.moveToFirst()
            ) {
                cursor.getString(nameColumn)
            } else {
                null
            }
        }
    }.getOrNull()
}

private fun persistMediaReadAccess(
    context: Context,
    uri: Uri
) {
    runCatching {
        context.contentResolver
            .takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
    }
}
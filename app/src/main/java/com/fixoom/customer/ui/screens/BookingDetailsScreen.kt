package com.fixoom.customer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixoom.customer.data.BookingStatus
import com.fixoom.customer.data.BookingStore
import com.fixoom.customer.ui.theme.FixoomBackground
import com.fixoom.customer.ui.theme.FixoomBorder
import com.fixoom.customer.ui.theme.FixoomGreen
import com.fixoom.customer.ui.theme.FixoomGreenDark
import com.fixoom.customer.ui.theme.FixoomOrange
import com.fixoom.customer.ui.theme.FixoomSecondaryText
import com.fixoom.customer.ui.theme.FixoomSoftGreen
import com.fixoom.customer.ui.theme.FixoomText


@Composable
fun BookingDetailsScreen(
    bookingId: Long,
    onBackClick: () -> Unit,
    onCancelClick: () -> Unit
) {

    val booking = remember(bookingId) {
        BookingStore.getBookingById(
            bookingId
        )
    }


    if (booking == null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FixoomBackground),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Booking not found",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FixoomText
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = onBackClick
            ) {
                Text(
                    text = "Go back"
                )
            }
        }

        return
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FixoomBackground)
    ) {


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
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
                    contentDescription = null
                )
            }


            Text(
                text = "Booking details",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = FixoomText
            )
        }



        Column(
            modifier = Modifier.padding(
                20.dp
            )
        ) {


            StatusCard(
                status = booking.status
            )


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            DetailCard(
                title = "Service provider"
            ) {

                DetailRow(
                    Icons.Filled.Work,
                    booking.providerName
                )

                DetailRow(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    booking.category
                )

                DetailRow(
                    Icons.Filled.CheckCircle,
                    booking.profession
                )
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            DetailCard(
                title = "Appointment"
            ) {

                DetailRow(
                    Icons.Filled.CalendarMonth,
                    booking.serviceDate
                )

                DetailRow(
                    Icons.Filled.Schedule,
                    booking.serviceTime
                )

                DetailRow(
                    Icons.Filled.LocationOn,
                    booking.serviceAddress
                )
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            DetailCard(
                title = "Problem description"
            ) {

                Text(
                    text = booking.problemDescription,
                    color = FixoomSecondaryText,
                    fontSize = 14.sp
                )
            }


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            Text(
                text = booking.estimatedPrice,
                color = FixoomGreenDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(15.dp)
            )


            if (
                booking.status == BookingStatus.UPCOMING
            ) {

                Button(
                    onClick = onCancelClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FixoomOrange
                    )
                ) {

                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )


                    Text(
                        text = "Cancel booking",
                        fontWeight = FontWeight.Bold
                    )
                }

            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            OutlinedButton(
                onClick = {

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    FixoomGreen
                )
            ) {

                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = FixoomGreen
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )


                Text(
                    text = "Contact provider",
                    color = FixoomGreen
                )
            }
        }
    }
}



@Composable
private fun StatusCard(
    status: BookingStatus
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = when(status){

            BookingStatus.UPCOMING ->
                FixoomSoftGreen

            BookingStatus.COMPLETED ->
                Color(0xFFE8F8EE)

            BookingStatus.CANCELLED ->
                Color(0xFFFFF0E8)
        }
    ){

        Text(
            text = when(status){

                BookingStatus.UPCOMING ->
                    "Booking confirmed"

                BookingStatus.COMPLETED ->
                    "Service completed"

                BookingStatus.CANCELLED ->
                    "Booking cancelled"
            },
            modifier = Modifier.padding(
                18.dp
            ),
            color = when(status){

                BookingStatus.UPCOMING ->
                    FixoomGreenDark

                BookingStatus.COMPLETED ->
                    FixoomGreenDark

                BookingStatus.CANCELLED ->
                    FixoomOrange
            },
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}



@Composable
private fun DetailCard(
    title:String,
    content:@Composable () -> Unit
){

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            1.dp,
            FixoomBorder
        )
    ){

        Column(
            modifier = Modifier.padding(
                18.dp
            )
        ){

            Text(
                text = title,
                color = FixoomText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            content()
        }
    }
}



@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text:String
){

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 7.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ){

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FixoomGreen,
            modifier = Modifier.size(20.dp)
        )


        Spacer(
            modifier = Modifier.width(12.dp)
        )


        Text(
            text = text,
            color = FixoomSecondaryText,
            fontSize = 14.sp
        )
    }
}
package com.fixoom.customer.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.fixoom.customer.R
import com.fixoom.customer.ui.theme.FixoomBackground
import com.fixoom.customer.ui.theme.FixoomGreen
import com.fixoom.customer.ui.theme.FixoomGreenDark
import com.fixoom.customer.ui.theme.FixoomOrange
import com.fixoom.customer.ui.theme.FixoomSecondaryText
import com.fixoom.customer.ui.theme.FixoomSoftGreen
import com.fixoom.customer.ui.theme.FixoomText


@Composable
fun FeedScreen(
    onServiceClick: (String) -> Unit
) {


    LazyColumn(

        modifier = Modifier
            .fillMaxSize()
            .background(
                FixoomBackground
            ),

        contentPadding = PaddingValues(
            bottom = 30.dp
        )

    ) {



        item {

            FeedHeader()

        }



        item {

            FeedSearch()

        }



        item {

            ServiceStories(
                onServiceClick = onServiceClick
            )

        }



        item {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

        }



        item {

            PromotionalCard()

        }



        item {

            SectionTitle(
                title = "Nearby professionals"
            )

        }



        item {

            ProfessionalPost()

        }



        item {

            SectionTitle(
                title = "Home care tips"
            )

        }



        item {

            TipsPost()

        }

    }

}







@Composable
private fun FeedHeader(){

    Column(

        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White
            )
            .padding(
                20.dp
            )

    ){

        Text(

            text = "Fixoom Feed",

            fontSize = 27.sp,

            fontWeight = FontWeight.Bold,

            color = FixoomText

        )


        Spacer(
            modifier = Modifier.height(5.dp)
        )


        Text(

            text = "Discover services, offers and professionals near you",

            fontSize = 13.sp,

            color = FixoomSecondaryText

        )


    }

}







@Composable
private fun FeedSearch(){

    OutlinedTextField(

        value = "",

        onValueChange = {},


        modifier = Modifier
            .fillMaxWidth()
            .padding(
                20.dp
            ),


        placeholder = {

            Text(
                text = "What service do you need?"
            )

        },


        leadingIcon = {

            Icon(

                imageVector = Icons.Filled.Search,

                contentDescription = null,

                tint = FixoomGreen

            )

        },


        shape = RoundedCornerShape(
            18.dp
        ),


        colors = TextFieldDefaults.colors(

            focusedContainerColor = Color.White,

            unfocusedContainerColor = Color.White,

            focusedIndicatorColor = Color.Transparent,

            unfocusedIndicatorColor = Color.Transparent

        )

    )

}








private data class ServiceStory(

    val name:String,

    val icon:Int

)



private val serviceStories = listOf(

    ServiceStory(
        "Plumbing",
        R.drawable.service_plumbing
    ),

    ServiceStory(
        "Electrical",
        R.drawable.service_electrical
    ),

    ServiceStory(
        "Painting",
        R.drawable.service_painting
    ),

    ServiceStory(
        "Carpentry",
        R.drawable.service_carpentry
    )

)






@Composable
private fun ServiceStories(

    onServiceClick:(String)->Unit

){

    Column(

        modifier = Modifier.padding(
            top = 5.dp
        )

    ){

        Text(

            text = "Services",

            modifier = Modifier.padding(
                horizontal = 20.dp
            ),

            fontSize = 18.sp,

            fontWeight = FontWeight.Bold,

            color = FixoomText

        )



        Spacer(
            modifier = Modifier.height(12.dp)
        )



        LazyRow(

            contentPadding = PaddingValues(
                horizontal = 20.dp
            ),

            horizontalArrangement = Arrangement.spacedBy(
                15.dp
            )

        ){


            items(serviceStories){ service ->


                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally,


                    modifier = Modifier.clickable {

                        onServiceClick(
                            service.name
                        )

                    }

                ){


                    Surface(

                        modifier =
                            Modifier.size(
                                72.dp
                            ),

                        shape = CircleShape,

                        color = FixoomSoftGreen

                    ){


                        Image(

                            painter =
                                painterResource(
                                    service.icon
                                ),

                            contentDescription = null,

                            contentScale =
                                ContentScale.Crop

                        )

                    }



                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )



                    Text(

                        text = service.name,

                        fontSize = 11.sp,

                        color = FixoomText,

                        fontWeight = FontWeight.SemiBold

                    )


                }

            }


        }

    }

}








@Composable
private fun PromotionalCard(){


    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                20.dp
            ),

        shape = RoundedCornerShape(
            24.dp
        ),

        colors = CardDefaults.cardColors(

            containerColor = FixoomGreen

        )

    ){


        Column(

            modifier = Modifier.padding(
                20.dp
            )

        ){


            Text(

                text = "Special offer 🔥",

                color = Color.White,

                fontSize = 14.sp

            )



            Spacer(
                modifier = Modifier.height(8.dp)
            )



            Text(

                text = "20% OFF your first home service",

                color = Color.White,

                fontSize = 22.sp,

                fontWeight = FontWeight.Bold

            )



            Spacer(
                modifier = Modifier.height(8.dp)
            )



            Text(

                text = "Book trusted professionals today",

                color = Color.White.copy(
                    alpha = 0.8f
                ),

                fontSize = 13.sp

            )

        }


    }


}








@Composable
private fun SectionTitle(
    title:String
){

    Text(

        text = title,

        modifier = Modifier.padding(
            horizontal = 20.dp,
            vertical = 5.dp
        ),

        fontSize = 19.sp,

        fontWeight = FontWeight.Bold,

        color = FixoomText

    )

}








@Composable
private fun ProfessionalPost(){


    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 10.dp
            ),

        shape = RoundedCornerShape(
            22.dp
        ),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )

    ){


        Column(

            modifier = Modifier.padding(
                16.dp
            )

        ){



            Row(

                verticalAlignment =
                    Alignment.CenterVertically

            ){


                Surface(

                    modifier =
                        Modifier.size(
                            50.dp
                        ),

                    shape = CircleShape,

                    color = FixoomSoftGreen

                ){

                    Icon(

                        imageVector =
                            Icons.Filled.Work,

                        contentDescription = null,

                        tint = FixoomGreen

                    )

                }




                Column(

                    modifier = Modifier.padding(
                        start = 12.dp
                    )

                ){

                    Text(

                        text = "Apex Plumbing",

                        fontWeight = FontWeight.Bold,

                        color = FixoomText

                    )


                    Row{

                        Icon(

                            imageVector =
                                Icons.Filled.Star,

                            contentDescription = null,

                            modifier =
                                Modifier.size(
                                    16.dp
                                ),

                            tint = FixoomOrange

                        )


                        Text(

                            text = " 4.9 • Available now",

                            color = FixoomSecondaryText,

                            fontSize = 12.sp

                        )

                    }

                }

            }



            Spacer(
                modifier = Modifier.height(15.dp)
            )



            Text(

                text = "Professional plumbing service available in your area. Fast response and trusted technicians.",

                color = FixoomSecondaryText,

                fontSize = 14.sp

            )



            Spacer(
                modifier = Modifier.height(15.dp)
            )


            Row(

                horizontalArrangement =
                    Arrangement.spacedBy(
                        20.dp
                    )

            ){


                Icon(
                    Icons.Filled.ThumbUp,
                    null,
                    tint = FixoomGreen
                )


                Icon(
                    Icons.Filled.FavoriteBorder,
                    null,
                    tint = FixoomOrange
                )


                Icon(
                    Icons.Filled.Share,
                    null,
                    tint = FixoomSecondaryText
                )


            }


        }


    }


}








@Composable
private fun TipsPost(){


    Card(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 10.dp
            ),

        shape = RoundedCornerShape(
            22.dp
        ),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )

    ){


        Column(

            modifier = Modifier.padding(
                18.dp
            )

        ){


            Text(

                text = "🏠 Home maintenance tip",

                color = FixoomGreenDark,

                fontWeight = FontWeight.Bold

            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(

                text = "Regularly checking pipes and electrical systems can prevent expensive repairs.",

                color = FixoomSecondaryText,

                fontSize = 14.sp,

                lineHeight = 20.sp

            )


        }


    }


}
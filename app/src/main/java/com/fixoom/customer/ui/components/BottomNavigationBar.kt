package com.fixoom.customer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fixoom.customer.navigation.Screen

private data class BottomDestination(
    val label: String,
    val route: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination(
        label = "Home",
        route = Screen.Home.route,
        icon = Icons.Rounded.Home
    ),
    BottomDestination(
        label = "Explore",
        route = Screen.Explore.route,
        icon = Icons.Rounded.Explore
    ),
    BottomDestination(
        label = "Bookings",
        route = Screen.Bookings.route,
        icon = Icons.Rounded.CalendarMonth
    ),
    BottomDestination(
        label = "Messages",
        route = Screen.Messages.route,
        icon = Icons.Rounded.ChatBubble
    ),
    BottomDestination(
        label = "Profile",
        route = Screen.Profile.route,
        icon = Icons.Rounded.Person
    )
)

@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {
    val backStackEntry by
    navController.currentBackStackEntryAsState()

    val currentRoute =
        backStackEntry?.destination?.route

    val topLevelRoutes =
        bottomDestinations.map { destination ->
            destination.route
        }

    if (currentRoute !in topLevelRoutes) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 10.dp
            )
            .height(94.dp)
    ) {
        NavigationBackground()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.Bottom
        ) {
            bottomDestinations.forEach { destination ->
                val selected =
                    currentRoute == destination.route

                val interactionSource =
                    remember(destination.route) {
                        MutableInteractionSource()
                    }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            interactionSource =
                                interactionSource,
                            indication = null,
                            enabled = true,
                            role = Role.Tab,
                            onClick = {
                                navigateToDestination(
                                    navController =
                                        navController,
                                    destination =
                                        destination,
                                    selected = selected
                                )
                            }
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (selected) {
                        SelectedNavigationItem(
                            destination = destination
                        )
                    } else {
                        UnselectedNavigationItem(
                            destination = destination
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.NavigationBackground() {
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp
    ) {}
}

@Composable
private fun SelectedNavigationItem(
    destination: BottomDestination
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .background(
                    color = MaterialTheme
                        .colorScheme
                        .secondary
                        .copy(alpha = 0.18f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                shadowElevation = 10.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        modifier = Modifier.size(27.dp),
                        tint = MaterialTheme
                            .colorScheme
                            .onSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UnselectedNavigationItem(
    destination: BottomDestination
) {
    Column(
        modifier = Modifier.padding(bottom = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme
                .colorScheme
                .onPrimaryContainer
                .copy(alpha = 0.72f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme
                .colorScheme
                .onPrimaryContainer
                .copy(alpha = 0.72f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun navigateToDestination(
    navController: NavHostController,
    destination: BottomDestination,
    selected: Boolean
) {
    if (selected) {
        return
    }

    navController.navigate(destination.route) {
        popUpTo(Screen.Home.route) {
            saveState = true
        }

        launchSingleTop = true
        restoreState = true
    }
}
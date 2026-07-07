package com.example.commingsoon.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavScreens (
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    object Home : NavScreens("home", "Home", icon = Icons.Default.Home)
    object Journey : NavScreens("journeys", "Journeys", icon = Icons.Default.Flight)
    object OpenGuesser : NavScreens("openguesser","Play OpenGuesser", icon = Icons.Default.PlayArrow)
    object Friends : NavScreens("friends","Friends", icon = Icons.Default.Groups)
    object AddFriend : NavScreens("addfriend", "Add Friend")
    object FriendDetail : NavScreens("friend/{friendId}", "Friend")
    object Settings : NavScreens("settings","App Settings", icon = Icons.Default.Settings)
    object Profile : NavScreens("profile","Profile", icon = Icons.Default.Person)
}
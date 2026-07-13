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
    object JourneyEditor : NavScreens("journeyeditor/{journeyId}", "Journey Editor") {
        fun createRoute(journeyId: Int? = null): String {
            return if (journeyId == null)
                "journeyeditor/-1"
            else
                "journeyeditor/$journeyId"
        }
    }
    object JourneyDetail : NavScreens("journey/{journeyId}", "Journey") {
        fun createRoute(jouneyId: Int): String {
            return "journey/$jouneyId"
        }
    }
    object OpenGuesser : NavScreens("openguesser","Play OpenGuesser", icon = Icons.Default.PlayArrow)
    object OpenGuesserOnline : NavScreens("openguesser/online", "Online OpenGuesser")
    object OpenGuesserLocal : NavScreens("openguesser/local", "Local OpenGuesser")
    object OpenGuesserLocalMap : NavScreens("openguesser/local/map", "Local OpenGuesser Map")
    object Friends : NavScreens("friends","Friends", icon = Icons.Default.Groups)
    object FriendDetail : NavScreens("friend/{friendId}", "Friend") {
        fun createRoute(friendId: Int): String {
            return "friend/$friendId"
        }
    }
    object LiveLocations : NavScreens("livelocations", "Live Locations")
    object Settings : NavScreens("settings","App Settings", icon = Icons.Default.Settings)
    object Profile : NavScreens("profile","Profile", icon = Icons.Default.Person)
    object ProfileEditor : NavScreens("profileeditor", "Profile Editor")
}

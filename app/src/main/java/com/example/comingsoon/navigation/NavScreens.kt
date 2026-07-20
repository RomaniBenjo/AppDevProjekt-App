package com.example.comingsoon.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.comingsoon.R

sealed class NavScreens (
    val route: String,
    @StringRes val title: Int,
    val icon: ImageVector? = null
) {
    object Login : NavScreens("login", R.string.login)
    object Home : NavScreens("home", R.string.home, icon = Icons.Default.Home)
    object Journey : NavScreens("journeys", R.string.journeys, icon = Icons.Default.Flight)
    object JourneyEditor : NavScreens("journeyeditor/{journeyId}", R.string.journey_editor) {
        fun createRoute(journeyId: Int? = null): String {
            return if (journeyId == null) {
                "journeyeditor/-1"
            } else {
                "journeyeditor/$journeyId"
            }
        }
    }
    object JourneyDetail : NavScreens("journey/{journeyId}", R.string.journey_detail) {
        fun createRoute(jouneyId: Int): String {
            return "journey/$jouneyId"
        }
    }
    object OpenGuesser : NavScreens("openguesser",R.string.play_guesser, icon = Icons.Default.PlayArrow)
    object OpenGuesserOnline : NavScreens("openguesser/online", R.string.guesser_online)
    object OpenGuesserLocal : NavScreens("openguesser/local", R.string.guesser_local)
    object OpenGuesserLocalLobby : NavScreens("openguesser/local/lobby", R.string.guesser_connect)
    object Friends : NavScreens("friends",R.string.friends, icon = Icons.Default.Groups)
    object FriendDetail : NavScreens("friend/{friendId}", R.string.friend_detail) {
        fun createRoute(friendId: Int): String {
            return "friend/$friendId"
        }
    }
    object LiveLocations : NavScreens("livelocations", R.string.live_location)
    object Settings : NavScreens("settings",R.string.settings, icon = Icons.Default.Settings)
    object Profile : NavScreens("profile",R.string.profile, icon = Icons.Default.Person)
    object ProfileEditor : NavScreens("profileeditor", R.string.profile_editor)
}

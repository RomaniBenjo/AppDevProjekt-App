package com.example.commingsoon.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.commingsoon.ui.screens.HomeScreen
import com.example.commingsoon.ui.screens.OpenGuesserScreen
import com.example.commingsoon.ui.screens.SettingsScreen

@Composable
fun AppNavHost (
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = NavScreens.Home.route
    ) {
        composable(NavScreens.Home.route) {
            HomeScreen()
        }

        composable(NavScreens.Journey.route) {
            //JourneyScreen()
        }

        composable(NavScreens.OpenGuesser.route) {
            OpenGuesserScreen()
        }

        composable(NavScreens.Friends.route) {
            //FriendsScreen()
        }

        composable(NavScreens.Settings.route) {
            SettingsScreen()
        }

        composable(NavScreens.Profile.route) {
            //ProfileScreen()
        }
    }
}
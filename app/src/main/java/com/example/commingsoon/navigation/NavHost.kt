package com.example.commingsoon.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.ui.screens.HomeScreen
import com.example.commingsoon.ui.screens.JourneyOverviewScreen
import com.example.commingsoon.ui.screens.OpenGuesserScreen
import com.example.commingsoon.ui.screens.SettingsScreen
import com.example.commingsoon.ui.theme.AppThemeViewModel
import com.example.commingsoon.viewmodels.JourneyViewModel

@Composable
fun AppNavHost (
    navController: NavHostController,
    themeViewModel: AppThemeViewModel,
    languageViewModel: AppLanguageViewModel,
    journeyViewModel: JourneyViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NavScreens.Home.route
    ) {
        composable(NavScreens.Home.route) {
            HomeScreen()
        }

        composable(NavScreens.Journey.route) {
            JourneyOverviewScreen(
                viewModel = journeyViewModel
            )
        }
        composable(NavScreens.AddJourney.route) {

        }

        composable(NavScreens.OpenGuesser.route) {
            OpenGuesserScreen()
        }

        composable(NavScreens.Friends.route) {
            //FriendsScreen()
        }

        composable(NavScreens.Settings.route) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                languageViewModel = languageViewModel
            )
        }

        composable(NavScreens.Profile.route) {
            //ProfileScreen()
        }
    }
}
package com.example.commingsoon.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.ui.screens.FriendOverviewScreen
import com.example.commingsoon.ui.screens.HomeScreen
import com.example.commingsoon.ui.screens.JourneyDetailScreen
import com.example.commingsoon.ui.screens.JourneyEditorScreen
import com.example.commingsoon.ui.screens.JourneyOverviewScreen
import com.example.commingsoon.ui.screens.OpenGuesserScreen
import com.example.commingsoon.ui.screens.SettingsScreen
import com.example.commingsoon.ui.theme.AppThemeViewModel
import com.example.commingsoon.viewmodels.FriendViewModel
import com.example.commingsoon.viewmodels.JourneyViewModel

@Composable
fun AppNavHost (
    navController: NavHostController,
    themeViewModel: AppThemeViewModel,
    languageViewModel: AppLanguageViewModel,
    journeyViewModel: JourneyViewModel,
    friendViewModel: FriendViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NavScreens.Home.route
    ) {
        composable(NavScreens.Home.route) {
            HomeScreen(
                viewModel = journeyViewModel,
                navController = navController
            )
        }

        composable(NavScreens.Journey.route) {
            JourneyOverviewScreen(
                viewModel = journeyViewModel,
                navController = navController
            )
        }
        composable(NavScreens.JourneyEditor.route) { backStackEntry ->
            val journeyId = backStackEntry.arguments?.getString("journeyId")?.toIntOrNull() ?: -1
            val journey = if (journeyId == -1) { null } else { journeyViewModel.getJourney(journeyId) }

            JourneyEditorScreen(
                viewModel = journeyViewModel,
                journey = journey,
                onDiscard = {
                    navController.popBackStack()
                },
                onSave = { editedJourney ->
                    if (journey == null) {
                        journeyViewModel.addJourney(
                            editedJourney.copy(id = journeyViewModel.getNextJourneyId())
                        )
                    } else {
                        journeyViewModel.updateJourney(editedJourney)
                    }
                    navController.popBackStack()
                }
            )

        }
        composable(
            route = NavScreens.JourneyDetail.route
        ) { backStackEntry ->

            val journeyId = backStackEntry.arguments?.getString("journeyId")?.toIntOrNull() ?: return@composable

            JourneyDetailScreen(
                journeyId = journeyId,
                viewModel = journeyViewModel,
                navController = navController
            )
        }

        composable(NavScreens.OpenGuesser.route) {
            OpenGuesserScreen()
        }

        composable(NavScreens.Friends.route) {
            FriendOverviewScreen(
                viewModel = friendViewModel,
                navController = navController
            )
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
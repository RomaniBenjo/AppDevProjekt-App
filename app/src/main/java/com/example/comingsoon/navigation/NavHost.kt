package com.example.comingsoon.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.comingsoon.language.AppLanguageViewModel
import com.example.comingsoon.R
import com.example.comingsoon.BuildConfig
import com.example.comingsoon.auth.AuthApiClient
import com.example.comingsoon.auth.AuthApiException
import com.example.comingsoon.auth.AuthRepository
import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.auth.GoogleAuthManager
import com.example.comingsoon.auth.GoogleSignInResult
import com.example.comingsoon.overlays.OverlayViewModel
import com.example.comingsoon.ui.screens.FriendDetailScreen
import com.example.comingsoon.ui.screens.FriendOverviewScreen
import com.example.comingsoon.ui.screens.HomeScreen
import com.example.comingsoon.ui.screens.JourneyDetailScreen
import com.example.comingsoon.ui.screens.JourneyEditorScreen
import com.example.comingsoon.ui.screens.JourneyOverviewScreen
import com.example.comingsoon.ui.screens.LiveLocationScreen
import com.example.comingsoon.ui.screens.LoginScreen
import com.example.comingsoon.ui.screens.OnlineOpenGuesserScreen
import com.example.comingsoon.ui.screens.OpenGuesserScreen
import com.example.comingsoon.ui.screens.ProfileEditorScreen
import com.example.comingsoon.ui.screens.ProfileScreen
import com.example.comingsoon.ui.screens.SettingsScreen
import com.example.comingsoon.ui.screens.localopenguesser.LocalOpenGuesserStartScreen
import com.example.comingsoon.ui.screens.localopenguesser.lobby.LocalGameLobbyScreen
import com.example.comingsoon.ui.theme.AppThemeViewModel
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.viewmodels.JourneyViewModel
import com.example.comingsoon.viewmodels.ProfileViewModel
import com.example.comingsoon.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost (
    navController: NavHostController,
    themeViewModel: AppThemeViewModel,
    languageViewModel: AppLanguageViewModel,
    settingsViewModel: SettingsViewModel,
    journeyViewModel: JourneyViewModel,
    friendViewModel: FriendViewModel,
    overlayViewModel: OverlayViewModel,
    profileViewModel: ProfileViewModel
) {
    val context = LocalContext.current
    val authRepository = remember(context) {
        AuthRepository(
            apiClient = AuthApiClient(BuildConfig.API_BASE_URL),
            sessionStore = AuthSessionStore(context.applicationContext)
        )
    }
    val startDestination = remember(authRepository) {
        if (authRepository.currentSession() != null) {
            NavScreens.Home.route
        } else {
            NavScreens.Login.route
        }
    }
    LaunchedEffect(authRepository, profileViewModel) {
        authRepository.currentSession()?.user?.let(profileViewModel::updateFromAuthenticatedUser)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavScreens.Login.route) {
            val context = LocalContext.current
            val authManager = remember(context) { GoogleAuthManager(context.applicationContext) }
            val coroutineScope = rememberCoroutineScope()
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val serverClientId = context.getString(R.string.google_web_client_id)

            LoginScreen(
                onGoogleSignInClick = {
                    if (!isLoading) {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            when (
                                val result = authManager.signIn(
                                    activityContext = context,
                                    serverClientId = serverClientId
                                )
                            ) {
                                is GoogleSignInResult.Success -> {
                                    try {
                                        val session = authRepository.authenticateGoogleUser(
                                            result.user.idToken
                                        )
                                        profileViewModel.updateFromAuthenticatedUser(session.user)
                                        friendViewModel.refresh()
                                        friendViewModel.startRealtimeUpdates()
                                        navController.navigate(NavScreens.Home.route) {
                                            popUpTo(NavScreens.Login.route) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } catch (exception: AuthApiException) {
                                        errorMessage = exception.message
                                    } catch (exception: Exception) {
                                        errorMessage = exception.localizedMessage
                                            ?: "Die Anmeldung konnte nicht abgeschlossen werden."
                                    }
                                }
                                GoogleSignInResult.Cancelled -> Unit
                                is GoogleSignInResult.Error -> {
                                    errorMessage = result.message
                                }
                            }
                            isLoading = false
                        }
                    }
                },
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }

        composable(NavScreens.Home.route) {
            HomeScreen(
                viewModel = journeyViewModel,
                navController = navController
            )
        }

        composable(NavScreens.Journey.route) {
            JourneyOverviewScreen(
                viewModel = journeyViewModel,
                navController = navController,
                overlayViewModel = overlayViewModel
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
                navController = navController,
                overlayViewModel = overlayViewModel
            )
        }

        composable(NavScreens.OpenGuesser.route) {
            OpenGuesserScreen(navController = navController)
        }
        composable(NavScreens.OpenGuesserOnline.route) {
            OnlineOpenGuesserScreen(navController = navController)
        }
        composable(NavScreens.OpenGuesserLocal.route) {
            LocalOpenGuesserStartScreen(navController = navController)
        }
        composable(NavScreens.OpenGuesserLocalLobby.route) {
            LocalGameLobbyScreen(
                navController = navController,
                profileName = profileViewModel.profile.name
            )
        }
        composable(NavScreens.Friends.route) {
            FriendOverviewScreen(
                viewModel = friendViewModel,
                navController = navController,
                overlayViewModel = overlayViewModel
            )
        }
        composable(
            route = NavScreens.FriendDetail.route
        ) { backStackEntry ->

            val friendId = backStackEntry.arguments?.getString("friendId")?.toIntOrNull() ?: return@composable

            FriendDetailScreen(
                friendId = friendId,
                friendViewModel = friendViewModel,
                navController = navController
            )
        }
        composable (NavScreens.LiveLocations.route) {
            LiveLocationScreen(
                navController = navController
            )
        }

        composable(NavScreens.Settings.route) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                languageViewModel = languageViewModel,
                settingsViewModel = settingsViewModel
            )
        }

        composable(NavScreens.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                navController = navController
            )
        }
        composable(NavScreens.ProfileEditor.route) {
            ProfileEditorScreen(
                viewModel = profileViewModel,
                navController = navController
            )
        }
    }
}

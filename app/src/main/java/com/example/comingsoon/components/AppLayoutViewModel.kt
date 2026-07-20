package com.example.comingsoon.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.comingsoon.language.AppLanguageViewModel
import com.example.comingsoon.navigation.AppNavHost
import com.example.comingsoon.navigation.NavScreens
import com.example.comingsoon.overlays.AddFriendOverlay
import com.example.comingsoon.overlays.OverlayType
import com.example.comingsoon.overlays.OverlayViewModel
import com.example.comingsoon.overlays.ShareJourneyOverlay
import com.example.comingsoon.overlays.ShareWithFriendOverlay
import com.example.comingsoon.ui.theme.AppThemeDefinition
import com.example.comingsoon.ui.theme.AppThemeViewModel
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.viewmodels.JourneyViewModel
import com.example.comingsoon.viewmodels.ProfileViewModel
import com.example.comingsoon.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppLayoutViewModel (
    navController: NavHostController,
    themeDefinition: AppThemeDefinition,
    title: String,
    themeViewModel: AppThemeViewModel,
    languageViewModel: AppLanguageViewModel,
    settingsViewModel: SettingsViewModel,
    journeyViewModel: JourneyViewModel,
    friendViewModel: FriendViewModel,
    overlayViewModel: OverlayViewModel,
    profileViewModel: ProfileViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isLoginScreen = currentRoute == NavScreens.Login.route
    val drawerGesturesEnabled = currentRoute != NavScreens.OpenGuesserLocalLobby.route && !isLoginScreen

    when (overlayViewModel.overlayType) {
        OverlayType.SHARE_JOURNEY -> {
            overlayViewModel.selectedJourney?.let { journey ->
                ShareJourneyOverlay(
                    journey = journey,
                    friends = friendViewModel.friends,
                    onDismiss = { overlayViewModel.dismiss() },
                    onShare = { friend ->
                        // TODO
                    }
                )
            }
        }
        OverlayType.SHARE_WITH_FRIEND -> {
            overlayViewModel.selectedFriend?.let { friend ->
                ShareWithFriendOverlay(
                    friend = friend,
                    journeys = journeyViewModel.journeys,
                    onDismiss = { overlayViewModel.dismiss() },
                    onShare = { journey ->
                        // TODO
                    }
                )
            }
        }
        OverlayType.ADD_FRIEND -> {
            AddFriendOverlay(
                viewModel = friendViewModel,
                onDismiss = { overlayViewModel.dismiss() }
            )
        }
        OverlayType.NONE -> {}
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerGesturesEnabled,
        drawerContent = {
            AppMenu(
                navController = navController,
                currentRoute = currentRoute,
                assets = themeDefinition.assets,
                profile = profileViewModel.profile,
                closeMenu = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    )  {
        Scaffold(
            topBar = {
                if (!isLoginScreen) {
                    AppHeader(
                        title = title,
                        assets = themeDefinition.assets,
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        }
                    )
                }
            }
        ) { innerpadding ->
            Box(modifier = Modifier.padding(innerpadding)) {
                AppNavHost(
                    navController = navController,
                    themeViewModel = themeViewModel,
                    languageViewModel = languageViewModel,
                    settingsViewModel = settingsViewModel,
                    journeyViewModel = journeyViewModel,
                    friendViewModel = friendViewModel,
                    overlayViewModel = overlayViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}

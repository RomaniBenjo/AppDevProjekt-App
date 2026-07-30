package com.example.comingsoon.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.comingsoon.viewmodels.AppViewModelFactory
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.viewmodels.JourneyViewModel
import com.example.comingsoon.viewmodels.JourneyShareViewModel
import com.example.comingsoon.viewmodels.CountryViewModel
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
    journeyShareViewModel: JourneyShareViewModel,
    countryViewModel: CountryViewModel,
    friendViewModel: FriendViewModel,
    overlayViewModel: OverlayViewModel,
    profileViewModel: ProfileViewModel,
    viewModelFactory: AppViewModelFactory
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isLoginScreen = currentRoute == NavScreens.Login.route
    val drawerGesturesEnabled = currentRoute != NavScreens.OpenGuesserLocalLobby.route &&
        currentRoute != NavScreens.OpenGuesserOnline.route &&
        currentRoute != NavScreens.LiveLocations.route &&
        !isLoginScreen
    val sharingMessage = journeyShareViewModel.feedback ?: journeyShareViewModel.error

    LaunchedEffect(sharingMessage) {
        sharingMessage?.let {
            snackbarHostState.showSnackbar(it)
            journeyShareViewModel.clearMessage()
        }
    }

    when (overlayViewModel.overlayType) {
        OverlayType.SHARE_JOURNEY -> {
            overlayViewModel.selectedJourney?.let { journey ->
                ShareJourneyOverlay(
                    journey = journey,
                    friends = friendViewModel.friends.filter {
                        it.isServerFriend || it.addedNearby
                    },
                    friendViewModel = friendViewModel,
                    isNetworkAvailable = journeyViewModel.isNetworkAvailable,
                    shareTypesByFriendId = friendViewModel.friends.associate { friend ->
                        friend.id to journeyShareViewModel.shareTypeFor(journey, friend.id)
                    }.filterValues { it != null }.mapValues { requireNotNull(it.value) },
                    operationKey = journeyShareViewModel.operationKey,
                    errorMessage = journeyShareViewModel.error,
                    feedbackMessage = journeyShareViewModel.feedback,
                    qrDeepLink = journeyShareViewModel.qrDeepLink,
                    qrExpiresAt = journeyShareViewModel.qrExpiresAt,
                    isCreatingQrLink = journeyShareViewModel.isCreatingQrLink,
                    onDismiss = {
                        journeyShareViewModel.resetQrShareLink()
                        journeyShareViewModel.clearMessage()
                        overlayViewModel.dismiss()
                    },
                    onShare = { friend ->
                        journeyShareViewModel.shareJourney(journey.id, friend.id)
                    },
                    onUnshare = { friend ->
                        journeyShareViewModel.unshareJourney(journey.id, friend.id)
                    },
                    onRequestQrLink = {
                        journeyShareViewModel.createQrShareLink(journey.id)
                    }
                )
            }
        }
        OverlayType.SHARE_WITH_FRIEND -> {
            overlayViewModel.selectedFriend?.let { friend ->
                ShareWithFriendOverlay(
                    friend = friend,
                    journeys = journeyViewModel.journeys,
                    friendViewModel = friendViewModel,
                    isNetworkAvailable = journeyViewModel.isNetworkAvailable,
                    shareTypesByJourneyId = journeyViewModel.journeys.associate { journey ->
                        journey.id to journeyShareViewModel.shareTypeFor(journey, friend.id)
                    }.filterValues { it != null }.mapValues { requireNotNull(it.value) },
                    operationKey = journeyShareViewModel.operationKey,
                    errorMessage = journeyShareViewModel.error,
                    feedbackMessage = journeyShareViewModel.feedback,
                    onDismiss = {
                        journeyShareViewModel.clearMessage()
                        overlayViewModel.dismiss()
                    },
                    onShare = { journey ->
                        journeyShareViewModel.shareJourney(journey.id, friend.id)
                    },
                    onUnshare = { journey ->
                        journeyShareViewModel.unshareJourney(journey.id, friend.id)
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
        gesturesEnabled = drawerGesturesEnabled || drawerState.isOpen,
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    journeyShareViewModel = journeyShareViewModel,
                    countryViewModel = countryViewModel,
                    friendViewModel = friendViewModel,
                    overlayViewModel = overlayViewModel,
                    profileViewModel = profileViewModel,
                    viewModelFactory = viewModelFactory
                )
            }
        }
    }
}

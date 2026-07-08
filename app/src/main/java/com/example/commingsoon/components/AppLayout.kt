package com.example.commingsoon.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.navigation.AppNavHost
import com.example.commingsoon.overlays.ShareJourneyOverlay
import com.example.commingsoon.overlays.ShareViewModel
import com.example.commingsoon.ui.theme.AppThemeDefinition
import com.example.commingsoon.ui.theme.AppThemeViewModel
import com.example.commingsoon.viewmodels.FriendViewModel
import com.example.commingsoon.viewmodels.JourneyViewModel
import kotlinx.coroutines.launch

@Composable
fun AppLayout (
    navController: NavHostController,
    themeDefinition: AppThemeDefinition,
    title: String,
    themeViewModel: AppThemeViewModel,
    languageViewModel: AppLanguageViewModel,
    journeyViewModel: JourneyViewModel,
    friendViewModel: FriendViewModel,
    shareViewModel: ShareViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    shareViewModel.selectedJourney?.let { journey ->
        ShareJourneyOverlay(
            journey = journey,
            friends = friendViewModel.friends,
            onDismiss = {
                shareViewModel.hide()
            },
            onShare = { friend ->
                // TODO
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppMenu(
                navController = navController,
                currentRoute = navController.currentDestination?.route,
                assets = themeDefinition.assets,
                closeMenu = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    )  {
        Scaffold(
            topBar = {
                AppHeader(
                    title = title,
                    assets = themeDefinition.assets,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    }
                )
            }
        ) { innerpadding ->
            Box(modifier = Modifier.padding(innerpadding)) {
                AppNavHost(
                    navController = navController,
                    themeViewModel = themeViewModel,
                    languageViewModel = languageViewModel,
                    journeyViewModel = journeyViewModel,
                    friendViewModel = friendViewModel,
                    shareViewModel = shareViewModel
                )
            }
        }
    }
}
package com.example.commingsoon.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.commingsoon.navigation.NavScreens
import com.example.commingsoon.ui.theme.AppThemeAssets

@Composable
fun AppMenu (
    navController: NavController,
    currentRoute: String?,
    assets: AppThemeAssets,
    closeMenu: () -> Unit
) {
    val items = listOf(
        NavScreens.Home,
        NavScreens.Journey,
        NavScreens.OpenGuesser,
        NavScreens.Friends,
        NavScreens.Settings,
        NavScreens.Profile
    )

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxWidth(0.66f)
            .navigationBarsPadding(),
        drawerContainerColor = MaterialTheme.colorScheme.tertiary
    ) {
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.tertiary)
                    .fillMaxSize()
            )

            Column() {
                items.forEach { screens ->
                    NavigationDrawerItem(
                        icon = {
                            screens.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    modifier = Modifier.width(28.dp),
                                    tint = MaterialTheme.colorScheme.background
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screens.title,
                                color = MaterialTheme.colorScheme.background
                            )
                        },
                        selected = currentRoute == screens.route,
                        onClick = {
                            navController.navigate(screens.route) { launchSingleTop = true }
                            closeMenu()
                        }
                    )
                }
            }
        }

    }
}
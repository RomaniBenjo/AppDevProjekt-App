package com.example.commingsoon.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            .navigationBarsPadding()
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.secondary)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            Color.Transparent
                        )
                    )
                )
        )

        // Image on Side
        Image(
            painter = painterResource(assets.menuShape),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column() {
            items.forEach { screens ->
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.width(28.dp)
                        )
                    },
                    label = {
                        Text(screens.title)
                    },
                    selected = currentRoute == screens.route,
                    onClick = {
                        navController.navigate(screens.route)
                        closeMenu()
                    }
                )
            }
        }
    }
}
package com.example.comingsoon.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.comingsoon.navigation.NavScreens
import com.example.comingsoon.ui.theme.AppThemeAssets
import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalConfiguration
import com.example.comingsoon.language.appString
import com.example.comingsoon.R
import com.example.comingsoon.viewmodels.Profile

@Composable
fun AppMenu (
    navController: NavController,
    currentRoute: String?,
    assets: AppThemeAssets,
    profile: Profile,
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

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxWidth(if (isLandscape) 0.33f else 0.66f)
            .navigationBarsPadding(),
        drawerContainerColor = MaterialTheme.colorScheme.tertiary
    ) {
        Spacer(Modifier.height(if (isLandscape) 8.dp else 32.dp))

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.tertiary)
                    .fillMaxSize()
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = if (isLandscape) 8.dp else 24.dp,
                    bottom = 24.dp
                )
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProfileAvatar(
                            profile = profile,
                            contentDescription = appString(R.string.profile_picture_of, profile.name),
                            modifier = Modifier.size(84.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = profile.name,
                            color = MaterialTheme.colorScheme.background,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                items(items) { screens ->
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
                                text = appString(screens.title),
                                color = MaterialTheme.colorScheme.background
                            )
                        },
                        selected = false /* currentRoute == screens.route */,
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

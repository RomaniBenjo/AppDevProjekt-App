package com.example.commingsoon.components

import android.R.attr.theme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.commingsoon.ui.theme.AppThemeDefinition
import kotlinx.coroutines.launch

@Composable
fun AppScaffold (
    navController: NavHostController,
    themeDefinition: AppThemeDefinition,
    title: String
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppMenu(
                navController = navController,
                currentRoute = navController.currentDestination?.route,
                assets = /*TODO*/,
                closeMenu = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    )  {

    }
}
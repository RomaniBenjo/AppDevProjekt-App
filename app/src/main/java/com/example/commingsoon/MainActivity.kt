package com.example.commingsoon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.commingsoon.components.AppLayout
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.overlays.ShareViewModel
import com.example.commingsoon.ui.theme.AppThemeViewModel
import com.example.commingsoon.ui.theme.CommingSoonTheme
import com.example.commingsoon.viewmodels.FriendViewModel
import com.example.commingsoon.viewmodels.JourneyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val themeViewModel: AppThemeViewModel = viewModel()
            val languageViewModel: AppLanguageViewModel = viewModel()
            val journeyViewModel: JourneyViewModel = viewModel()
            val friendViewModel: FriendViewModel = viewModel()
            val shareViewModel: ShareViewModel = viewModel()

            val isDark = isSystemInDarkTheme()
            LaunchedEffect(Unit) {
                themeViewModel.setMode(isDark)
            }
            val currentAppTheme = themeViewModel.getThemeDefinition()

            CommingSoonTheme(
                theme = currentAppTheme,
                darkTheme = themeViewModel.darkMode
            ) {
                AppLayout(
                    navController = navController,
                    themeDefinition = currentAppTheme,
                    title = "Comming Soon",
                    themeViewModel = themeViewModel,
                    languageViewModel = languageViewModel,
                    journeyViewModel = journeyViewModel,
                    friendViewModel= friendViewModel,
                    shareViewModel = shareViewModel
                )
            }

        }
    }
}


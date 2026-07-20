package com.example.commingsoon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.commingsoon.components.AppLayoutViewModel
import com.example.commingsoon.data.AppPreferenceRepository
import com.example.commingsoon.auth.AuthSessionStore
import com.example.commingsoon.friends.FriendsApiClient
import com.example.commingsoon.friends.FriendsRepository
import com.example.commingsoon.language.AppLanguageViewModel
import com.example.commingsoon.language.LocalAppLanguage
import com.example.commingsoon.language.LocalLocalizedContext
import com.example.commingsoon.language.localized
import com.example.commingsoon.notifications.NotificationsHelper
import com.example.commingsoon.overlays.OverlayViewModel
import com.example.commingsoon.ui.theme.AppThemeViewModel
import com.example.commingsoon.ui.theme.CommingSoonTheme
import com.example.commingsoon.viewmodels.AppViewModelFactory
import com.example.commingsoon.viewmodels.FriendViewModel
import com.example.commingsoon.viewmodels.JourneyViewModel
import com.example.commingsoon.viewmodels.ProfileViewModel
import com.example.commingsoon.viewmodels.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationsHelper(this).createNotificationChannel()
        enableEdgeToEdge()

        setContent {
            val repository = AppPreferenceRepository(applicationContext)
            val friendsRepository = FriendsRepository(
                apiClient = FriendsApiClient(BuildConfig.API_BASE_URL),
                sessionStore = AuthSessionStore(applicationContext)
            )
            val factory = AppViewModelFactory(repository, friendsRepository)

            val navController = rememberNavController()
            val themeViewModel: AppThemeViewModel = viewModel(factory = factory)
            val languageViewModel: AppLanguageViewModel = viewModel(factory = factory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val journeyViewModel: JourneyViewModel = viewModel()
            val friendViewModel: FriendViewModel = viewModel(factory = factory)
            val overlayViewModel: OverlayViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()

            val context = androidx.compose.ui.platform.LocalContext.current
            val isDark = isSystemInDarkTheme()
            LaunchedEffect(Unit) {
                themeViewModel.updateMode(isDark)
                journeyViewModel.loadJourneys(context)
            }

            val currentAppTheme = themeViewModel.getThemeDefinition()

            val localizedContext = LocalContext.current.localized(languageViewModel.currentLanguage)
            CompositionLocalProvider(
                LocalAppLanguage provides languageViewModel.currentLanguage,
                LocalLocalizedContext provides localizedContext
            ) {
                CommingSoonTheme(
                    theme = currentAppTheme,
                    darkTheme = themeViewModel.darkMode
                ) {
                    AppLayoutViewModel(
                        navController = navController,
                        themeDefinition = currentAppTheme,
                        title = "Coming Soon",
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
}


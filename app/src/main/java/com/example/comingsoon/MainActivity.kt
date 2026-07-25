package com.example.comingsoon

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.comingsoon.components.AppLayoutViewModel
import com.example.comingsoon.data.AppPreferenceRepository
import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.friends.FriendsApiClient
import com.example.comingsoon.friends.FriendsRepository
import com.example.comingsoon.friends.FriendQrPayload
import com.example.comingsoon.sync.ClaimedCountriesApiClient
import com.example.comingsoon.sync.ClaimedCountriesRepository
import com.example.comingsoon.sync.JourneysApiClient
import com.example.comingsoon.sync.JourneysRepository
import com.example.comingsoon.sync.LiveLocationApiClient
import com.example.comingsoon.sync.LiveLocationRepository
import com.example.comingsoon.db.AppDatabase
import com.example.comingsoon.language.AppLanguageViewModel
import com.example.comingsoon.language.LocalAppLanguage
import com.example.comingsoon.language.LocalLocalizedContext
import com.example.comingsoon.language.localized
import com.example.comingsoon.notifications.NotificationsHelper
import com.example.comingsoon.overlays.OverlayViewModel
import com.example.comingsoon.ui.theme.AppThemeViewModel
import com.example.comingsoon.ui.theme.ComingSoonTheme
import com.example.comingsoon.viewmodels.AppViewModelFactory
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.viewmodels.JourneyViewModel
import com.example.comingsoon.viewmodels.ProfileViewModel
import com.example.comingsoon.viewmodels.SettingsViewModel
import com.example.comingsoon.navigation.NavScreens

class MainActivity : ComponentActivity() {
    private var pendingFriendId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingFriendId = FriendQrPayload.parse(intent?.dataString)
        NotificationsHelper(this).apply {
            createNotificationChannel()
            createLiveLocationNotificationChannel()
        }
        enableEdgeToEdge()

        setContent {
            val repository = AppPreferenceRepository(applicationContext)
            val friendsRepository = FriendsRepository(
                apiClient = FriendsApiClient(BuildConfig.API_BASE_URL),
                sessionStore = AuthSessionStore(applicationContext),
                friendDao = AppDatabase.getDatabase(applicationContext).friendDao(),
                context = applicationContext
            )
            val journeysRepository = JourneysRepository(
                apiClient = JourneysApiClient(BuildConfig.API_BASE_URL),
                sessionStore = AuthSessionStore(applicationContext),
                journeyDao = AppDatabase.getDatabase(applicationContext).journeyDao()
            )
            val claimedCountriesRepository = ClaimedCountriesRepository(
                apiClient = ClaimedCountriesApiClient(BuildConfig.API_BASE_URL),
                sessionStore = AuthSessionStore(applicationContext),
                claimedCountryDao = AppDatabase.getDatabase(applicationContext).claimedCountryDao(),
                context = applicationContext
            )
            val liveLocationRepository = LiveLocationRepository(
                apiClient = LiveLocationApiClient(BuildConfig.API_BASE_URL),
                sessionStore = AuthSessionStore(applicationContext)
            )
            val factory = AppViewModelFactory(
                repository,
                friendsRepository,
                journeysRepository,
                claimedCountriesRepository,
                liveLocationRepository,
                applicationContext
            )

            val navController = rememberNavController()
            val themeViewModel: AppThemeViewModel = viewModel(factory = factory)
            val languageViewModel: AppLanguageViewModel = viewModel(factory = factory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val journeyViewModel: JourneyViewModel = viewModel(factory = factory)
            val friendViewModel: FriendViewModel = viewModel(factory = factory)
            val overlayViewModel: OverlayViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()

            LaunchedEffect(pendingFriendId, friendViewModel.currentUserId) {
                val friendId = pendingFriendId ?: return@LaunchedEffect
                val currentUserId = friendViewModel.currentUserId ?: return@LaunchedEffect

                pendingFriendId = null
                if (friendId != currentUserId) {
                    navController.navigate(NavScreens.Friends.route) {
                        launchSingleTop = true
                    }
                    friendViewModel.sendFriendRequest(friendId)
                }
            }

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
                ComingSoonTheme(
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
                        profileViewModel = profileViewModel,
                        viewModelFactory = factory
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        FriendQrPayload.parse(intent?.dataString)?.let { pendingFriendId = it }
    }
}


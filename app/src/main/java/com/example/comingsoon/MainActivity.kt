package com.example.comingsoon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.core.content.ContextCompat
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
import com.example.comingsoon.sync.JourneyShareLink
import com.example.comingsoon.sync.LiveLocationApiClient
import com.example.comingsoon.sync.LiveLocationRepository
import com.example.comingsoon.db.AppDatabase
import com.example.comingsoon.language.AppLanguageViewModel
import com.example.comingsoon.language.LocalAppLanguage
import com.example.comingsoon.language.LocalLocalizedContext
import com.example.comingsoon.language.localized
import com.example.comingsoon.notifications.NotificationsHelper
import com.example.comingsoon.notifications.FriendRequestNotificationScheduler
import com.example.comingsoon.overlays.OverlayViewModel
import com.example.comingsoon.ui.theme.AppThemeViewModel
import com.example.comingsoon.ui.theme.ComingSoonTheme
import com.example.comingsoon.viewmodels.AppViewModelFactory
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.viewmodels.JourneyViewModel
import com.example.comingsoon.viewmodels.JourneyShareViewModel
import com.example.comingsoon.viewmodels.CountryViewModel
import com.example.comingsoon.viewmodels.ProfileViewModel
import com.example.comingsoon.viewmodels.SettingsViewModel
import com.example.comingsoon.navigation.NavScreens

class MainActivity : ComponentActivity() {
    private var pendingFriendId by mutableStateOf<Int?>(null)
    private var pendingJourneyShareToken by mutableStateOf<String?>(null)
    private var openFriendsFromNotification by mutableStateOf(false)
    private var notificationPermissionRequestedThisSession = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingFriendId = FriendQrPayload.parse(intent?.dataString)
        pendingJourneyShareToken = JourneyShareLink.parse(intent?.dataString)
        openFriendsFromNotification = intent?.getBooleanExtra(OPEN_FRIENDS_EXTRA, false) == true
        NotificationsHelper(this).apply {
            createNotificationChannel()
            createLiveLocationNotificationChannel()
            createFriendNotificationChannel()
        }
        FriendRequestNotificationScheduler.schedule(applicationContext)
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
                journeyDao = AppDatabase.getDatabase(applicationContext).journeyDao(),
                sharedJourneyDao = AppDatabase.getDatabase(applicationContext).sharedJourneyDao(),
                pendingJourneyShareDao = AppDatabase.getDatabase(applicationContext)
                    .pendingJourneyShareDao()
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
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val themeViewModel: AppThemeViewModel = viewModel(factory = factory)
            val languageViewModel: AppLanguageViewModel = viewModel(factory = factory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val journeyViewModel: JourneyViewModel = viewModel(factory = factory)
            val journeyShareViewModel: JourneyShareViewModel = viewModel(factory = factory)
            val countryViewModel: CountryViewModel = viewModel(factory = factory)
            val friendViewModel: FriendViewModel = viewModel(factory = factory)
            val overlayViewModel: OverlayViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()

            DisposableEffect(friendViewModel, journeyViewModel) {
                friendViewModel.bindOfflineJourneyCallbacks(
                    onReceived = journeyShareViewModel::receiveOfflineJourney,
                    onSent = journeyShareViewModel::recordSentOfflineJourney
                )
                journeyShareViewModel.bindJourneyRefresh(journeyViewModel::reloadFromDatabase)
                onDispose {
                    friendViewModel.unbindOfflineJourneyCallbacks()
                }
            }

            LaunchedEffect(
                currentBackStackEntry?.destination?.route,
                friendViewModel.currentUserId
            ) {
                if (friendViewModel.currentUserId != null) {
                    requestNotificationPermissionIfNeeded()
                }
            }

            LaunchedEffect(openFriendsFromNotification, friendViewModel.currentUserId) {
                if (
                    openFriendsFromNotification &&
                    friendViewModel.currentUserId != null
                ) {
                    openFriendsFromNotification = false
                    navController.navigate(NavScreens.Friends.route) {
                        launchSingleTop = true
                    }
                }
            }

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
            LaunchedEffect(pendingJourneyShareToken, friendViewModel.currentUserId) {
                val token = pendingJourneyShareToken ?: return@LaunchedEffect
                if (friendViewModel.currentUserId == null) return@LaunchedEffect
                pendingJourneyShareToken = null
                journeyShareViewModel.acceptShareLink(token) { share ->
                    if (share != null) {
                        navController.navigate(
                            NavScreens.SharedJourneyDetail.createRoute(
                                share.ownerId,
                                requireNotNull(share.journey.serverId)
                            )
                        )
                    }
                }
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            val isDark = isSystemInDarkTheme()
            LaunchedEffect(Unit) {
                themeViewModel.updateMode(isDark)
                journeyViewModel.loadJourneys()
                journeyShareViewModel.load()
                countryViewModel.load()
            }
            val journeyRemindersEnabled = settingsViewModel.isJourneyReminderEnabled()
            val journeyReminderTime = settingsViewModel.getReminderTime()
            LaunchedEffect(journeyRemindersEnabled, journeyReminderTime) {
                journeyViewModel.configureJourneyReminders(
                    enabled = journeyRemindersEnabled,
                    reminderTime = journeyReminderTime
                )
            }
            LaunchedEffect(friendViewModel.journeyShareUpdateVersion) {
                if (friendViewModel.journeyShareUpdateVersion > 0) {
                    journeyViewModel.triggerSync()
                    journeyShareViewModel.synchronize()
                }
            }
            LaunchedEffect(journeyViewModel.isNetworkAvailable) {
                if (journeyViewModel.isNetworkAvailable) {
                    journeyShareViewModel.synchronize()
                    countryViewModel.synchronize()
                }
            }

            val currentAppTheme = themeViewModel.getThemeDefinition()

            val localizedContext = LocalContext.current.localized(languageViewModel.currentLanguage)
            LaunchedEffect(languageViewModel.currentLanguage) {
                journeyViewModel.onLanguageChanged()
                journeyShareViewModel.onLanguageChanged()
                countryViewModel.onLanguageChanged()
                friendViewModel.onLanguageChanged()
                NotificationsHelper(applicationContext).apply {
                    createNotificationChannel()
                    createLiveLocationNotificationChannel()
                    createFriendNotificationChannel()
                }
            }
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
                        title = localizedContext.getString(R.string.app_name),
                        themeViewModel = themeViewModel,
                        languageViewModel = languageViewModel,
                        settingsViewModel = settingsViewModel,
                        journeyViewModel = journeyViewModel,
                        journeyShareViewModel = journeyShareViewModel,
                        countryViewModel = countryViewModel,
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
        JourneyShareLink.parse(intent?.dataString)?.let {
            pendingJourneyShareToken = it
        }
        if (intent?.getBooleanExtra(OPEN_FRIENDS_EXTRA, false) == true) {
            openFriendsFromNotification = true
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33 || notificationPermissionRequestedThisSession) return
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionRequestedThisSession = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val OPEN_FRIENDS_EXTRA = "open_friends_from_notification"
    }
}

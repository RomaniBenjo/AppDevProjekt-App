package com.example.comingsoon.location

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.comingsoon.BuildConfig
import com.example.comingsoon.MainActivity
import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.data.AppPreferenceRepository
import com.example.comingsoon.notifications.NotificationsHelper
import com.example.comingsoon.sync.LiveLocationApiClient
import com.example.comingsoon.sync.LiveLocationRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class LiveLocationService : Service() {

    companion object {
        private const val TAG = "LiveLocationService"
        const val ACTION_START = "com.example.comingsoon.action.START_LIVE_LOCATION"
        const val ACTION_STOP = "com.example.comingsoon.action.STOP_LIVE_LOCATION"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private val _lastLocation = MutableStateFlow<Location?>(null)
        val lastLocation: StateFlow<Location?> = _lastLocation

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LiveLocationService::class.java).setAction(ACTION_START)
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, LiveLocationService::class.java).setAction(ACTION_STOP)
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: com.google.android.gms.location.FusedLocationProviderClient
    private lateinit var repository: LiveLocationRepository
    private lateinit var preferenceRepository: AppPreferenceRepository
    private lateinit var notificationsHelper: NotificationsHelper
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        repository = LiveLocationRepository(
            apiClient = LiveLocationApiClient(BuildConfig.API_BASE_URL),
            sessionStore = AuthSessionStore(applicationContext)
        )
        preferenceRepository = AppPreferenceRepository(applicationContext)
        notificationsHelper = NotificationsHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = notificationsHelper.buildLiveLocationNotification(
            contentIntent = PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ),
            stopIntent = PendingIntent.getService(
                this, 0, Intent(this, LiveLocationService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationsHelper.LIVE_LOCATION_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NotificationsHelper.LIVE_LOCATION_NOTIFICATION_ID, notification)
        }

        _isRunning.value = true
        scope.launch {
            preferenceRepository.saveLiveLocationSharingEnabled(true)
            runCatching { repository.startSharing() }
                .onSuccess { Log.i(TAG, "Sharing session started: $it") }
                .onFailure { Log.e(TAG, "Failed to start sharing session", it) }
        }
        beginLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
        _isRunning.value = false
        _lastLocation.value = null
        scope.launch {
            runCatching { repository.stopSharing() }
                .onFailure { Log.e(TAG, "Failed to stop sharing session", it) }
            preferenceRepository.saveLiveLocationSharingEnabled(false)
        }.invokeOnCompletion { scope.cancel() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun beginLocationUpdates() {
        if (!hasForegroundLocationPermission(this)) {
            Log.e(TAG, "No foreground location permission — stopping service")
            stopSelf()
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 17_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .setMaxUpdateDelayMillis(20_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                Log.i(TAG, "New fix: ${location.latitude}, ${location.longitude} (±${location.accuracy}m)")
                _lastLocation.value = location
                scope.launch {
                    runCatching {
                        repository.pushFix(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy,
                            recordedAt = Instant.now().toString()
                        )
                    }.onFailure { Log.e(TAG, "Failed to push fix", it) }
                }
            }
        }
        locationCallback = callback

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates", e)
            stopSelf()
        }
    }
}

package com.example.comingsoon.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.comingsoon.BuildConfig
import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.friends.FriendsApiClient
import com.example.comingsoon.friends.ServerFriendRequest
import java.util.concurrent.TimeUnit

class FriendRequestNotificationStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun remember(userId: Int, requestIds: Collection<Int>) {
        val merged = (knownIds(userId) + requestIds)
            .sortedDescending()
            .take(MAX_REMEMBERED_IDS)
            .toSet()
        preferences.edit()
            .putBoolean(initializedKey(userId), true)
            .putStringSet(idsKey(userId), merged.map(Int::toString).toSet())
            .apply()
    }

    @Synchronized
    fun newRequests(
        userId: Int,
        requests: List<ServerFriendRequest>
    ): List<ServerFriendRequest> {
        if (!preferences.getBoolean(initializedKey(userId), false)) {
            remember(userId, requests.map(ServerFriendRequest::id))
            return emptyList()
        }
        val known = knownIds(userId)
        val newIds = newIncomingFriendRequestIds(
            knownRequestIds = known,
            currentRequestIds = requests.map(ServerFriendRequest::id)
        )
        val result = requests.filter { it.id in newIds }
        remember(userId, requests.map(ServerFriendRequest::id))
        return result
    }

    private fun knownIds(userId: Int): Set<Int> =
        preferences.getStringSet(idsKey(userId), emptySet())
            .orEmpty()
            .mapNotNullTo(mutableSetOf(), String::toIntOrNull)

    private fun initializedKey(userId: Int) = "initialized_$userId"
    private fun idsKey(userId: Int) = "request_ids_$userId"

    private companion object {
        const val PREFERENCES_NAME = "friend_request_notifications"
        const val MAX_REMEMBERED_IDS = 200
    }
}

class FriendRequestNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val session = AuthSessionStore(applicationContext).load() ?: return Result.success()
        return runCatching {
            val notificationsHelper = NotificationsHelper(applicationContext).apply {
                createFriendNotificationChannel()
            }
            val requests = FriendsApiClient(BuildConfig.API_BASE_URL)
                .getRequests(session.accessToken)
                .incoming
            FriendRequestNotificationStore(applicationContext)
                .newRequests(session.user.id.toInt(), requests)
                .forEach { request ->
                    val senderName = request.sender.name
                        ?.takeIf(String::isNotBlank)
                        ?: request.sender.email.substringBefore('@')
                    notificationsHelper.showFriendRequest(
                        requestId = request.id,
                        senderName = senderName
                    )
                }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}

object FriendRequestNotificationScheduler {
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<FriendRequestNotificationWorker>(
            15,
            TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private const val WORK_NAME = "friend_request_notifications"
}

internal fun newIncomingFriendRequestIds(
    knownRequestIds: Set<Int>,
    currentRequestIds: Collection<Int>
): Set<Int> = currentRequestIds.filterTo(mutableSetOf()) { it !in knownRequestIds }

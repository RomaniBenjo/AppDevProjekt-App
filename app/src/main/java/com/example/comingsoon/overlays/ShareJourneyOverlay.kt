package com.example.comingsoon.overlays

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.comingsoon.viewmodels.Friend
import com.example.comingsoon.R
import com.example.comingsoon.language.appString
import com.example.comingsoon.viewmodels.Journey
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.friends.OfflinePairingMode
import com.example.comingsoon.friends.OfflinePairingPhase
import com.example.comingsoon.ui.screens.localopenguesser.connection.hasNearbyPermissions
import com.example.comingsoon.ui.screens.localopenguesser.connection.requiredNearbyPermissions
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ShareTab {
    FRIENDS,
    QR_CODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareJourneyOverlay(
    journey: Journey,
    friends: List<Friend>,
    friendViewModel: FriendViewModel,
    isNetworkAvailable: Boolean,
    shareTypesByFriendId: Map<Int, String> = emptyMap(),
    operationKey: String? = null,
    errorMessage: String? = null,
    feedbackMessage: String? = null,
    qrDeepLink: String? = null,
    qrExpiresAt: String? = null,
    isCreatingQrLink: Boolean = false,
    onDismiss: () -> Unit,
    onShare: (Friend) -> Unit,
    onUnshare: (Friend) -> Unit,
    onRequestQrLink: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentTab by rememberSaveable { mutableStateOf(ShareTab.FRIENDS) }
    val context = LocalContext.current
    val offlineState by friendViewModel.offlinePairingState.collectAsState()
    var pendingOfflineFriend by remember { mutableStateOf<Friend?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val friend = pendingOfflineFriend
        pendingOfflineFriend = null
        if (friend != null && result.values.all { it }) {
            friendViewModel.shareJourneyOffline(journey, friend)
        }
    }
    val shareWithFriend: (Friend) -> Unit = { friend ->
        val useNearby = friend.addedNearby && (!isNetworkAvailable || friend.id <= 0)
        if (!useNearby) {
            onShare(friend)
        } else if (hasNearbyPermissions(context)) {
            friendViewModel.shareJourneyOffline(journey, friend)
        } else {
            pendingOfflineFriend = friend
            permissionLauncher.launch(requiredNearbyPermissions())
        }
    }
    val offlineTransferActive =
        offlineState.mode == OfflinePairingMode.JOURNEY_SHARING &&
            offlineState.phase != OfflinePairingPhase.IDLE

    ModalBottomSheet(
        onDismissRequest = {
            if (offlineTransferActive) friendViewModel.stopOfflinePairing()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {

            // switch at top
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 20.dp)
                    .height(5.dp)
                    .fillMaxWidth(.18f)
                    .background(
                        Color.LightGray,
                        RoundedCornerShape(50)
                    )
            )
            if (offlineTransferActive) {
                OfflineJourneyTransferPanel(
                    viewModel = friendViewModel,
                    onDone = {
                        friendViewModel.stopOfflinePairing()
                        onDismiss()
                    }
                )
                return@Column
            }

            Text(text = journey.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = appString(R.string.share_journey),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            feedbackMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ShareModeSwitch(
                selected = currentTab,
                onSelected = {
                    currentTab = it
                },
                leftTitle = appString(R.string.friends),
                rightTitle = appString(R.string.qr_code)
            )

            when (currentTab) {
                ShareTab.FRIENDS -> {
                    FriendShareList(
                        friends = friends,
                        shareTypesByFriendId = shareTypesByFriendId,
                        operationKey = operationKey,
                        journeyId = journey.id,
                        onShare = shareWithFriend,
                        onUnshare = onUnshare
                    )
                }
                ShareTab.QR_CODE -> {
                    LaunchedEffect(Unit) { onRequestQrLink() }
                    QrCodeView(
                        journey = journey,
                        deepLink = qrDeepLink,
                        expiresAt = qrExpiresAt,
                        isLoading = isCreatingQrLink
                    )
                }
            }
        }
    }
}

@Composable
fun ShareModeSwitch(
    selected: ShareTab,
    onSelected: (ShareTab) -> Unit,
    leftTitle: String,
    rightTitle: String
) {
    Row(
        modifier = Modifier
            .padding(vertical = 20.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(50)
            )
    ) {
        val selectedColor = MaterialTheme.colorScheme.secondary
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (selected == ShareTab.FRIENDS)
                        selectedColor
                    else
                        Color.Transparent
                )
                .clickable { onSelected(ShareTab.FRIENDS) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leftTitle,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    if (selected == ShareTab.QR_CODE) {
                        selectedColor
                    } else { Color.Transparent }
                )
                .clickable { onSelected(ShareTab.QR_CODE) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rightTitle,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun FriendShareList(
    friends: List<Friend>,
    shareTypesByFriendId: Map<Int, String>,
    operationKey: String?,
    journeyId: Int,
    onShare: (Friend) -> Unit,
    onUnshare: (Friend) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 450.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(friends) { friend ->
            FriendShareItem(
                friend = friend,
                shareType = shareTypesByFriendId[friend.id],
                isLoading = operationKey == "$journeyId:${friend.id}",
                onShare = { onShare(friend) },
                onUnshare = { onUnshare(friend) }
            )
            HorizontalDivider(
                color = Color.LightGray.copy(alpha = .3f),
                modifier = Modifier.padding(start = 50.dp)
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun FriendShareItem(
    friend: Friend,
    shareType: String?,
    isLoading: Boolean,
    onShare: () -> Unit,
    onUnshare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                friend.image ?: R.drawable.profile_placeholder
            ),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )

        Spacer(Modifier.width(14.dp))

        Text(
            text = friend.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium
        )

        FilledTonalButton(
            onClick = if (shareType == "manual") onUnshare else onShare,
            enabled = !isLoading && shareType != "automatic" && shareType != "offline",
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (shareType == "manual") {
                        Icons.Outlined.LinkOff
                    } else {
                        Icons.Outlined.Share
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when (shareType) {
                        "manual" -> appString(R.string.unshare)
                        "automatic" -> appString(R.string.shared_automatically)
                        "offline" -> appString(R.string.offline_shared)
                        else -> appString(R.string.share)
                    }
                )
            }
        }
    }
}

@Composable
fun QrCodeView(
    journey: Journey,
    deepLink: String?,
    expiresAt: String?,
    isLoading: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val qrBitmap = remember(deepLink) {
        deepLink?.let { createQrBitmap(it, 760) }
    }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var saveFailed by remember { mutableStateOf(false) }
    val savedToGalleryMessage = appString(R.string.qr_saved_to_gallery)
    val saveFailedMessage = appString(R.string.qr_save_failed)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = journey.title,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = appString(R.string.share_journey_qr_description),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.size(250.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator()
                    qrBitmap != null -> Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = appString(R.string.qr_code),
                        modifier = Modifier.size(210.dp)
                    )
                    else -> Text(appString(R.string.qr_unavailable))
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = if (expiresAt != null) {
                appString(R.string.qr_expires, expiresAt.take(16).replace("T", " "))
            } else {
                appString(R.string.friends_can_scan)
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(28.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(50),
            enabled = qrBitmap != null && !isLoading && !isSaving,
            onClick = {
                val bitmap = qrBitmap ?: return@Button
                isSaving = true
                saveMessage = null
                scope.launch {
                    val saved = saveQrBitmap(
                        context = context,
                        bitmap = bitmap,
                        journeyTitle = journey.title
                    )
                    isSaving = false
                    saveFailed = !saved
                    saveMessage = if (saved) savedToGalleryMessage else saveFailedMessage
                }
            }
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(appString(R.string.save_qr_code))
            }
        }

        saveMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = if (saveFailed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

private suspend fun saveQrBitmap(
    context: Context,
    bitmap: Bitmap,
    journeyTitle: String
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val safeTitle = journeyTitle
            .trim()
            .replace(Regex("[^A-Za-z0-9_-]+"), "-")
            .trim('-')
            .take(40)
            .ifBlank { "journey" }
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "comingsoon-$safeTitle-${System.currentTimeMillis()}.png"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/ComingSoon"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create MediaStore entry")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Could not encode QR bitmap"
                }
            } ?: error("Could not open MediaStore output")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null
            )
        } catch (exception: Exception) {
            resolver.delete(uri, null, null)
            throw exception
        }
    }.isSuccess
}

private fun createQrBitmap(value: String, size: Int): Bitmap {
    val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

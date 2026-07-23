package com.example.comingsoon.overlays

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.comingsoon.viewmodels.Friend
import com.example.comingsoon.viewmodels.FriendViewModel
import com.example.comingsoon.R
import com.example.comingsoon.components.FriendAvatar
import com.example.comingsoon.language.appString
import com.example.comingsoon.friends.FriendQrPayload
import com.example.comingsoon.friends.createFriendQrBitmap
import com.example.comingsoon.friends.OfflinePairingPhase
import com.example.comingsoon.ui.screens.localopenguesser.connection.hasNearbyPermissions
import com.example.comingsoon.ui.screens.localopenguesser.connection.requiredNearbyPermissions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendOverlay(
    viewModel: FriendViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var currentTab by rememberSaveable {
        mutableStateOf(ShareTab.FRIENDS)
    }
    var showOfflinePairing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.clearError() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
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

            if (showOfflinePairing) {
                OfflineFriendPairingView(
                    viewModel = viewModel,
                    onBack = { showOfflinePairing = false }
                )
            } else {
                Text(
                    text = appString(R.string.new_friend),
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = appString(R.string.search_friend_or_qr),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = { showOfflinePairing = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.BluetoothSearching, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(appString(R.string.offline_friend_nearby))
                }

                // composable in ShareJourneyOverlay.kt
                ShareModeSwitch(
                    selected = currentTab,
                    onSelected = { currentTab = it },
                    leftTitle = appString(R.string.search),
                    rightTitle = appString(R.string.qr_code)
                )

                when (currentTab) {
                    ShareTab.FRIENDS ->
                        FriendSearchView(
                            viewModel = viewModel
                        )

                    ShareTab.QR_CODE ->
                        FriendQrView(viewModel)
                }
            }
        }
    }
}

@Composable
private fun OfflineFriendPairingView(
    viewModel: FriendViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.offlinePairingState.collectAsState()
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) pendingAction?.invoke()
        pendingAction = null
    }
    val runWithPermissions: (() -> Unit) -> Unit = { action ->
        if (hasNearbyPermissions(context)) {
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(requiredNearbyPermissions())
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopOfflinePairing() }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(appString(R.string.back))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = appString(R.string.offline_friend_title),
            style = MaterialTheme.typography.titleLarge
        )
    }
      Spacer(Modifier.height(12.dp))
      Text(
        text = appString(R.string.offline_friend_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
      Spacer(Modifier.height(20.dp))

      if (!viewModel.hasGooglePlayServices()) {
        Text(
            text = appString(R.string.offline_friend_play_services_missing),
            color = MaterialTheme.colorScheme.error
        )
        return@Column
      }

      when (state.phase) {
        OfflinePairingPhase.IDLE -> {
            Button(
                onClick = { runWithPermissions(viewModel::hostOfflinePairing) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appString(R.string.offline_friend_be_visible))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { runWithPermissions(viewModel::searchOfflineFriends) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appString(R.string.offline_friend_search))
            }
        }

        OfflinePairingPhase.ADVERTISING -> PairingProgress(
            text = appString(R.string.offline_friend_waiting),
            onCancel = viewModel::stopOfflinePairing
        )

        OfflinePairingPhase.DISCOVERING -> {
            Text(appString(R.string.offline_friend_searching))
            Spacer(Modifier.height(12.dp))
            if (state.discoveredEndpoints.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.discoveredEndpoints.forEach { endpoint ->
                Button(
                    onClick = { viewModel.connectOfflineFriend(endpoint) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(appString(R.string.offline_friend_connect_to, endpoint.name))
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = viewModel::stopOfflinePairing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appString(R.string.cancel))
            }
        }

        OfflinePairingPhase.REQUESTING -> PairingProgress(
            text = appString(R.string.offline_friend_requesting),
            onCancel = viewModel::stopOfflinePairing
        )

        OfflinePairingPhase.AWAITING_CONFIRMATION -> {
            val pending = state.pendingConnection
            Text(
                text = appString(
                    R.string.offline_friend_confirm_person,
                    pending?.endpoint?.name.orEmpty()
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(appString(R.string.offline_friend_compare_code))
            Text(
                text = pending?.authenticationDigits.orEmpty(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::acceptOfflineFriend,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appString(R.string.offline_friend_code_matches))
            }
            OutlinedButton(
                onClick = viewModel::rejectOfflineFriend,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appString(R.string.reject))
            }
        }

        OfflinePairingPhase.CONNECTING,
        OfflinePairingPhase.EXCHANGING_PROFILE -> PairingProgress(
            text = appString(R.string.offline_friend_exchanging),
            onCancel = viewModel::stopOfflinePairing
        )

        OfflinePairingPhase.PAIRED -> {
            Text(
                text = appString(
                    R.string.offline_friend_added,
                    state.pairedFriendName.orEmpty()
                ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.done))
            }
        }

        OfflinePairingPhase.ERROR -> {
            Text(
                text = state.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::stopOfflinePairing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(appString(R.string.try_again))
            }
        }
      }

      Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PairingProgress(
    text: String,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(text)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onCancel) {
            Text(appString(R.string.cancel))
        }
    }
}

@Composable
fun FriendSearchView(
    viewModel: FriendViewModel
) {

    var search by rememberSaveable {
        mutableStateOf("")
    }
    Column {
        Row {
            TextField(
                value = search,
                onValueChange = {
                    search = it
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = {
                    Text(appString(R.string.friend_name))
                }
            )

            Spacer(Modifier.width(12.dp))

            Button(
                enabled = !viewModel.isSearching,
                onClick = {
                    viewModel.searchFriends(search)
                }
            ) {
                Icon(
                    Icons.Outlined.Search,
                    null
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (viewModel.isSearching) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        viewModel.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn {
            items(viewModel.searchResults) { friend ->
                FriendSearchItem(
                    friend = friend,
                    onAdd = {
                        viewModel.sendFriendRequest(friend)
                    }
                )
                HorizontalDivider(
                    color = Color.LightGray.copy(alpha = .3f),
                    modifier = Modifier.padding(start = 50.dp)
                )
            }
        }
    }
}

@Composable
fun FriendSearchItem(
    friend: Friend,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FriendAvatar(friend = friend, modifier = Modifier.size(44.dp))

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = friend.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = friend.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FilledTonalButton(
            onClick = onAdd
        ) {
            Icon(
                Icons.Outlined.PersonAdd,
                null,
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(appString(R.string.add))
        }
    }
}

@Composable
fun FriendQrView(viewModel: FriendViewModel) {
    val context = LocalContext.current
    val userId = viewModel.currentUserId
    val sizePx = with(LocalDensity.current) { 220.dp.roundToPx() }
    val qrBitmap = remember(userId, sizePx) {
        userId?.let { createFriendQrBitmap(it, sizePx) }
    }
    val scanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }
    var isScanning by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    val invalidMessage = appString(R.string.invalid_friend_qr)
    val ownCodeMessage = appString(R.string.own_friend_qr)
    val successMessage = appString(R.string.friend_request_sent)
    val scannerFailedMessage = appString(R.string.qr_scanner_failed)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = appString(R.string.my_friend_qr),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))

        if (qrBitmap != null) {
            Card(shape = RoundedCornerShape(24.dp)) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = appString(R.string.my_friend_qr),
                    modifier = Modifier.size(220.dp)
                )
            }
        } else {
            Text(
                text = appString(R.string.qr_user_missing),
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = appString(R.string.my_friend_qr_description),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))

        Button(
            enabled = !isScanning && !viewModel.isLoading && userId != null,
            onClick = {
                isScanning = true
                scanMessage = null
                viewModel.clearError()
                scanner.startScan()
                    .addOnSuccessListener { barcode ->
                        isScanning = false
                        val scannedId = FriendQrPayload.parse(barcode.rawValue)
                        when {
                            scannedId == null -> scanMessage = invalidMessage
                            scannedId == userId -> scanMessage = ownCodeMessage
                            else -> viewModel.sendFriendRequest(scannedId) {
                                scanMessage = successMessage
                            }
                        }
                    }
                    .addOnCanceledListener { isScanning = false }
                    .addOnFailureListener {
                        isScanning = false
                        scanMessage = it.localizedMessage ?: scannerFailedMessage
                    }
            }
        ) {
            if (isScanning || viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(appString(R.string.scan_friend_qr))
            }
        }

        scanMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        viewModel.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

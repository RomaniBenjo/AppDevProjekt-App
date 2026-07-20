package com.example.commingsoon.overlays

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.commingsoon.viewmodels.Friend
import com.example.commingsoon.viewmodels.FriendViewModel
import com.example.commingsoon.R
import com.example.commingsoon.components.FriendAvatar
import com.example.commingsoon.language.appString
import com.example.commingsoon.friends.FriendQrPayload
import com.example.commingsoon.friends.createFriendQrBitmap
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

            Text(
                text = appString(R.string.new_friend),
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = appString(R.string.search_friend_or_qr),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

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

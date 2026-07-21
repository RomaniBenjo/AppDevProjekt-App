package com.example.comingsoon.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.comingsoon.BuildConfig
import com.example.comingsoon.navigation.NavScreens
import com.example.comingsoon.R
import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.language.appString
import com.example.comingsoon.ui.screens.onlineopenguesser.OpenGuesserApiClient
import com.example.comingsoon.ui.screens.onlineopenguesser.OpenGuesserGame
import com.example.comingsoon.ui.screens.onlineopenguesser.OpenGuesserSocketEvent
import com.example.comingsoon.ui.screens.onlineopenguesser.StreetViewPanorama
import kotlinx.coroutines.launch

@Composable
fun OpenGuesserScreen(navController: NavHostController) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .heightIn(min = maxHeight - 40.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = appString(R.string.how_to_play),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = appString(R.string.online_or_local),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(28.dp))

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OpenGuesserModeCard(
                        title = appString(R.string.online_guesser),
                        description = appString(R.string.online_guesser_text),
                        icon = Icons.Default.Cloud,
                        onClick = { navController.navigate(NavScreens.OpenGuesserOnline.route) },
                        modifier = Modifier.weight(1f)
                    )
                    OpenGuesserModeCard(
                        title = appString(R.string.local_guesser),
                        description = appString(R.string.local_guesser_text),
                        icon = Icons.Default.Map,
                        onClick = { navController.navigate(NavScreens.OpenGuesserLocal.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OpenGuesserModeCard(
                    title = appString(R.string.online_guesser),
                    description = appString(R.string.online_guesser_text),
                    icon = Icons.Default.Cloud,
                    onClick = { navController.navigate(NavScreens.OpenGuesserOnline.route) }
                )
                Spacer(Modifier.height(16.dp))
                OpenGuesserModeCard(
                    title = appString(R.string.local_guesser),
                    description = appString(R.string.local_guesser_text),
                    icon = Icons.Default.Map,
                    onClick = { navController.navigate(NavScreens.OpenGuesserLocal.route) }
                )
            }
        }
    }
}

@Composable
private fun OpenGuesserModeCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.padding(start = 20.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun OnlineOpenGuesserScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionStore = remember(context) { AuthSessionStore(context.applicationContext) }
    val apiClient = remember { OpenGuesserApiClient(BuildConfig.API_BASE_URL) }
    var state by remember { mutableStateOf<OnlineGuesserUiState>(OnlineGuesserUiState.Loading) }

    DisposableEffect(apiClient) {
        onDispose { apiClient.close() }
    }

    fun refreshLobbies(showLoading: Boolean = true) {
        coroutineScope.launch {
            if (showLoading) state = OnlineGuesserUiState.Loading
            val accessToken = sessionStore.load()?.accessToken
            state = if (accessToken == null) {
                OnlineGuesserUiState.Error(context.getString(R.string.online_guesser_sign_in_again))
            } else {
                runCatching { apiClient.listLobbies(accessToken) }
                    .fold(
                        onSuccess = { OnlineGuesserUiState.Browser(it) },
                        onFailure = {
                            OnlineGuesserUiState.Error(
                                it.localizedMessage
                                    ?: context.getString(R.string.online_guesser_server_error)
                            )
                        }
                    )
            }
        }
    }

    fun runLobbyRequest(request: suspend (String) -> OpenGuesserGame) {
        coroutineScope.launch {
            state = OnlineGuesserUiState.Loading
            val accessToken = sessionStore.load()?.accessToken
            state = if (accessToken == null) {
                OnlineGuesserUiState.Error(context.getString(R.string.online_guesser_sign_in_again))
            } else {
                runCatching { request(accessToken) }
                    .fold(
                        onSuccess = { game ->
                            if (game.status == "lobby") {
                                OnlineGuesserUiState.Lobby(game)
                            } else {
                                OnlineGuesserUiState.Game(game)
                            }
                        },
                        onFailure = {
                            OnlineGuesserUiState.Error(
                                it.localizedMessage
                                    ?: context.getString(R.string.online_guesser_server_error)
                            )
                        }
                    )
            }
        }
    }

    fun leaveLobby(gameId: String) {
        coroutineScope.launch {
            state = OnlineGuesserUiState.Loading
            val accessToken = sessionStore.load()?.accessToken
            if (accessToken == null) {
                state = OnlineGuesserUiState.Error(
                    context.getString(R.string.online_guesser_sign_in_again)
                )
                return@launch
            }
            state = runCatching {
                apiClient.leaveLobby(gameId, accessToken)
                apiClient.listLobbies(accessToken)
            }.fold(
                onSuccess = { OnlineGuesserUiState.Browser(it) },
                onFailure = {
                    OnlineGuesserUiState.Error(
                        it.localizedMessage
                            ?: context.getString(R.string.online_guesser_server_error)
                    )
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        val accessToken = sessionStore.load()?.accessToken
        if (accessToken == null) {
            state = OnlineGuesserUiState.Error(
                context.getString(R.string.online_guesser_sign_in_again)
            )
            return@LaunchedEffect
        }
        state = runCatching { apiClient.listLobbies(accessToken) }
            .fold(
                onSuccess = { OnlineGuesserUiState.Browser(it) },
                onFailure = {
                    OnlineGuesserUiState.Error(
                        it.localizedMessage
                            ?: context.getString(R.string.online_guesser_server_error)
                    )
                }
            )

        apiClient.events.collect { event ->
            when (event) {
                is OpenGuesserSocketEvent.LobbiesUpdated -> {
                    if (state is OnlineGuesserUiState.Browser) {
                        state = OnlineGuesserUiState.Browser(event.lobbies)
                    }
                }
                is OpenGuesserSocketEvent.GameUpdated -> {
                    val current = state
                    if (current is OnlineGuesserUiState.Lobby && current.game.id == event.game.id) {
                        state = if (event.game.status == "lobby") {
                            OnlineGuesserUiState.Lobby(event.game)
                        } else {
                            OnlineGuesserUiState.Game(event.game)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val currentState = state) {
            OnlineGuesserUiState.Loading -> OnlineGuesserMessage(
                message = appString(R.string.online_guesser_starting),
                showProgress = true
            )
            is OnlineGuesserUiState.Error -> OnlineGuesserMessage(
                message = currentState.message,
                actionLabel = appString(R.string.online_guesser_retry),
                onAction = { refreshLobbies() }
            )
            is OnlineGuesserUiState.Browser -> OnlineGuesserLobbyBrowser(
                lobbies = currentState.lobbies,
                currentUserId = sessionStore.load()?.user?.id,
                onCreateLobby = {
                    runLobbyRequest { token -> apiClient.createLobby(token) }
                },
                onJoinLobby = { gameId ->
                    runLobbyRequest { token -> apiClient.joinLobby(gameId, token) }
                },
                onRefresh = { refreshLobbies() }
            )
            is OnlineGuesserUiState.Lobby -> OnlineGuesserLobbyView(
                lobby = currentState.game,
                currentUserId = sessionStore.load()?.user?.id,
                onStart = {
                    runLobbyRequest { token ->
                        apiClient.startLobby(currentState.game.id, token)
                    }
                },
                onLeave = { leaveLobby(currentState.game.id) }
            )
            is OnlineGuesserUiState.Game -> OnlineGuesserGameView(
                game = currentState.game,
                onBrowseLobbies = { refreshLobbies() }
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 4.dp
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = appString(R.string.back)
                )
            }
        }
    }
}

@Composable
private fun OnlineGuesserLobbyBrowser(
    lobbies: List<OpenGuesserGame>,
    currentUserId: Long?,
    onCreateLobby: () -> Unit,
    onJoinLobby: (String) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 80.dp,
            end = 20.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                appString(R.string.online_guesser_lobbies),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(6.dp))
            Text(
                appString(R.string.online_guesser_lobbies_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onCreateLobby, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Groups, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(appString(R.string.online_guesser_host_lobby))
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = appString(R.string.online_guesser_refresh_lobbies)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (lobbies.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Text(
                        appString(R.string.online_guesser_no_lobbies),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(lobbies, key = { it.id }) { lobby ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            lobby.host.displayName(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            appString(
                                R.string.online_guesser_player_count,
                                lobby.participants.size
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { onJoinLobby(lobby.id) }) {
                        Text(
                            appString(
                                if (lobby.host.id == currentUserId) {
                                    R.string.online_guesser_open_lobby
                                } else {
                                    R.string.online_guesser_join_lobby
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineGuesserLobbyView(
    lobby: OpenGuesserGame,
    currentUserId: Long?,
    onStart: () -> Unit,
    onLeave: () -> Unit
) {
    val isHost = lobby.host.id == currentUserId
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 82.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Groups,
            contentDescription = null,
            modifier = Modifier.size(58.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            appString(R.string.online_guesser_lobby_title, lobby.host.displayName()),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isHost) {
                appString(R.string.online_guesser_host_waiting)
            } else {
                appString(R.string.online_guesser_guest_waiting)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    appString(R.string.online_guesser_players),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(10.dp))
                lobby.participants.forEach { participant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Text(
                            participant.displayName(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )
                        if (participant.id == lobby.host.id) {
                            Text(
                                appString(R.string.online_guesser_host),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        if (isHost) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(appString(R.string.online_guesser_start_game))
            }
        } else {
            CircularProgressIndicator()
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
            Text(
                appString(
                    if (isHost) {
                        R.string.online_guesser_close_lobby
                    } else {
                        R.string.online_guesser_leave_lobby
                    }
                )
            )
        }
    }
}

@Composable
private fun OnlineGuesserGameView(
    game: OpenGuesserGame,
    onBrowseLobbies: () -> Unit
) {
    var viewerState by remember(game.id) {
        mutableStateOf(StreetViewState.Loading)
    }

    Box(Modifier.fillMaxSize()) {
        val panorama = game.panorama
        if (BuildConfig.GOOGLE_MAPS_API_KEY_CONFIGURED && panorama != null) {
            StreetViewPanorama(
                latitude = panorama.latitude,
                longitude = panorama.longitude,
                onPanoramaLoaded = { viewerState = StreetViewState.Ready }
            )
        } else if (panorama == null) {
            OnlineGuesserMessage(
                message = appString(R.string.online_guesser_panorama_missing)
            )
        } else {
            OnlineGuesserMessage(
                message = appString(R.string.online_guesser_maps_key_missing)
            )
        }

        if (BuildConfig.GOOGLE_MAPS_API_KEY_CONFIGURED &&
            panorama != null &&
            viewerState == StreetViewState.Loading
        ) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 4.dp
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Text(
                    appString(R.string.online_guesser),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    appString(
                        R.string.online_guesser_round,
                        game.roundNumber,
                        game.totalRounds
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    appString(R.string.online_guesser_player_count, game.participants.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Button(
            onClick = onBrowseLobbies,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
                .height(54.dp)
        ) {
            Icon(Icons.Default.Groups, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(appString(R.string.online_guesser_browse_lobbies))
        }
    }
}

@Composable
private fun OnlineGuesserMessage(
    message: String,
    showProgress: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().padding(28.dp)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showProgress) {
                CircularProgressIndicator()
                Spacer(Modifier.height(20.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(
                text = message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private sealed interface OnlineGuesserUiState {
    data object Loading : OnlineGuesserUiState
    data class Error(val message: String) : OnlineGuesserUiState
    data class Browser(val lobbies: List<OpenGuesserGame>) : OnlineGuesserUiState
    data class Lobby(val game: OpenGuesserGame) : OnlineGuesserUiState
    data class Game(val game: OpenGuesserGame) : OnlineGuesserUiState
}

private fun com.example.comingsoon.auth.AuthenticatedUser.displayName(): String =
    name?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')

private enum class StreetViewState {
    Loading,
    Ready
}

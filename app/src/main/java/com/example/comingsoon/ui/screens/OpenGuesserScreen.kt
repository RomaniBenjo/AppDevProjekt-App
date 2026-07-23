package com.example.comingsoon.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.example.comingsoon.BuildConfig
import com.example.comingsoon.navigation.NavScreens
import com.example.comingsoon.R
import com.example.comingsoon.auth.AuthSessionStore
import com.example.comingsoon.language.appString
import com.example.comingsoon.ui.screens.onlineopenguesser.OpenGuesserApiClient
import com.example.comingsoon.ui.screens.onlineopenguesser.OpenGuesserGame
import com.example.comingsoon.ui.screens.onlineopenguesser.OpenGuesserPanorama
import com.example.comingsoon.ui.screens.onlineopenguesser.OpenGuesserRoundResult
import com.example.comingsoon.ui.screens.onlineopenguesser.OpenGuesserSocketEvent
import com.example.comingsoon.ui.screens.onlineopenguesser.StreetViewPanorama
import com.example.comingsoon.ui.screens.openguesser.OpenGuesserMap
import com.example.comingsoon.ui.screens.openguesser.OpenGuesserResultMarker
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.maplibre.android.geometry.LatLng
import java.time.OffsetDateTime

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

    fun runGameUpdate(request: suspend (String) -> OpenGuesserGame) {
        coroutineScope.launch {
            val accessToken = sessionStore.load()?.accessToken ?: return@launch
            runCatching { request(accessToken) }
                .onSuccess { state = OnlineGuesserUiState.Game(it) }
        }
    }

    fun leaveGameAndExit(gameId: String) {
        coroutineScope.launch {
            sessionStore.load()?.accessToken?.let { token ->
                runCatching { apiClient.leaveGame(gameId, token) }
            }
            navController.popBackStack()
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
                is OpenGuesserSocketEvent.GameClosed -> {
                    val current = state
                    if (current is OnlineGuesserUiState.Game && current.game.id == event.gameId) {
                        refreshLobbies(showLoading = false)
                    }
                }
                is OpenGuesserSocketEvent.GameUpdated -> {
                    val current = state
                    val currentGameId = when (current) {
                        is OnlineGuesserUiState.Lobby -> current.game.id
                        is OnlineGuesserUiState.Game -> current.game.id
                        else -> null
                    }
                    if (currentGameId == event.game.id) {
                        state = if (event.game.status == "lobby") OnlineGuesserUiState.Lobby(event.game)
                        else OnlineGuesserUiState.Game(event.game)
                    }
                }
            }
        }
    }

    val activeGame = (state as? OnlineGuesserUiState.Game)?.game
        ?.takeIf { it.status != "finished" }
    BackHandler(enabled = activeGame != null) {
        activeGame?.let { leaveGameAndExit(it.id) }
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
                onStart = { roundCount, roundSeconds, locationFilter ->
                    runLobbyRequest { token ->
                        apiClient.startLobby(
                            currentState.game.id, roundCount, roundSeconds, locationFilter, token
                        )
                    }
                },
                onLeave = { leaveLobby(currentState.game.id) }
            )
            is OnlineGuesserUiState.Game -> OnlineGuesserGameView(
                game = currentState.game,
                currentUserId = sessionStore.load()?.user?.id,
                onSubmitGuess = { guess ->
                    runLobbyRequest { token ->
                        apiClient.submitGuess(
                            currentState.game.id, guess.latitude, guess.longitude, token
                        )
                    }
                },
                onNextRound = {
                    runLobbyRequest { token -> apiClient.nextRound(currentState.game.id, token) }
                },
                onUnavailableLocation = { location ->
                    runGameUpdate { token ->
                        apiClient.replaceUnavailableLocation(
                            currentState.game.id, location.latitude, location.longitude, token
                        )
                    }
                },
                onTimerExpired = {
                    runGameUpdate { token ->
                        apiClient.getGame(currentState.game.id, token)
                    }
                },
                onExit = { refreshLobbies() }
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
                onClick = {
                    val current = state
                    if (current is OnlineGuesserUiState.Game && current.game.status != "finished") {
                        leaveGameAndExit(current.game.id)
                    } else {
                        navController.popBackStack()
                    }
                },
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
    onStart: (Int, Int, String) -> Unit,
    onLeave: () -> Unit
) {
    val isHost = lobby.host.id == currentUserId
    var roundCount by remember(lobby.id) { mutableStateOf(5) }
    var roundSeconds by remember(lobby.id) { mutableStateOf(60) }
    var locationFilter by remember(lobby.id) { mutableStateOf("world") }
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
            Text(
                appString(R.string.online_guesser_round_count),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(3, 5, 10).forEach { count ->
                    FilterChip(
                        selected = roundCount == count,
                        onClick = { roundCount = count },
                        label = { Text(appString(R.string.online_guesser_round_count_option, count)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                appString(R.string.online_guesser_time_limit),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(30, 60, 120).forEach { seconds ->
                    FilterChip(
                        selected = roundSeconds == seconds,
                        onClick = { roundSeconds = seconds },
                        label = { Text(appString(R.string.online_guesser_time_option, seconds)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                appString(R.string.online_guesser_location_filter),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            OnlineLocationFilters(
                selected = locationFilter,
                onSelected = { locationFilter = it }
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onStart(roundCount, roundSeconds, locationFilter) },
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
    currentUserId: Long?,
    onSubmitGuess: (LatLng) -> Unit,
    onNextRound: () -> Unit,
    onUnavailableLocation: (OpenGuesserPanorama) -> Unit,
    onTimerExpired: () -> Unit,
    onExit: () -> Unit
) {
    if (game.status == "finished") {
        OnlineGuesserEndScreen(game = game, currentUserId = currentUserId, onExit = onExit)
        return
    }
    var viewerState by remember(
        game.id, game.roundNumber, game.panorama?.latitude, game.panorama?.longitude
    ) {
        mutableStateOf(StreetViewState.Loading)
    }
    var panoramaRetries by remember(game.id, game.roundNumber) { mutableStateOf(0) }
    var reportedUnavailable by remember(game.id, game.roundNumber) {
        mutableStateOf<String?>(null)
    }
    var activeView by remember(game.id, game.roundNumber) {
        mutableStateOf(OnlineRoundView.STREET_VIEW)
    }
    var selectedGuess by remember(game.id, game.roundNumber) {
        mutableStateOf<LatLng?>(null)
    }
    val isHost = game.host.id == currentUserId
    val ownScore = game.scores.firstOrNull { it.user.id == currentUserId }
    val shownGuess = if (game.roundComplete) {
        ownScore?.guess?.let { LatLng(it.latitude, it.longitude) }
    } else selectedGuess
    val actualLocation = game.actualLocation?.let { LatLng(it.latitude, it.longitude) }
    val resultMarkers = if (game.roundComplete) game.scores.mapNotNull { score ->
        score.guess?.let { guess ->
            OpenGuesserResultMarker(
                location = LatLng(guess.latitude, guess.longitude),
                label = score.user.displayName()
            )
        }
    } else emptyList()
    var secondsRemaining by remember(game.id, game.roundNumber) {
        mutableStateOf(game.roundSeconds)
    }
    LaunchedEffect(game.id, game.roundNumber, game.roundStartedAt, game.roundComplete) {
        if (game.roundComplete || game.roundStartedAt == null) return@LaunchedEffect
        val deadline = runCatching {
            OffsetDateTime.parse(game.roundStartedAt).toInstant().toEpochMilli() +
                game.roundSeconds * 1_000L
        }.getOrNull() ?: return@LaunchedEffect
        while (true) {
            secondsRemaining = ((deadline - System.currentTimeMillis() + 999L) / 1_000L)
                .coerceAtLeast(0L).toInt()
            if (secondsRemaining == 0) {
                onTimerExpired()
                break
            }
            delay(250)
        }
    }

    Box(Modifier.fillMaxSize()) {
        val panorama = game.panorama
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (activeView == OnlineRoundView.STREET_VIEW) 1f else 0f)
                .zIndex(if (activeView == OnlineRoundView.STREET_VIEW) 1f else 0f)
        ) {
            if (BuildConfig.GOOGLE_MAPS_API_KEY_CONFIGURED && panorama != null) {
                StreetViewPanorama(
                    latitude = panorama.latitude,
                    longitude = panorama.longitude,
                    panoId = panorama.panoId,
                    isVisible = activeView == OnlineRoundView.STREET_VIEW,
                    onPanoramaLoaded = { viewerState = StreetViewState.Ready },
                    onPanoramaUnavailable = {
                        viewerState = StreetViewState.Unavailable
                        val key = "${panorama.latitude},${panorama.longitude}"
                        if (reportedUnavailable != key && panoramaRetries < 5) {
                            reportedUnavailable = key
                            panoramaRetries += 1
                            onUnavailableLocation(panorama)
                        }
                    }
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
            if (viewerState == StreetViewState.Unavailable) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (panoramaRetries < 5) CircularProgressIndicator()
                        Text(
                            appString(
                                if (panoramaRetries < 5) R.string.online_guesser_finding_street_view
                                else R.string.online_guesser_street_view_unavailable
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (activeView == OnlineRoundView.MAP) 1f else 0f)
                .zIndex(if (activeView == OnlineRoundView.MAP) 1f else 0f)
        ) {
            OpenGuesserMap(
                archiveUrl = { "pmtiles://${BuildConfig.ONLINE_MAP_URL}" },
                selectedLocation = if (game.roundComplete) null else shownGuess,
                onLocationSelected = { selectedGuess = it },
                mapLoadError = appString(R.string.online_guesser_map_load_failed),
                guessMarkerTitle = appString(R.string.local_guesser_your_guess),
                actualMarkerTitle = appString(R.string.local_guesser_real_location),
                actualLocation = actualLocation,
                resultMarkers = resultMarkers,
                isGuessingEnabled = secondsRemaining > 0 && !game.currentUserSubmitted && !game.roundComplete,
                modifier = Modifier.fillMaxSize()
            )
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
                if (!game.roundComplete) {
                    Text(
                        appString(R.string.online_guesser_time_remaining, secondsRemaining),
                        color = if (secondsRemaining <= 10) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
                .zIndex(3f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        if (activeView == OnlineRoundView.MAP && secondsRemaining > 0 && !game.currentUserSubmitted && !game.roundComplete) {
            Button(
                onClick = { selectedGuess?.let(onSubmitGuess) },
                enabled = selectedGuess != null,
                modifier = Modifier.height(54.dp)
            ) {
                Text(appString(R.string.online_guesser_submit_guess))
            }
            Spacer(Modifier.height(8.dp))
        }
        if (game.currentUserSubmitted && !game.roundComplete) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Text(
                    appString(R.string.online_guesser_waiting_for_guesses),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (game.roundComplete) {
            OnlineRoundResults(game = game)
            if (isHost) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onNextRound, modifier = Modifier.height(50.dp)) {
                    Text(
                        appString(
                            if (game.roundNumber >= game.totalRounds) R.string.online_guesser_finish_game
                            else R.string.online_guesser_next_round
                        )
                    )
                }
            } else if (game.status != "finished") {
                Text(appString(R.string.online_guesser_waiting_for_host))
            }
            Spacer(Modifier.height(8.dp))
        }
        if (game.status != "finished") Button(
            onClick = {
                activeView = if (activeView == OnlineRoundView.STREET_VIEW) {
                    OnlineRoundView.MAP
                } else {
                    OnlineRoundView.STREET_VIEW
                }
            },
            modifier = Modifier.height(54.dp)
        ) {
            Icon(
                imageVector = if (activeView == OnlineRoundView.STREET_VIEW) {
                    Icons.Default.Map
                } else {
                    Icons.Default.Streetview
                },
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(
                appString(
                    if (activeView == OnlineRoundView.STREET_VIEW) {
                        R.string.online_guesser_guess
                    } else {
                        R.string.online_guesser_street_view
                    }
                )
            )
        }
        }
    }
}

@Composable
private fun OnlineLocationFilters(
    selected: String,
    onSelected: (String) -> Unit
) {
    val filters = listOf(
        "world" to R.string.online_guesser_region_world,
        "europe" to R.string.online_guesser_region_europe,
        "north_america" to R.string.online_guesser_region_north_america,
        "south_america" to R.string.online_guesser_region_south_america,
        "asia" to R.string.online_guesser_region_asia,
        "africa" to R.string.online_guesser_region_africa,
        "oceania" to R.string.online_guesser_region_oceania,
        "austria" to R.string.online_guesser_country_austria,
        "germany" to R.string.online_guesser_country_germany,
        "france" to R.string.online_guesser_country_france,
        "italy" to R.string.online_guesser_country_italy,
        "united_kingdom" to R.string.online_guesser_country_uk,
        "spain" to R.string.online_guesser_country_spain,
        "portugal" to R.string.online_guesser_country_portugal,
        "norway" to R.string.online_guesser_country_norway,
        "sweden" to R.string.online_guesser_country_sweden,
        "poland" to R.string.online_guesser_country_poland,
        "united_states" to R.string.online_guesser_country_usa,
        "canada" to R.string.online_guesser_country_canada,
        "mexico" to R.string.online_guesser_country_mexico,
        "brazil" to R.string.online_guesser_country_brazil,
        "argentina" to R.string.online_guesser_country_argentina,
        "chile" to R.string.online_guesser_country_chile,
        "colombia" to R.string.online_guesser_country_colombia,
        "japan" to R.string.online_guesser_country_japan,
        "south_korea" to R.string.online_guesser_country_south_korea,
        "india" to R.string.online_guesser_country_india,
        "thailand" to R.string.online_guesser_country_thailand,
        "indonesia" to R.string.online_guesser_country_indonesia,
        "south_africa" to R.string.online_guesser_country_south_africa,
        "botswana" to R.string.online_guesser_country_botswana,
        "ghana" to R.string.online_guesser_country_ghana,
        "australia" to R.string.online_guesser_country_australia,
        "new_zealand" to R.string.online_guesser_country_new_zealand
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        filters.chunked(2).forEach { rowFilters ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowFilters.forEach { (key, label) ->
                    FilterChip(
                        selected = selected == key,
                        onClick = { onSelected(key) },
                        label = { Text(appString(label)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowFilters.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OnlineGuesserEndScreen(
    game: OpenGuesserGame,
    currentUserId: Long?,
    onExit: () -> Unit
) {
    val leaders = game.scores.sortedByDescending { it.totalPoints }
    val topPoints = leaders.firstOrNull()?.totalPoints
    val winners = leaders.filter { it.totalPoints == topPoints }
    val headline = when {
        winners.isEmpty() -> appString(R.string.online_guesser_game_complete)
        winners.size > 1 -> appString(R.string.online_guesser_tie_game)
        winners.first().user.id == currentUserId -> appString(R.string.online_guesser_you_win)
        else -> appString(R.string.online_guesser_player_wins, winners.first().user.displayName())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, top = 82.dp, end = 20.dp, bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        appString(R.string.online_guesser_game_complete),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        headline,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Text(
                appString(R.string.online_guesser_final_standings),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(leaders, key = { it.user.id }) { score ->
            val isWinner = score.totalPoints == topPoints
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWinner) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer
                ),
                border = if (isWinner) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "${leaders.indexOf(score) + 1}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isWinner) Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        if (score.user.id == currentUserId) appString(R.string.online_guesser_you)
                        else score.user.displayName(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        appString(R.string.online_guesser_points, score.totalPoints),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Text(
                appString(R.string.online_guesser_points_by_round),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(game.roundResults, key = OpenGuesserRoundResult::roundNumber) { result ->
            OnlineFinishedRoundCard(game = game, result = result, currentUserId = currentUserId)
        }
        item {
            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text(appString(R.string.online_guesser_back_to_lobbies))
            }
        }
    }
}

@Composable
private fun OnlineFinishedRoundCard(
    game: OpenGuesserGame,
    result: OpenGuesserRoundResult,
    currentUserId: Long?
) {
    val bestPoints = result.scores.maxOfOrNull { it.roundPoints ?: 0 }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                appString(R.string.online_guesser_round_title, result.roundNumber),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            result.scores.forEach { score ->
                val runningTotal = game.roundResults
                    .filter { it.roundNumber <= result.roundNumber }
                    .sumOf { round ->
                        round.scores.firstOrNull { it.user.id == score.user.id }?.roundPoints ?: 0
                    }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                if (score.user.id == currentUserId) appString(R.string.online_guesser_you)
                                else score.user.displayName(),
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (score.roundPoints == bestPoints) Icon(
                                Icons.Rounded.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        score.distanceKm?.let {
                            Text(
                                appString(R.string.online_guesser_distance, it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "+${score.roundPoints ?: 0}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            appString(R.string.online_guesser_running_total, runningTotal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineRoundResults(game: OpenGuesserGame) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                appString(
                    if (game.status == "finished") R.string.online_guesser_final_results
                    else R.string.online_guesser_round_results
                ),
                style = MaterialTheme.typography.titleMedium
            )
            game.scores.forEachIndexed { index, score ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${index + 1}. ${score.user.displayName()}", modifier = Modifier.weight(1f))
                    score.distanceKm?.let {
                        Text(appString(R.string.online_guesser_distance, it))
                    }
                    Text(appString(R.string.online_guesser_points, score.totalPoints))
                }
            }
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
    Ready,
    Unavailable
}

private enum class OnlineRoundView {
    STREET_VIEW,
    MAP
}

package `in`.coinslive.live.ui.adhyayan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveFormat
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.ui.components.CenteredLoading
import `in`.coinslive.live.ui.room.MediaControlBar
import `in`.coinslive.live.ui.room.ParticipantGrid
import `in`.coinslive.live.ui.room.ParticipantStage
import `in`.coinslive.live.ui.room.participantLabel
import `in`.coinslive.live.util.rememberMediaPermissionState
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import kotlinx.coroutines.launch

@Composable
fun AdhyayanRoomScreen(
    onExit: () -> Unit,
    viewModel: AdhyayanRoomViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val permissions = rememberMediaPermissionState()

    LaunchedEffect(permissions.resolved) {
        if (!permissions.resolved) return@LaunchedEffect
        if (!permissions.micGranted && !permissions.cameraGranted) {
            viewModel.onMediaPermissionDenied()
        }
        viewModel.start(
            enableMic = permissions.micGranted,
            enableCamera = permissions.cameraGranted,
        )
    }

    val leave: () -> Unit = { onExit() }
    BackHandler(onBack = leave)

    CoinsLiveTheme {
        val room = state.room
        when {
            !permissions.resolved || room == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            state.title ?: viewModel.slug,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                        if (state.connectionState == "Failed") {
                            Text(state.errorMessage ?: "Could not join", color = Color(0xFFFF8A80))
                        } else {
                            val label = when {
                                !permissions.resolved -> "Checking permissions..."
                                else -> state.connectionState
                            }
                            CenteredLoading(Modifier.height(80.dp), label = label)
                        }
                        TextButton(onClick = leave) { Text("Leave", color = Color.White) }
                    }
                }
            }

            else -> {
                // Room is already connected by the ViewModel; RoomScope only provides composition locals.
                RoomScope(
                    passedRoom = room,
                    connect = false,
                    disconnectOnDispose = false,
                    audio = false,
                    video = false,
                ) {
                    AdhyayanRoomContent(
                        room = room,
                        state = state,
                        viewModel = viewModel,
                        onLeave = leave,
                        onEnd = {
                            scope.launch {
                                if (viewModel.endSession()) onExit()
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdhyayanRoomContent(
    room: Room,
    state: AdhyayanRoomState,
    viewModel: AdhyayanRoomViewModel,
    onLeave: () -> Unit,
    onEnd: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val tracks by rememberTracks(
        sources = listOf(Track.Source.CAMERA, Track.Source.SCREEN_SHARE),
        usePlaceholders = setOf(Track.Source.CAMERA),
    )
    val cameraTracks = tracks.filter { it.source == Track.Source.CAMERA }
    val sharer = tracks.firstOrNull { it.source == Track.Source.SCREEN_SHARE && it.publication != null }
    val roomCount = cameraTracks.mapNotNull {
        runCatching { it.participant.sid.value }.getOrNull()
            ?: runCatching { it.participant.identity?.value }.getOrNull()
    }.distinct().size

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.title ?: viewModel.slug,
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                        )
                        Text(
                            "Adhyayan · ${CoinsLiveFormat.statusLabel(state.status)} · $roomCount",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLeave) { Text("Leave", color = Color(0xFFFF8A80)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CoinsLiveColors.Navy,
                    titleContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            Column {
                state.errorMessage?.let { error ->
                    Text(
                        error,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(8.dp),
                    )
                }
                MediaControlBar(
                    isMicEnabled = state.isMicEnabled,
                    isCameraEnabled = state.isCameraEnabled,
                    isScreenShareEnabled = state.isScreenShareEnabled,
                    canScreenShare = false,
                    showEnd = state.isHost && state.status != "ended",
                    onToggleMic = { scope.launch { viewModel.toggleMic(room) } },
                    onToggleCamera = { scope.launch { viewModel.toggleCamera(room) } },
                    onEnd = onEnd,
                    onLeave = onLeave,
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                sharer != null -> ParticipantStage(
                    tracks = tracks.filter { it.participant == sharer.participant },
                    label = participantLabel(sharer.participant),
                    modifier = Modifier.fillMaxSize(),
                    preferScreenShare = true,
                )

                cameraTracks.isEmpty() -> CenteredLoading(
                    Modifier.fillMaxSize(),
                    label = state.connectionState,
                )

                else -> ParticipantGrid(
                    cameraTracks = cameraTracks,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

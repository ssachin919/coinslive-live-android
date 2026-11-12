package `in`.coinslive.live.ui.webinar

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.coinslive.live.api.HostInboxMessage
import `in`.coinslive.live.api.HostQuestion
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.ui.components.CenteredLoading
import `in`.coinslive.live.ui.room.MediaControlBar
import `in`.coinslive.live.ui.room.ParticipantStage
import `in`.coinslive.live.ui.room.participantLabel
import `in`.coinslive.live.theme.CoinsLiveFormat
import `in`.coinslive.live.util.rememberMediaPermissionState
import io.livekit.android.compose.local.RoomScope
import io.livekit.android.compose.state.rememberTracks
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.track.Track
import kotlinx.coroutines.launch

private enum class SideTab(val label: String) {
    QA("Q&A"),
    MESSAGES("Messages"),
    BROADCASTS("Announce"),
    PEOPLE("People"),
}

@Composable
fun WebinarRoomScreen(
    onExit: () -> Unit,
    viewModel: WebinarRoomViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val permissions = rememberMediaPermissionState(requestOnEnter = state.canPublishMedia)

    LaunchedEffect(permissions.resolved, state.canPublishMedia) {
        // Attendees skip the permission gate; hosts wait until the prompt resolves.
        if (state.canPublishMedia && !permissions.resolved) return@LaunchedEffect
        viewModel.start(
            enableMic = permissions.micGranted,
            enableCamera = permissions.cameraGranted,
        )
    }

    val leaveRoom: () -> Unit = {
        scope.launch {
            viewModel.leave()
            onExit()
        }
    }
    BackHandler(onBack = leaveRoom)

    CoinsLiveTheme {
        val room = state.room
        when {
            room == null -> ConnectingScreen(
                title = state.title ?: viewModel.slug,
                message = state.errorMessage ?: when {
                    state.canPublishMedia && !permissions.resolved -> "Checking permissions..."
                    else -> state.connectionState
                },
                failed = state.connectionState == "Failed",
                onLeave = leaveRoom,
            )

            else -> {
                // Room is already connected by the ViewModel; RoomScope only provides locals.
                RoomScope(
                    passedRoom = room,
                    connect = false,
                    disconnectOnDispose = false,
                    audio = false,
                    video = false,
                ) {
                    WebinarRoomContent(
                        room = room,
                        state = state,
                        viewModel = viewModel,
                        onLeave = leaveRoom,
                        onEnded = {
                            scope.launch {
                                viewModel.endWebinar()
                                onExit()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectingScreen(
    title: String,
    message: String,
    failed: Boolean,
    onLeave: () -> Unit,
) {
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
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            if (failed) {
                Text(message, color = Color(0xFFFF8A80))
            } else {
                CenteredLoading(Modifier.height(80.dp), label = message)
            }
            TextButton(onClick = onLeave) { Text("Leave", color = Color.White) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebinarRoomContent(
    room: Room,
    state: WebinarRoomState,
    viewModel: WebinarRoomViewModel,
    onLeave: () -> Unit,
    onEnded: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSidePanel by remember { mutableStateOf(false) }

    val tracks by rememberTracks(sources = listOf(Track.Source.CAMERA, Track.Source.SCREEN_SHARE))

    // Host sees self; attendees see the first remote publisher (host).
    val stageParticipant = if (state.isHost) {
        tracks.firstOrNull { it.participant is LocalParticipant }?.participant
    } else {
        (tracks.firstOrNull { it.participant !is LocalParticipant }
            ?: tracks.firstOrNull())?.participant
    }
    val stageTracks = tracks.filter { it.participant == stageParticipant }
    val stageLabel = if (state.isHost) "You" else stageParticipant?.let(::participantLabel) ?: "Host"

    val roleLabel = if (state.isHost) "Host" else "Attendee"
    val liveLabel = if (state.isLive) "LIVE" else if (state.isEnded) "Ended" else "Prep"
    val subtitle = "$roleLabel · $liveLabel · ${state.attendeeCount} watching"

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
                        Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                },
                actions = {
                    if (state.isHost) {
                        IconButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, viewModel.shareUrl)
                            }
                            context.startActivity(Intent.createChooser(send, "Share workshop"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                    TextButton(onClick = { showSidePanel = true }) {
                        Text(if (state.isHost) "Tools" else "Chat", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CoinsLiveColors.Navy,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
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
                if (state.canPublishMedia) {
                    MediaControlBar(
                        isMicEnabled = state.isMicEnabled,
                        isCameraEnabled = state.isCameraEnabled,
                        isScreenShareEnabled = false,
                        canScreenShare = false, // TODO: screen share (MediaProjection) on Android.
                        showGoLive = state.canGoLive,
                        showEnd = !state.isEnded,
                        onToggleMic = { scope.launch { viewModel.toggleMic(room) } },
                        onToggleCamera = { scope.launch { viewModel.toggleCamera(room) } },
                        onGoLive = {
                            scope.launch {
                                if (viewModel.goLive()) {
                                    // Let attendees' clients refresh immediately.
                                    runCatching {
                                        room.localParticipant.publishData(
                                            """{"type":"go_live"}""".toByteArray(),
                                            topic = "webinar-engagement",
                                        )
                                    }
                                }
                            }
                        },
                        onEnd = onEnded,
                        onLeave = onLeave,
                    )
                } else {
                    MediaControlBar(
                        isMicEnabled = false,
                        isCameraEnabled = false,
                        isScreenShareEnabled = false,
                        showMediaToggles = false,
                        showEnd = false,
                        onLeave = onLeave,
                    )
                }
            }
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val wide = maxWidth >= 840.dp
            val stage = @Composable { modifier: Modifier ->
                ParticipantStage(
                    tracks = stageTracks,
                    label = stageLabel,
                    modifier = modifier,
                    showCameraPip = state.isHost,
                    placeholder = when {
                        state.isHost -> if (state.isConnected) "Camera is off" else "Connecting…"
                        state.isEnded -> "This workshop has ended."
                        state.isLive -> "Waiting for the host's video…"
                        else -> "Waiting for the host to go live."
                    },
                )
            }

            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    stage(Modifier.weight(1f).fillMaxHeight())
                    CoinsLiveTheme(paper = true) {
                        WebinarSidePanel(state, viewModel, Modifier.width(340.dp).fillMaxHeight())
                    }
                }
            } else {
                stage(Modifier.fillMaxSize())
            }
        }
    }

    if (showSidePanel && !isWideLayout()) {
        CoinsLiveTheme(paper = true) {
            ModalBottomSheet(
                onDismissRequest = { showSidePanel = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                WebinarSidePanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp),
                )
            }
        }
    }
}

@Composable
private fun isWideLayout(): Boolean =
    androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 840

// region Side panel

@Composable
private fun WebinarSidePanel(
    state: WebinarRoomState,
    viewModel: WebinarRoomViewModel,
    modifier: Modifier = Modifier,
) {
    val tabs = if (state.isHost) SideTab.entries.toList() else listOf(SideTab.QA, SideTab.MESSAGES)
    var selected by remember { mutableStateOf(SideTab.QA) }
    if (selected !in tabs) selected = tabs.first()

    Column(modifier = modifier.background(CoinsLiveColors.Paper)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEach { tab ->
                FilterChip(
                    selected = selected == tab,
                    onClick = { selected = tab },
                    label = { Text(tab.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CoinsLiveColors.Brand,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }

        when (selected) {
            SideTab.QA -> QuestionsPanel(state, viewModel, Modifier.weight(1f))
            SideTab.MESSAGES -> MessagesPanel(state, viewModel, Modifier.weight(1f))
            SideTab.BROADCASTS -> BroadcastsPanel(state, viewModel, Modifier.weight(1f))
            SideTab.PEOPLE -> PeoplePanel(state, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuestionsPanel(state: WebinarRoomState, viewModel: WebinarRoomViewModel, modifier: Modifier) {
    Column(modifier) {
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
            if (state.questions.isEmpty()) {
                item {
                    EmptyHint(if (state.isHost) "No questions yet." else "Ask a question.")
                }
            }
            items(state.questions, key = { it.id }) { question ->
                QuestionItem(question, state.isHost, viewModel)
                HorizontalDivider()
            }
        }
        if (!state.isHost) {
            ComposeRow(placeholder = "Ask a question", action = "Ask", onSend = viewModel::askQuestion)
        }
    }
}

@Composable
private fun QuestionItem(question: HostQuestion, isHost: Boolean, viewModel: WebinarRoomViewModel) {
    Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            question.authorName ?: if (isHost) "Attendee" else "You",
            fontSize = 12.sp,
            color = CoinsLiveColors.OnLightMuted,
        )
        Text(question.body, color = CoinsLiveColors.OnLightPrimary)
        if (!question.answerText.isNullOrBlank()) {
            Text("Host: ${question.answerText}", fontSize = 13.sp, color = CoinsLiveColors.Brand)
        }
        Text(CoinsLiveFormat.capitalize(question.status), fontSize = 11.sp, color = CoinsLiveColors.OnLightMuted)
        if (isHost) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                if (question.published != true) {
                    TextButton(onClick = { viewModel.publishQuestion(question.id) }) { Text("Publish") }
                }
                TextButton(onClick = { viewModel.answerQuestionLive(question.id) }) { Text("Answered Live") }
                TextButton(onClick = { viewModel.dismissQuestion(question.id) }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun MessagesPanel(state: WebinarRoomState, viewModel: WebinarRoomViewModel, modifier: Modifier) {
    Column(modifier) {
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
            if (state.messages.isEmpty()) {
                item {
                    EmptyHint(if (state.isHost) "No messages yet." else "Send a private message to the host.")
                }
            }
            items(state.messages, key = { it.id }) { message ->
                MessageItem(message, state.isHost, viewModel)
                HorizontalDivider()
            }
        }
        if (!state.isHost) {
            ComposeRow(placeholder = "Message to host", action = "Send", onSend = viewModel::sendMessageToHost)
        }
    }
}

@Composable
private fun MessageItem(message: HostInboxMessage, isHost: Boolean, viewModel: WebinarRoomViewModel) {
    var reply by remember(message.id) { mutableStateOf("") }
    Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            message.authorName ?: if (isHost) "Attendee" else "You",
            fontSize = 12.sp,
            color = CoinsLiveColors.OnLightMuted,
        )
        Text(message.body, color = CoinsLiveColors.OnLightPrimary)
        if (!message.hostReply.isNullOrBlank()) {
            Text("Host: ${message.hostReply}", fontSize = 13.sp, color = CoinsLiveColors.Brand)
        }
        if (isHost) {
            OutlinedTextField(
                value = reply,
                onValueChange = { reply = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Your reply") },
                singleLine = true,
            )
            Row {
                TextButton(
                    onClick = {
                        viewModel.replyToMessage(message.id, reply)
                        reply = ""
                    },
                    enabled = reply.isNotBlank(),
                ) { Text("Reply") }
                TextButton(onClick = { viewModel.resolveMessage(message.id) }) { Text("Resolve") }
            }
        }
    }
}

@Composable
private fun BroadcastsPanel(state: WebinarRoomState, viewModel: WebinarRoomViewModel, modifier: Modifier) {
    Column(modifier) {
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
            if (state.broadcasts.isEmpty()) {
                item { EmptyHint("No announcements yet.") }
            }
            items(state.broadcasts, key = { it.id }) { broadcast ->
                Text(
                    broadcast.body,
                    color = CoinsLiveColors.OnLightPrimary,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                HorizontalDivider()
            }
        }
        ComposeRow(placeholder = "Announce to everyone", action = "Send", onSend = viewModel::sendBroadcast)
    }
}

@Composable
private fun PeoplePanel(state: WebinarRoomState, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            Text(
                "${state.attendeeCount} in room",
                fontSize = 13.sp,
                color = CoinsLiveColors.OnLightMuted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(state.participants, key = { it.identity }) { participant ->
            Text(
                participant.name,
                color = CoinsLiveColors.OnLightPrimary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        color = CoinsLiveColors.OnLightMuted,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun ComposeRow(placeholder: String, action: String, onSend: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder) },
            singleLine = true,
        )
        Button(
            onClick = {
                val text = draft
                draft = ""
                onSend(text)
            },
            enabled = draft.isNotBlank(),
        ) { Text(action) }
    }
}

// endregion

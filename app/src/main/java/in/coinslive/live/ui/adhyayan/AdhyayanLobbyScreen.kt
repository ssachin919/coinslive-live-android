package `in`.coinslive.live.ui.adhyayan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.coinslive.live.CoinsLiveApp
import `in`.coinslive.live.api.AdhyayanDetailResponse
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.ui.components.CenteredLoading
import `in`.coinslive.live.ui.components.CoinsLiveCard
import `in`.coinslive.live.ui.components.CoinsLivePrimaryButton
import `in`.coinslive.live.ui.components.CoinsLiveTopBar
import `in`.coinslive.live.ui.components.ErrorText
import `in`.coinslive.live.ui.components.LabeledRow
import `in`.coinslive.live.ui.components.StatusChip
import `in`.coinslive.live.ui.navigation.Routes
import `in`.coinslive.live.theme.CoinsLiveFormat
import `in`.coinslive.live.util.nilIfEmpty
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdhyayanLobbyState(
    val title: String? = null,
    val status: String = "scheduled",
    val isHost: Boolean = false,
    val joinStatus: String? = null,
    val role: String? = null,
    val roomCode: String? = null,
    val accessMode: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
) {
    val isAdmitted: Boolean get() = joinStatus == "admitted" || isHost
}

sealed interface AdhyayanLobbyEvent {
    data class EnterRoom(val title: String?) : AdhyayanLobbyEvent
}

class AdhyayanLobbyViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val app = CoinsLiveApp.instance
    val slug: String = savedStateHandle.get<String>(Routes.ARG_SLUG).orEmpty()

    private val _state = MutableStateFlow(AdhyayanLobbyState())
    val state: StateFlow<AdhyayanLobbyState> = _state.asStateFlow()

    private val _events = Channel<AdhyayanLobbyEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun load() {
        viewModelScope.launch { reload(showSpinner = true) }
    }

    private suspend fun reload(showSpinner: Boolean) {
        _state.update { it.copy(isLoading = showSpinner, errorMessage = null) }
        try {
            val token = app.auth.bearerToken()
            apply(app.api.fetchAdhyayan(slug, token))
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Could not load Adhyayan") }
        }
    }

    private fun apply(response: AdhyayanDetailResponse) {
        val session = response.session
        val host = session.isHost == true
        _state.update {
            it.copy(
                title = session.title,
                status = session.status,
                isHost = host,
                accessMode = session.accessMode,
                roomCode = session.roomCode,
                role = response.me?.role ?: if (host) "host" else null,
                joinStatus = response.me?.joinStatus ?: if (host) "admitted" else null,
                isLoading = false,
            )
        }
    }

    fun joinSession(privateCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, errorMessage = null) }
            try {
                val token = app.auth.bearerToken()
                val join = app.api.joinAdhyayan(slug, privateCode.nilIfEmpty(), token)
                _state.update { it.copy(joinStatus = join.joinStatus, role = join.role) }
                reload(showSpinner = false)
                if (join.joinStatus == "pending") {
                    _state.update { it.copy(errorMessage = "Request sent. Wait for the host to admit you.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Could not join") }
            } finally {
                _state.update { it.copy(isWorking = false) }
            }
        }
    }

    fun transition(action: String) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, errorMessage = null) }
            try {
                val token = app.auth.bearerToken()
                val response = app.api.postAdhyayanStatus(slug, action, token)
                response.session?.let { s -> _state.update { it.copy(status = s.status) } }
                reload(showSpinner = false)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Request failed") }
            } finally {
                _state.update { it.copy(isWorking = false) }
            }
        }
    }

    fun goLiveThenEnter() {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, errorMessage = null) }
            try {
                val token = app.auth.bearerToken()
                if (_state.value.status == "scheduled") {
                    app.api.postAdhyayanStatus(slug, "enter_backstage", token)
                }
                val response = app.api.postAdhyayanStatus(slug, "go_live", token)
                _state.update { it.copy(status = response.session?.status ?: "live") }
                _events.send(AdhyayanLobbyEvent.EnterRoom(_state.value.title))
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Could not go live") }
            } finally {
                _state.update { it.copy(isWorking = false) }
            }
        }
    }
}

@Composable
fun AdhyayanLobbyScreen(
    onBack: () -> Unit,
    onEnterRoom: (title: String?) -> Unit,
    viewModel: AdhyayanLobbyViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var privateCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AdhyayanLobbyEvent.EnterRoom -> onEnterRoom(event.title)
            }
        }
    }

    CoinsLiveTheme(paper = true) {
        Scaffold(
            containerColor = CoinsLiveColors.Paper,
            topBar = { CoinsLiveTopBar(title = state.title ?: "Adhyayan", onBack = onBack, onDark = false) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CoinsLiveCard {
                        Text("Adhyayan", fontWeight = FontWeight.SemiBold, color = CoinsLiveColors.Brand)
                        state.title?.let { LabeledRow("Title", it) }
                        LabeledRow("Slug", viewModel.slug)
                        LabeledRow("Status", CoinsLiveFormat.statusLabel(state.status))
                        state.accessMode?.let { LabeledRow("Access", CoinsLiveFormat.capitalize(it)) }
                        state.role?.let { LabeledRow("Your role", CoinsLiveFormat.capitalize(it)) }
                        StatusChip(state.status)
                        if (state.isHost) {
                            state.roomCode?.let { LabeledRow("Room code", it) }
                        }
                    }

                    if (state.isHost) {
                        CoinsLiveCard {
                            Text("Host", fontWeight = FontWeight.SemiBold, color = CoinsLiveColors.Brand)
                            if (state.status == "scheduled") {
                                CoinsLivePrimaryButton(
                                    title = "Open prep room",
                                    isLoading = state.isWorking,
                                    onClick = { viewModel.transition("enter_backstage") },
                                )
                            }
                            if (state.status == "scheduled" || state.status == "backstage") {
                                CoinsLivePrimaryButton(
                                    title = "Go live and enter",
                                    isLoading = state.isWorking,
                                    onClick = viewModel::goLiveThenEnter,
                                )
                            }
                            if (state.status == "live" || state.status == "backstage") {
                                CoinsLivePrimaryButton(
                                    title = "Enter room",
                                    tint = CoinsLiveColors.Navy,
                                    onClick = { onEnterRoom(state.title) },
                                )
                            }
                            if (state.status != "ended") {
                                OutlinedButton(
                                    onClick = { viewModel.transition("end") },
                                    enabled = !state.isWorking,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("End session", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else {
                        CoinsLiveCard {
                            Text("Join", fontWeight = FontWeight.SemiBold, color = CoinsLiveColors.Brand)
                            when {
                                !state.isAdmitted -> {
                                    if (state.accessMode == "private") {
                                        OutlinedTextField(
                                            value = privateCode,
                                            onValueChange = { privateCode = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            label = { Text("Room code") },
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Characters,
                                            ),
                                        )
                                    }
                                    CoinsLivePrimaryButton(
                                        title = if (state.accessMode == "approval") "Request to join" else "Join session",
                                        isLoading = state.isWorking,
                                        onClick = { viewModel.joinSession(privateCode) },
                                    )
                                }

                                state.status == "live" -> CoinsLivePrimaryButton(
                                    title = "Enter room",
                                    onClick = { onEnterRoom(state.title) },
                                )

                                state.status == "ended" -> Text(
                                    "This session has ended.",
                                    fontSize = 14.sp,
                                    color = CoinsLiveColors.OnLightMuted,
                                )

                                else -> Text(
                                    "Waiting for the host to go live.",
                                    fontSize = 14.sp,
                                    color = CoinsLiveColors.OnLightMuted,
                                )
                            }
                        }
                    }

                    ErrorText(state.errorMessage)
                }

                if (state.isLoading) {
                    CenteredLoading(Modifier.fillMaxSize())
                }
            }
        }
    }
}
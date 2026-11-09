package `in`.coinslive.live.ui.webinar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.coinslive.live.CoinsLiveApp
import `in`.coinslive.live.api.CoinsLiveApiException
import `in`.coinslive.live.api.WebinarClientRole
import `in`.coinslive.live.api.WebinarDetail
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WebinarLobbyState(
    val title: String? = null,
    val status: String = "…",
    val isHost: Boolean = false,
    val hasAccessToken: Boolean = false,
    val isLoading: Boolean = true,
    val isRegistering: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface WebinarLobbyEvent {
    data class EnterRoom(val title: String?, val role: WebinarClientRole) : WebinarLobbyEvent
}

class WebinarLobbyViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val app = CoinsLiveApp.instance
    val slug: String = savedStateHandle.get<String>(Routes.ARG_SLUG).orEmpty()

    private val _state = MutableStateFlow(WebinarLobbyState(hasAccessToken = app.accessStore.token(slug) != null))
    val state: StateFlow<WebinarLobbyState> = _state.asStateFlow()

    private val _events = Channel<WebinarLobbyEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    hasAccessToken = app.accessStore.token(slug) != null,
                )
            }
            try {
                val token = app.auth.bearerToken()
                apply(app.api.fetchWebinar(slug, bearerToken = token), includeHostRole = true)
            } catch (authError: Exception) {
                // Fall back to the public (unauthenticated) view.
                try {
                    apply(app.api.fetchWebinar(slug), includeHostRole = false)
                } catch (_: Exception) {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = authError.message ?: "Could not load workshop")
                    }
                }
            }
        }
    }

    private fun apply(detail: WebinarDetail, includeHostRole: Boolean) {
        _state.update {
            it.copy(
                title = detail.title ?: it.title,
                status = detail.status,
                isHost = if (includeHostRole) detail.isHost == true else it.isHost,
                isLoading = false,
            )
        }
    }

    fun registerAsAttendee() {
        viewModelScope.launch {
            _state.update { it.copy(isRegistering = true, errorMessage = null) }
            try {
                val token = app.auth.bearerToken()
                val response = app.api.registerForWebinar(slug = slug, name = null, bearerToken = token)
                val accessToken = response.accessToken
                    ?: throw CoinsLiveApiException(response.error ?: "Registration failed")
                app.accessStore.save(accessToken, slug)
                _state.update { it.copy(isRegistering = false, hasAccessToken = true) }
                if (_state.value.status == "live") {
                    _events.send(WebinarLobbyEvent.EnterRoom(_state.value.title, WebinarClientRole.ATTENDEE))
                }
            } catch (e: Exception) {
                _state.update { it.copy(isRegistering = false, errorMessage = e.message ?: "Registration failed") }
            }
        }
    }
}

@Composable
fun WebinarLobbyScreen(
    onBack: () -> Unit,
    onEnterRoom: (title: String?, role: WebinarClientRole) -> Unit,
    viewModel: WebinarLobbyViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WebinarLobbyEvent.EnterRoom -> onEnterRoom(event.title, event.role)
            }
        }
    }

    CoinsLiveTheme(paper = true) {
        Scaffold(
            containerColor = CoinsLiveColors.Paper,
            topBar = { CoinsLiveTopBar(title = state.title ?: "Workshop", onBack = onBack, onDark = false) },
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
                        Text("Workshop", fontWeight = FontWeight.SemiBold, color = CoinsLiveColors.Brand)
                        state.title?.let { LabeledRow("Title", it) }
                        LabeledRow("Slug", viewModel.slug)
                        LabeledRow("Status", CoinsLiveFormat.statusLabel(state.status))
                        StatusChip(state.status)
                    }

                    if (state.isHost) {
                        CoinsLiveCard {
                            Text("Host", fontWeight = FontWeight.SemiBold, color = CoinsLiveColors.Brand)
                            CoinsLivePrimaryButton(
                                title = "Enter as host",
                                enabled = state.status != "ended",
                                onClick = { onEnterRoom(state.title, WebinarClientRole.HOST) },
                            )
                        }
                    }

                    CoinsLiveCard {
                        Text("Attendee", fontWeight = FontWeight.SemiBold, color = CoinsLiveColors.Brand)
                        if (state.hasAccessToken) {
                            CoinsLivePrimaryButton(
                                title = "Enter as attendee",
                                enabled = state.status != "ended" && state.status != "scheduled",
                                onClick = { onEnterRoom(state.title, WebinarClientRole.ATTENDEE) },
                            )
                            if (state.status == "scheduled" || state.status == "backstage") {
                                Text(
                                    "Waiting for the host to go live.",
                                    fontSize = 13.sp,
                                    color = CoinsLiveColors.OnLightMuted,
                                )
                            }
                        } else {
                            CoinsLivePrimaryButton(
                                title = "Register to join",
                                isLoading = state.isRegistering,
                                enabled = state.status != "ended",
                                onClick = viewModel::registerAsAttendee,
                            )
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

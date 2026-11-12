package `in`.coinslive.live.ui.webinar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import `in`.coinslive.live.CoinsLiveApp
import `in`.coinslive.live.api.CoinsLiveApiException
import `in`.coinslive.live.api.HostInboxMessage
import `in`.coinslive.live.api.HostQuestion
import `in`.coinslive.live.api.LiveJoinRequest
import `in`.coinslive.live.api.LiveRoomKind
import `in`.coinslive.live.api.WebinarBroadcast
import `in`.coinslive.live.api.WebinarClientRole
import `in`.coinslive.live.api.WebinarParticipant
import `in`.coinslive.live.config.AppConfig
import `in`.coinslive.live.ui.navigation.Routes
import `in`.coinslive.live.ui.room.LiveKitRoomFactory
import `in`.coinslive.live.util.CrashLog
import io.livekit.android.room.Room
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WebinarRoomState(
    val connectionState: String = "Idle",
    val errorMessage: String? = null,
    val title: String? = null,
    val status: String = "scheduled",
    val attendeeCount: Int = 0,
    val participants: List<WebinarParticipant> = emptyList(),
    val isMicEnabled: Boolean = false,
    val isCameraEnabled: Boolean = false,
    val role: WebinarClientRole = WebinarClientRole.HOST,
    val questions: List<HostQuestion> = emptyList(),
    val messages: List<HostInboxMessage> = emptyList(),
    val broadcasts: List<WebinarBroadcast> = emptyList(),
    /** Non-null once LiveKit Room is created and connect has been attempted. */
    val room: Room? = null,
    val isConnected: Boolean = false,
) {
    val isHost: Boolean get() = role == WebinarClientRole.HOST
    val isLive: Boolean get() = status == "live"
    val isEnded: Boolean get() = status == "ended"
    val canGoLive: Boolean get() = isHost && !isLive && !isEnded && isConnected
    val canPublishMedia: Boolean get() = isHost
}

/**
 * Drives the workshop room: token, LiveKit connect, status polling, Q&A / messages.
 */
class WebinarRoomViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val app = CoinsLiveApp.instance
    private val api = app.api
    private val auth = app.auth
    private val appContext = application.applicationContext

    val slug: String = savedStateHandle.get<String>(Routes.ARG_SLUG).orEmpty()
    private val initialTitle: String? = savedStateHandle.get<String>(Routes.ARG_TITLE)
    private val role: WebinarClientRole =
        WebinarClientRole.fromWire(savedStateHandle.get<String>(Routes.ARG_ROLE))

    private val _state = MutableStateFlow(
        WebinarRoomState(
            title = initialTitle,
            role = role,
            isMicEnabled = false,
            isCameraEnabled = false,
        ),
    )
    val state: StateFlow<WebinarRoomState> = _state.asStateFlow()

    val shareUrl: String get() = AppConfig.webinarShareUrl(slug)

    private var started = false
    private var intentionalLeave = false
    private var accessToken: String? = null
    private var ownedRoom: Room? = null

    private var statusJob: Job? = null
    private var participantsJob: Job? = null
    private var engagementJob: Job? = null

    private val isHost get() = role == WebinarClientRole.HOST

    // region Lifecycle

    fun start(enableMic: Boolean, enableCamera: Boolean) {
        if (started) return
        started = true
        viewModelScope.launch {
            intentionalLeave = false
            accessToken = if (isHost) null else app.accessStore.token(slug)

            if (!isHost && accessToken == null) {
                _state.update {
                    it.copy(
                        connectionState = "Failed",
                        errorMessage = "Register for this workshop before joining as an attendee.",
                    )
                }
                return@launch
            }

            try {
                val bearer = auth.bearerToken()

                runCatching { api.fetchWebinar(slug, bearerToken = bearer) }.getOrNull()?.let { detail ->
                    _state.update {
                        it.copy(
                            status = detail.status,
                            title = detail.title ?: it.title,
                            attendeeCount = detail.attendeeCount ?: 0,
                        )
                    }
                }

                CrashLog.breadcrumb(appContext, "webinar:fetch_token")
                _state.update { it.copy(connectionState = "Getting ready…", errorMessage = null) }
                val tokenResponse = api.fetchLiveToken(
                    LiveJoinRequest(
                        kind = LiveRoomKind.WEBINAR,
                        slug = slug,
                        asHost = isHost,
                        accessToken = accessToken,
                    ),
                    bearer,
                )
                _state.update {
                    it.copy(
                        connectionState = "Connecting…",
                        status = tokenResponse.status ?: it.status,
                    )
                }

                CrashLog.breadcrumb(appContext, "webinar:livekit_create")
                val room = LiveKitRoomFactory.create(appContext)
                ownedRoom = room
                _state.update { it.copy(room = room) }

                CrashLog.breadcrumb(appContext, "webinar:livekit_connect")
                room.connect(tokenResponse.url, tokenResponse.token)
                CrashLog.breadcrumb(appContext, "webinar:connected")
                CrashLog.clearBreadcrumb(appContext)

                onRoomConnected()
                if (canPublishForMedia()) {
                    enableLocalMedia(room, enableMic, enableCamera)
                }
            } catch (e: Exception) {
                CrashLog.write(appContext, e)
                _state.update {
                    it.copy(
                        connectionState = "Failed",
                        errorMessage = e.message ?: "Could not join the room.",
                    )
                }
            }
        }
    }

    private fun canPublishForMedia(): Boolean = _state.value.canPublishMedia

    private fun onRoomConnected() {
        _state.update { it.copy(connectionState = "Connected", isConnected = true, errorMessage = null) }
        viewModelScope.launch {
            if (isHost) {
                val presence = if (_state.value.isLive) "host_rejoin" else "enter_backstage"
                runCatching { api.postWebinarStatus(slug, presence, auth.bearerToken()) }
            }
            refreshStatus()
            startPolling()
        }
    }

    suspend fun enableLocalMedia(
        room: Room,
        enableMic: Boolean,
        enableCamera: Boolean,
    ) {
        if (!_state.value.canPublishMedia) {
            _state.update { it.copy(isMicEnabled = false, isCameraEnabled = false) }
            return
        }
        var micOn = false
        var camOn = false
        var error: String? = null
        if (enableMic) {
            try {
                CrashLog.breadcrumb(appContext, "webinar:enable_mic")
                room.localParticipant.setMicrophoneEnabled(true)
                micOn = true
            } catch (e: Exception) {
                error = e.message ?: "Microphone could not be enabled."
            }
        }
        if (enableCamera) {
            try {
                CrashLog.breadcrumb(appContext, "webinar:enable_camera")
                room.localParticipant.setCameraEnabled(true)
                camOn = true
            } catch (e: Exception) {
                error = listOfNotNull(error, e.message ?: "Camera could not be enabled.")
                    .joinToString(" ")
            }
        }
        CrashLog.clearBreadcrumb(appContext)
        _state.update {
            it.copy(
                isMicEnabled = micOn,
                isCameraEnabled = camOn,
                errorMessage = error ?: it.errorMessage,
            )
        }
    }

    fun onRoomDisconnected() {
        if (!_state.value.isConnected) return
        _state.update { it.copy(connectionState = "Disconnected", isConnected = false) }
        if (isHost && !intentionalLeave && !_state.value.isEnded) {
            viewModelScope.launch {
                runCatching { api.postWebinarStatus(slug, "host_disconnect", auth.bearerToken()) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    // endregion

    // region Host actions

    /** Returns true when the workshop is now live. */
    suspend fun goLive(): Boolean {
        if (!isHost) return false
        return try {
            val response = api.postWebinarStatus(slug, "go_live", auth.bearerToken())
            _state.update { it.copy(status = response.webinar?.status ?: "live") }
            true
        } catch (e: Exception) {
            _state.update { it.copy(errorMessage = e.message) }
            false
        }
    }

    suspend fun endWebinar() {
        if (!isHost) return
        intentionalLeave = true
        try {
            api.postWebinarStatus(slug, "end", auth.bearerToken())
            _state.update { it.copy(status = "ended") }
        } catch (e: Exception) {
            _state.update { it.copy(errorMessage = e.message) }
        }
        cancelPolling()
    }

    suspend fun leave() {
        intentionalLeave = true
        cancelPolling()
        if (isHost && !_state.value.isEnded) {
            runCatching { api.postWebinarStatus(slug, "host_disconnect", auth.bearerToken()) }
        }
    }

    suspend fun toggleMic(room: Room) {
        if (!_state.value.canPublishMedia) return
        val next = !_state.value.isMicEnabled
        try {
            room.localParticipant.setMicrophoneEnabled(next)
            _state.update { it.copy(isMicEnabled = next, errorMessage = null) }
        } catch (e: Exception) {
            _state.update { it.copy(errorMessage = e.message) }
        }
    }

    suspend fun toggleCamera(room: Room) {
        if (!_state.value.canPublishMedia) return
        val next = !_state.value.isCameraEnabled
        try {
            room.localParticipant.setCameraEnabled(next)
            _state.update { it.copy(isCameraEnabled = next, errorMessage = null) }
        } catch (e: Exception) {
            _state.update { it.copy(errorMessage = e.message) }
        }
    }

    // endregion

    // region Engagement

    fun refreshEngagement() {
        viewModelScope.launch { refreshEngagementNow() }
    }

    private suspend fun refreshEngagementNow() {
        try {
            val bearer = auth.bearerToken()
            if (isHost) {
                val questions = api.listQuestions(slug, bearerToken = bearer)
                val messages = api.listMessages(slug, bearerToken = bearer)
                val broadcasts = api.listBroadcasts(slug, bearer)
                _state.update { it.copy(questions = questions, messages = messages, broadcasts = broadcasts) }
            } else {
                val token = accessToken ?: return
                val questions = api.listQuestions(slug, bearerToken = bearer, accessToken = token)
                val messages = api.listMessages(slug, bearerToken = bearer, accessToken = token)
                _state.update { it.copy(questions = questions, messages = messages, broadcasts = emptyList()) }
            }
        } catch (_: Exception) {
            // Keep prior lists on transient poll failures.
        }
    }

    fun sendMessageToHost(body: String) = runAction {
        val token = accessToken ?: return@runAction
        api.sendWebinarMessage(slug, body.trim(), token, auth.bearerToken())
        refreshEngagementNow()
    }

    fun askQuestion(body: String) = runAction {
        val token = accessToken ?: return@runAction
        api.askQuestion(slug, body.trim(), token, auth.bearerToken())
        refreshEngagementNow()
    }

    fun publishQuestion(id: String) = questionAction(id, "publish")

    fun dismissQuestion(id: String) = questionAction(id, "dismiss")

    fun answerQuestionLive(id: String) =
        questionAction(id, "answer", answerText = "Answered live", publish = true)

    fun replyToMessage(id: String, reply: String) = messageAction(id, "reply", hostReply = reply.trim())

    fun resolveMessage(id: String) = messageAction(id, "resolve")

    fun sendBroadcast(body: String) = runAction {
        api.createBroadcast(slug, body.trim(), auth.bearerToken())
        refreshEngagementNow()
    }

    private fun questionAction(
        id: String,
        action: String,
        answerText: String? = null,
        publish: Boolean? = null,
    ) = runAction {
        api.questionAction(slug, id, action, answerText, publish, auth.bearerToken())
        refreshEngagementNow()
    }

    private fun messageAction(id: String, action: String, hostReply: String? = null) = runAction {
        api.messageAction(slug, id, action, hostReply, auth.bearerToken())
        refreshEngagementNow()
    }

    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CoinsLiveApiException) {
                _state.update { it.copy(errorMessage = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Request failed") }
            }
        }
    }

    // endregion

    // region Polling

    private fun startPolling() {
        cancelPolling()
        statusJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                refreshStatus()
            }
        }
        participantsJob = viewModelScope.launch {
            while (isActive) {
                refreshParticipants()
                delay(5_000)
            }
        }
        engagementJob = viewModelScope.launch {
            while (isActive) {
                refreshEngagementNow()
                delay(4_000)
            }
        }
    }

    private fun cancelPolling() {
        statusJob?.cancel()
        participantsJob?.cancel()
        engagementJob?.cancel()
        statusJob = null
        participantsJob = null
        engagementJob = null
    }

    private suspend fun refreshStatus() {
        val detail = runCatching {
            api.fetchWebinar(slug, fields = "status", bearerToken = auth.bearerToken())
        }.getOrNull() ?: return
        _state.update { it.copy(status = detail.status) }
    }

    private suspend fun refreshParticipants() {
        if (!isHost) return
        val response = runCatching {
            api.listWebinarParticipants(slug, auth.bearerToken())
        }.getOrNull() ?: return
        _state.update {
            it.copy(
                participants = response.participants,
                attendeeCount = response.count ?: response.participants.size,
            )
        }
    }

    // endregion

    override fun onCleared() {
        cancelPolling()
        val room = ownedRoom
        ownedRoom = null
        if (room != null) {
            runCatching {
                room.disconnect()
                room.release()
            }
        }
        super.onCleared()
    }
}

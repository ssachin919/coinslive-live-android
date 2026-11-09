package `in`.coinslive.live.ui.adhyayan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.coinslive.live.CoinsLiveApp
import `in`.coinslive.live.ui.navigation.Routes
import io.livekit.android.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdhyayanRoomState(
    val connectionState: String = "Idle",
    val errorMessage: String? = null,
    val title: String? = null,
    val status: String = "live",
    val role: String = "learner",
    val isMicEnabled: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val isScreenShareEnabled: Boolean = false,
    /** Non-null once the LiveKit token was fetched; the screen then mounts `RoomScope`. */
    val roomUrl: String? = null,
    val roomToken: String? = null,
) {
    val isHost: Boolean get() = role == "host" || role == "co_host"
}

/** Video-only Adhyayan room: peer cam/mic. No chat/polls. */
class AdhyayanRoomViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val app = CoinsLiveApp.instance
    private val api = app.api
    private val auth = app.auth

    val slug: String = savedStateHandle.get<String>(Routes.ARG_SLUG).orEmpty()

    private val _state = MutableStateFlow(
        AdhyayanRoomState(
            title = savedStateHandle.get<String>(Routes.ARG_TITLE),
            isMicEnabled = false,
            isCameraEnabled = false,
        ),
    )
    val state: StateFlow<AdhyayanRoomState> = _state.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            _state.update { it.copy(connectionState = "Getting ready…", errorMessage = null) }
            try {
                val response = api.fetchAdhyayanToken(slug, auth.bearerToken())
                _state.update {
                    it.copy(
                        connectionState = "Connecting…",
                        role = response.role,
                        status = response.status ?: it.status,
                        // Actual A/V starts after RoomScope connects (see enableLocalMedia).
                        isMicEnabled = false,
                        isCameraEnabled = false,
                        roomUrl = response.url,
                        roomToken = response.token,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(connectionState = "Failed", errorMessage = e.message) }
            }
        }
    }

    fun onRoomConnected() {
        _state.update { it.copy(connectionState = "Connected", errorMessage = null) }
    }

    fun onRoomDisconnected() {
        _state.update {
            if (it.connectionState == "Connected") it.copy(connectionState = "Disconnected") else it
        }
    }

    fun onRoomError(error: Exception?) {
        _state.update {
            it.copy(
                connectionState = "Failed",
                errorMessage = error?.message ?: "Could not connect to the room.",
            )
        }
    }

    fun onMediaPermissionDenied() {
        _state.update {
            it.copy(
                isMicEnabled = false,
                isCameraEnabled = false,
                errorMessage = "Camera and microphone permission is required to join Adhyayan.",
            )
        }
    }

    /**
     * Enable local A/V after connect. Must not be done via RoomScope's `audio`/`video`
     * flags — those call setMicrophoneEnabled/setCameraEnabled without try/catch and
     * can crash the process when permission is missing or the camera fails to open.
     */
    suspend fun enableLocalMedia(
        room: Room,
        enableMic: Boolean,
        enableCamera: Boolean,
    ) {
        var micOn = false
        var camOn = false
        var error: String? = null
        if (enableMic) {
            try {
                room.localParticipant.setMicrophoneEnabled(true)
                micOn = true
            } catch (e: Exception) {
                error = e.message ?: "Microphone could not be enabled."
            }
        }
        if (enableCamera) {
            try {
                room.localParticipant.setCameraEnabled(true)
                camOn = true
            } catch (e: Exception) {
                error = listOfNotNull(error, e.message ?: "Camera could not be enabled.")
                    .joinToString(" ")
            }
        }
        _state.update {
            it.copy(
                isMicEnabled = micOn,
                isCameraEnabled = camOn,
                errorMessage = error ?: it.errorMessage,
            )
        }
    }

    suspend fun toggleMic(room: Room) {
        val next = !_state.value.isMicEnabled
        try {
            room.localParticipant.setMicrophoneEnabled(next)
            _state.update { it.copy(isMicEnabled = next, errorMessage = null) }
        } catch (e: Exception) {
            _state.update { it.copy(errorMessage = e.message) }
        }
    }

    suspend fun toggleCamera(room: Room) {
        val next = !_state.value.isCameraEnabled
        try {
            room.localParticipant.setCameraEnabled(next)
            _state.update { it.copy(isCameraEnabled = next, errorMessage = null) }
        } catch (e: Exception) {
            _state.update { it.copy(errorMessage = e.message) }
        }
    }

    /** Host-only. Returns true when the session ended. */
    suspend fun endSession(): Boolean = try {
        api.postAdhyayanStatus(slug, "end", auth.bearerToken())
        _state.update { it.copy(status = "ended") }
        true
    } catch (e: Exception) {
        _state.update { it.copy(errorMessage = e.message) }
        false
    }
}

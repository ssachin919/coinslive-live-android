package `in`.coinslive.live.ui.adhyayan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import `in`.coinslive.live.CoinsLiveApp
import `in`.coinslive.live.ui.navigation.Routes
import `in`.coinslive.live.util.CrashLog
import io.livekit.android.AudioOptions
import io.livekit.android.LiveKit
import io.livekit.android.LiveKitOverrides
import io.livekit.android.RoomOptions
import io.livekit.android.audio.AudioHandler
import io.livekit.android.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdhyayanRoomState(
    val connectionState: String = "Idle",
    val errorMessage: String? = null,
    val title: String? = null,
    val status: String = "live",
    val role: String = "learner",
    val isMicEnabled: Boolean = false,
    val isCameraEnabled: Boolean = false,
    val isScreenShareEnabled: Boolean = false,
    /** Non-null once LiveKit Room is created and connect has been attempted. */
    val room: Room? = null,
) {
    val isHost: Boolean get() = role == "host" || role == "co_host"
}

/** Video-only Adhyayan room: peer cam/mic. No chat/polls. */
class AdhyayanRoomViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val app = CoinsLiveApp.instance
    private val api = app.api
    private val auth = app.auth
    private val appContext = application.applicationContext

    val slug: String = savedStateHandle.get<String>(Routes.ARG_SLUG).orEmpty()

    private val _state = MutableStateFlow(
        AdhyayanRoomState(title = savedStateHandle.get<String>(Routes.ARG_TITLE)),
    )
    val state: StateFlow<AdhyayanRoomState> = _state.asStateFlow()

    private var started = false
    private var ownedRoom: Room? = null

    fun start(enableMic: Boolean, enableCamera: Boolean) {
        if (started) return
        started = true
        viewModelScope.launch {
            _state.update { it.copy(connectionState = "Getting ready…", errorMessage = null) }
            try {
                CrashLog.breadcrumb(appContext, "adhyayan:fetch_token")
                val response = api.fetchAdhyayanToken(slug, auth.bearerToken())
                _state.update {
                    it.copy(
                        connectionState = "Connecting…",
                        role = response.role,
                        status = response.status ?: it.status,
                        title = it.title ?: slug,
                    )
                }

                CrashLog.breadcrumb(appContext, "adhyayan:livekit_init")
                withContext(Dispatchers.Main.immediate) {
                    runCatching { LiveKit.init(appContext) }
                }

                CrashLog.breadcrumb(appContext, "adhyayan:livekit_create")
                val room = withContext(Dispatchers.Main.immediate) {
                    LiveKit.create(
                        appContext = appContext,
                        options = RoomOptions(adaptiveStream = true, dynacast = true),
                        overrides = LiveKitOverrides(
                            // Avoid AudioSwitch/Bluetooth paths that can hard-crash on some OEMs.
                            audioOptions = AudioOptions(
                                audioHandler = object : AudioHandler {
                                    override fun start() = Unit
                                    override fun stop() = Unit
                                },
                            ),
                        ),
                    )
                }
                ownedRoom = room
                _state.update { it.copy(room = room) }

                CrashLog.breadcrumb(appContext, "adhyayan:livekit_connect")
                room.connect(response.url, response.token)
                CrashLog.breadcrumb(appContext, "adhyayan:connected")
                _state.update { it.copy(connectionState = "Connected", errorMessage = null) }
                CrashLog.clearBreadcrumb(appContext)

                enableLocalMedia(room, enableMic, enableCamera)
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

    fun onMediaPermissionDenied() {
        _state.update {
            it.copy(
                isMicEnabled = false,
                isCameraEnabled = false,
                errorMessage = "Camera and microphone permission is required to join Adhyayan.",
            )
        }
    }

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
                CrashLog.breadcrumb(appContext, "adhyayan:enable_mic")
                room.localParticipant.setMicrophoneEnabled(true)
                micOn = true
            } catch (e: Exception) {
                error = e.message ?: "Microphone could not be enabled."
            }
        }
        if (enableCamera) {
            try {
                CrashLog.breadcrumb(appContext, "adhyayan:enable_camera")
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

    override fun onCleared() {
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

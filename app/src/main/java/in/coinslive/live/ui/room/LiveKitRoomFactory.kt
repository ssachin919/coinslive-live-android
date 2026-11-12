package `in`.coinslive.live.ui.room

import android.content.Context
import io.livekit.android.AudioOptions
import io.livekit.android.LiveKit
import io.livekit.android.LiveKitOverrides
import io.livekit.android.RoomOptions
import io.livekit.android.audio.AudioHandler
import io.livekit.android.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Creates a LiveKit [Room] with OEM-safe audio defaults (no AudioSwitch/Bluetooth). */
object LiveKitRoomFactory {
    private val noOpAudioHandler = object : AudioHandler {
        override fun start() = Unit
        override fun stop() = Unit
    }

    suspend fun create(appContext: Context): Room = withContext(Dispatchers.Main.immediate) {
        runCatching { LiveKit.init(appContext) }
        LiveKit.create(
            appContext = appContext,
            options = RoomOptions(adaptiveStream = true, dynacast = true),
            overrides = LiveKitOverrides(
                audioOptions = AudioOptions(audioHandler = noOpAudioHandler),
            ),
        )
    }
}

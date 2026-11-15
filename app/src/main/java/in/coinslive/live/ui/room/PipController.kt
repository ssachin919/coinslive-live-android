package `in`.coinslive.live.ui.room

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates Android Picture-in-Picture for live Workshop / Adhyayan rooms.
 * Room screens mark a meeting active; [MainActivity] enters PiP when the user leaves the app.
 */
object PipController {
    private val _meetingActive = MutableStateFlow(false)
    val meetingActive: StateFlow<Boolean> = _meetingActive.asStateFlow()

    private val _inPip = MutableStateFlow(false)
    val inPip: StateFlow<Boolean> = _inPip.asStateFlow()

    private var meetingTitle: String? = null

    fun setMeetingActive(active: Boolean, title: String? = null) {
        _meetingActive.value = active
        meetingTitle = if (active) title else null
        if (!active) _inPip.value = false
    }

    fun setInPip(inPip: Boolean) {
        _inPip.value = inPip
    }

    /** Enter PiP if a live meeting is active. Returns true when PiP was requested or already active. */
    fun enterPip(activity: Activity): Boolean {
        if (!_meetingActive.value) return false
        if (activity.isInPictureInPictureMode) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            activity.enterPictureInPictureMode(buildParams(activity))
            true
        } catch (_: IllegalStateException) {
            false
        }
    }

    fun syncAutoEnter(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching {
            activity.setPictureInPictureParams(buildParams(activity))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildParams(@Suppress("UNUSED_PARAMETER") context: Context): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(_meetingActive.value)
                    setSeamlessResizeEnabled(true)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    meetingTitle?.takeIf { it.isNotBlank() }?.let(::setTitle)
                }
            }
            .build()
}

package `in`.coinslive.live.ui.room

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import `in`.coinslive.live.util.findActivity

/** Marks the current room as PiP-eligible while [active] and connected. */
@Composable
fun RememberPipMeeting(active: Boolean, title: String?) {
    val context = LocalContext.current
    val activity = context.findActivity()

    LaunchedEffect(active, title) {
        PipController.setMeetingActive(active, title)
        activity?.let(PipController::syncAutoEnter)
    }

    DisposableEffect(Unit) {
        onDispose {
            PipController.setMeetingActive(false)
            activity?.let(PipController::syncAutoEnter)
        }
    }
}

@Composable
fun rememberEnterPip(): () -> Boolean {
    val activity = LocalContext.current.findActivity()
    return remember(activity) {
        { activity?.let(PipController::enterPip) ?: false }
    }
}

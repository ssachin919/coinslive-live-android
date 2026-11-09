package `in`.coinslive.live.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.coinslive.live.theme.CoinsLiveColors

/**
 * Bottom controls for live rooms. Icon-free (text labels) so no extra icon dependency is needed.
 */
@Composable
fun MediaControlBar(
    isMicEnabled: Boolean,
    isCameraEnabled: Boolean,
    isScreenShareEnabled: Boolean,
    modifier: Modifier = Modifier,
    showMediaToggles: Boolean = true,
    // TODO: Android screen share needs MediaProjection consent + a mediaProjection foreground service.
    canScreenShare: Boolean = false,
    showGoLive: Boolean = false,
    showEnd: Boolean = true,
    onToggleMic: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onToggleScreenShare: () -> Unit = {},
    onGoLive: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null,
    onLeave: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CoinsLiveColors.Navy.copy(alpha = 0.92f))
            .navigationBarsPadding()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showMediaToggles) {
            ToggleButton(
                label = if (isMicEnabled) "Mic on" else "Mic off",
                active = isMicEnabled,
                onClick = onToggleMic,
            )
            ToggleButton(
                label = if (isCameraEnabled) "Cam on" else "Cam off",
                active = isCameraEnabled,
                onClick = onToggleCamera,
            )
            if (canScreenShare) {
                ToggleButton(
                    label = if (isScreenShareEnabled) "Stop share" else "Share",
                    active = isScreenShareEnabled,
                    activeColor = Color(0xFF2E7D32),
                    onClick = onToggleScreenShare,
                )
            }
        }
        if (showGoLive && onGoLive != null) {
            Button(
                onClick = onGoLive,
                colors = ButtonDefaults.buttonColors(containerColor = CoinsLiveColors.Live, contentColor = Color.White),
            ) { Text("Go Live") }
        }
        if (showEnd && onEnd != null) {
            OutlinedButton(
                onClick = onEnd,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80)),
            ) { Text("End") }
        }
        if (onLeave != null) {
            OutlinedButton(
                onClick = onLeave,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) { Text("Leave") }
        }
    }
}

@Composable
private fun ToggleButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    activeColor: Color = CoinsLiveColors.Brand,
) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (active) activeColor else Color(0xFFB71C1C),
            contentColor = Color.White,
        ),
    ) { Text(label) }
}

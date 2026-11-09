package `in`.coinslive.live.util

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

data class MediaPermissionState(
    /** True once we have asked (or already had) camera + mic. */
    val resolved: Boolean,
    val cameraGranted: Boolean,
    val micGranted: Boolean,
) {
    val canPublishMedia: Boolean get() = cameraGranted || micGranted
}

/**
 * Ensures CAMERA + RECORD_AUDIO are requested before LiveKit tries to publish.
 * RoomScope's built-in `audio`/`video` flags enable media without try/catch and can
 * crash the process on SecurityException — callers should enable media themselves.
 */
@Composable
fun rememberMediaPermissionState(
    requestOnEnter: Boolean = true,
): MediaPermissionState {
    val context = LocalContext.current

    fun snapshot(): MediaPermissionState {
        val camera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        val mic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        return MediaPermissionState(
            resolved = true,
            cameraGranted = camera,
            micGranted = mic,
        )
    }

    var state by remember {
        mutableStateOf(
            MediaPermissionState(
                resolved = false,
                cameraGranted = false,
                micGranted = false,
            ),
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        state = snapshot()
    }

    LaunchedEffect(requestOnEnter) {
        if (!requestOnEnter) {
            state = snapshot()
            return@LaunchedEffect
        }
        val current = snapshot()
        if (current.cameraGranted && current.micGranted) {
            state = current
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                ),
            )
        }
    }

    return state
}

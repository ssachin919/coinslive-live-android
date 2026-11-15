package `in`.coinslive.live

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import `in`.coinslive.live.deeplink.DeepLink
import `in`.coinslive.live.deeplink.DeepLinkInbox
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.ui.navigation.AppNav
import `in`.coinslive.live.ui.room.PipController

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Room screens re-check permissions before publishing local media.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            DeepLinkInbox.offer(DeepLink.parse(intent))
        }
        requestMediaPermissions()

        val auth = CoinsLiveApp.instance.auth
        setContent {
            CoinsLiveTheme {
                AppNav(auth)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinkInbox.offer(DeepLink.parse(intent))
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Pre-Android 12: enter PiP when the user presses Home during a live meeting.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PipController.enterPip(this)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        onPipModeChanged(isInPictureInPictureMode, resyncAutoEnter = true)
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        // Still called on some OEM builds alongside the Configuration overload.
        onPipModeChanged(isInPictureInPictureMode)
    }

    private fun onPipModeChanged(inPip: Boolean, resyncAutoEnter: Boolean = false) {
        PipController.setInPip(inPip)
        if (resyncAutoEnter && !inPip) {
            PipController.syncAutoEnter(this)
        }
    }

    private fun requestMediaPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            ),
        )
    }
}

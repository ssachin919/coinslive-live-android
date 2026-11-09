package `in`.coinslive.live

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import `in`.coinslive.live.deeplink.DeepLink
import `in`.coinslive.live.deeplink.DeepLinkInbox
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.ui.navigation.AppNav
import io.livekit.android.room.track.video.CameraCapturerUtils
import livekit.org.webrtc.CameraXHelper
import livekit.org.webrtc.CameraXProvider

class MainActivity : ComponentActivity() {

    private var cameraProvider: CameraXProvider? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Room screens re-check permissions before publishing local media.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerCameraX()

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

    override fun onDestroy() {
        cameraProvider?.let { CameraCapturerUtils.unregisterCameraProvider(it) }
        cameraProvider = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinkInbox.offer(DeepLink.parse(intent))
    }

    private fun registerCameraX() {
        runCatching {
            val provider = CameraXHelper.createCameraProvider(this)
            if (provider.isSupported(application)) {
                CameraCapturerUtils.registerCameraProvider(provider)
                cameraProvider = provider
            }
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

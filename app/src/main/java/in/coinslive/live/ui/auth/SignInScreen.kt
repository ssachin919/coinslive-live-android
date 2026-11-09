package `in`.coinslive.live.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clerk.ui.auth.AuthView
import `in`.coinslive.live.auth.AuthSession
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.ui.components.CenteredLoading
import `in`.coinslive.live.ui.components.CoinsLiveWordmark

/** Full-screen Clerk sign-in. Navigation switches to the app once a session exists. */
@Composable
fun SignInScreen(auth: AuthSession) {
    val initialized by auth.isInitialized.collectAsState()

    CoinsLiveTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CoinsLiveColors.Navy),
        ) {
            when {
                !auth.isConfigured -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CoinsLiveWordmark(onDark = true)
                    Text(
                        text = auth.configurationError ?: "Sign in is not configured for this build.",
                        color = CoinsLiveColors.OnDarkAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    )
                }

                !initialized -> CenteredLoading(Modifier.fillMaxSize(), label = "Connecting…")

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    CoinsLiveWordmark(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        compact = true,
                        onDark = true,
                    )
                    Text(
                        text = "Workshops and Adhyayan video calls. Use your CoinsLive website account.",
                        color = CoinsLiveColors.OnDarkSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    AuthView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

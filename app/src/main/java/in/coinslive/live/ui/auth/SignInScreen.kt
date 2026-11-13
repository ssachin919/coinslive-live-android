package `in`.coinslive.live.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clerk.ui.auth.AuthView
import `in`.coinslive.live.auth.AuthSession
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.theme.SpaceGroteskFamily
import `in`.coinslive.live.ui.components.CenteredLoading
import `in`.coinslive.live.ui.components.CoinsLiveWordmark

/** Full-screen Clerk sign-in. Navigation switches to the app once a session exists. */
@Composable
fun SignInScreen(auth: AuthSession) {
    val initialized by auth.isInitialized.collectAsState()

    CoinsLiveTheme {
        Column(
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

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CoinsLiveWordmark(onDark = true)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Learn together.\nBuild something real.",
                            color = Color.White,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 34.sp,
                        )
                        Text(
                            "Sign in with your CoinsLive account to host or join Workshop and Adhyayan rooms.",
                            color = CoinsLiveColors.OnDarkSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        AuthView(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

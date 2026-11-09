package `in`.coinslive.live.theme

import androidx.compose.ui.graphics.Color

/** CoinsLive brand tokens (aligned with web `globals.css` / iOS CoinsLiveTheme). */
object CoinsLiveColors {
    val Navy = Color(0xFF0D1B2A)
    val Brand = Color(0xFF1E88E5)
    val Tan = Color(0xFFF4A261)
    val Paper = Color(0xFFF3F0EA)
    val Slate = Color(0xFF4A5568)
    val Ink = Color(0xFF0D1B2A)
    val Body = Color(0xFF334155)
    val CardElevated = Color.White
    val Live = Color(0xFFE53935)

    val OnDarkPrimary = Color.White
    val OnDarkSecondary = Color.White.copy(alpha = 0.82f)
    val OnDarkMuted = Color.White.copy(alpha = 0.68f)
    val OnDarkAccent = Tan

    val OnLightPrimary = Ink
    val OnLightSecondary = Body
    val OnLightMuted = Slate
}

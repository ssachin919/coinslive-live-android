package `in`.coinslive.live.theme

import androidx.compose.ui.graphics.Color

/** CoinsLive brand tokens aligned with www.coinslive.in CSS. */
object CoinsLiveColors {
    val Navy = Color(0xFF0D1B2A)
    val Brand = Color(0xFF1E88E5)
    val Tan = Color(0xFFF4A261)
    val Paper = Color(0xFFF3F0EA) // rare editorial accent only
    val Slate = Color(0xFF4A5568)
    val Ink = Color(0xFF0D1B2A)
    val Body = Color(0xFF334155)
    val CardElevated = Color.White
    val Live = Color(0xFFE53935)

    // Light surfaces (website body)
    val Canvas = Color(0xFFF4F7FB)
    val Border = Color(0xFFDDE2E5)
    val BrandText = Color(0xFF1565C0)
    val BrandTint = Color(0xFFE8F3FC)
    val LiveTint = Color(0xFFFDECEC)
    val LiveText = Color(0xFFC62828)
    val TanTint = Color(0xFFFDEBDD)
    val TanText = Color(0xFF9A4F10)
    val EndedTint = Color(0xFFEEF1F4)

    // Dark surfaces
    val NavyCard = Color(0xFF182D41)
    val NavyDeep = Color(0xFF0B1420)
    val NavyLine = Color(0xFF1A3A5C)
    val BrandOnDark = Color(0xFF64B5F6)

    val OnDarkPrimary = Color.White
    val OnDarkSecondary = Color.White.copy(alpha = 0.82f)
    val OnDarkMuted = Color.White.copy(alpha = 0.68f)
    val OnDarkAccent = Tan

    val OnLightPrimary = Ink
    val OnLightSecondary = Body
    val OnLightMuted = Slate
}

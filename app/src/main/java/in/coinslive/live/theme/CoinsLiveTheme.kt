package `in`.coinslive.live.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = CoinsLiveColors.Brand,
    onPrimary = Color.White,
    secondary = CoinsLiveColors.Tan,
    onSecondary = CoinsLiveColors.Navy,
    tertiary = CoinsLiveColors.Tan,
    background = CoinsLiveColors.Navy,
    onBackground = Color.White,
    surface = CoinsLiveColors.NavyCard,
    onSurface = Color.White,
    surfaceVariant = CoinsLiveColors.NavyDeep,
    onSurfaceVariant = CoinsLiveColors.OnDarkSecondary,
    error = Color(0xFFFF8A80),
    outline = CoinsLiveColors.NavyLine,
)

/** Light scheme matching the website body (cool canvas, white cards). */
private val LightScheme = lightColorScheme(
    primary = CoinsLiveColors.Brand,
    onPrimary = Color.White,
    secondary = CoinsLiveColors.Tan,
    onSecondary = CoinsLiveColors.Navy,
    tertiary = CoinsLiveColors.Tan,
    background = CoinsLiveColors.Canvas,
    onBackground = CoinsLiveColors.Ink,
    surface = Color.White,
    onSurface = CoinsLiveColors.Ink,
    surfaceVariant = Color.White,
    onSurfaceVariant = CoinsLiveColors.Body,
    error = Color(0xFFC62828),
    outline = CoinsLiveColors.Border,
)

/**
 * Brand theme. [paper] / light = website canvas for Home, Join, lobbies, create, account.
 * Default dark = navy shell for room chrome and sign-in hero.
 */
@Composable
fun CoinsLiveTheme(
    paper: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (paper) LightScheme else DarkScheme,
        typography = CoinsLiveTypography,
        content = content,
    )
}

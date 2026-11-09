package `in`.coinslive.live.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand tokens live in Color.kt (`CoinsLiveColors`), typography in Type.kt.

private val DarkScheme = darkColorScheme(
    primary = CoinsLiveColors.Brand,
    onPrimary = Color.White,
    secondary = CoinsLiveColors.Tan,
    onSecondary = CoinsLiveColors.Navy,
    tertiary = CoinsLiveColors.Tan,
    background = CoinsLiveColors.Navy,
    onBackground = Color.White,
    surface = CoinsLiveColors.Navy,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF16283C),
    onSurfaceVariant = CoinsLiveColors.OnDarkSecondary,
    error = Color(0xFFFF8A80),
    outline = Color.White.copy(alpha = 0.4f),
)

private val PaperScheme = lightColorScheme(
    primary = CoinsLiveColors.Brand,
    onPrimary = Color.White,
    secondary = CoinsLiveColors.Tan,
    onSecondary = CoinsLiveColors.Navy,
    tertiary = CoinsLiveColors.Tan,
    background = CoinsLiveColors.Paper,
    onBackground = CoinsLiveColors.Ink,
    surface = CoinsLiveColors.Paper,
    onSurface = CoinsLiveColors.Ink,
    surfaceVariant = Color.White,
    onSurfaceVariant = CoinsLiveColors.Body,
    error = Color(0xFFC62828),
    outline = CoinsLiveColors.Slate.copy(alpha = 0.5f),
)

/**
 * Brand theme. [paper] switches to the light "paper" surface used by lobbies and create forms;
 * the default is the navy shell used by home / account / join.
 */
@Composable
fun CoinsLiveTheme(
    paper: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (paper) PaperScheme else DarkScheme,
        typography = CoinsLiveTypography,
        content = content,
    )
}

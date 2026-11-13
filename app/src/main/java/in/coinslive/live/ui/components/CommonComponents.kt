package `in`.coinslive.live.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveFormat
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.theme.SpaceGroteskFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinsLiveTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onDark: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    val container = if (onDark) CoinsLiveColors.Navy else Color.White
    val content = if (onDark) Color.White else CoinsLiveColors.Ink
    Column {
        TopAppBar(
            title = {
                Text(
                    title,
                    maxLines = 1,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = { actions() },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = container,
                titleContentColor = content,
                navigationIconContentColor = content,
                actionIconContentColor = content,
            ),
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = if (onDark) Color.White.copy(alpha = 0.08f) else CoinsLiveColors.Border,
        )
    }
}

@Composable
fun CoinsLiveWordmark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onDark: Boolean = true,
) {
    Wordmark(modifier = modifier, compact = compact, onDark = onDark)
}

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val key = status.lowercase()
    val (bg, fg) = when {
        onDark && key == "live" -> CoinsLiveColors.Live.copy(alpha = 0.2f) to Color(0xFFFF8A80)
        onDark && key == "backstage" -> CoinsLiveColors.Tan.copy(alpha = 0.2f) to CoinsLiveColors.Tan
        onDark && key == "ended" -> Color.White.copy(alpha = 0.12f) to CoinsLiveColors.OnDarkMuted
        onDark -> CoinsLiveColors.Brand.copy(alpha = 0.2f) to CoinsLiveColors.BrandOnDark
        key == "live" -> CoinsLiveColors.LiveTint to CoinsLiveColors.LiveText
        key == "backstage" -> CoinsLiveColors.TanTint to CoinsLiveColors.TanText
        key == "ended" -> CoinsLiveColors.EndedTint to CoinsLiveColors.Slate
        else -> CoinsLiveColors.BrandTint to CoinsLiveColors.BrandText
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (key == "live") {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(CoinsLiveColors.Live),
            )
        }
        Text(
            text = CoinsLiveFormat.statusLabel(status),
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
fun CoinsLivePrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CoinsLiveColors.Brand,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val labelColor = when (tint) {
        CoinsLiveColors.Tan -> CoinsLiveColors.Navy
        else -> Color.White
    }
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tint,
            contentColor = labelColor,
            disabledContainerColor = tint.copy(alpha = 0.38f),
            disabledContentColor = labelColor.copy(alpha = 0.7f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = labelColor,
            )
        } else {
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** White bordered card used on light canvas and navy shells. */
@Composable
fun CoinsLiveCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CoinsLiveTheme(paper = true) {
        CompositionLocalProvider(LocalContentColor provides CoinsLiveColors.Ink) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CoinsLiveColors.CardElevated)
                    .border(1.dp, CoinsLiveColors.Border, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun LabeledRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = CoinsLiveColors.OnLightMuted,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = CoinsLiveColors.OnLightPrimary,
        )
    }
}

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier, onDark: Boolean = false) {
    if (message.isNullOrBlank()) return
    Text(
        text = message,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = if (onDark) CoinsLiveColors.OnDarkAccent else MaterialTheme.colorScheme.error,
    )
}

@Composable
fun CenteredLoading(modifier: Modifier = Modifier, label: String? = null) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(color = CoinsLiveColors.Brand)
            if (label != null) {
                Text(label, color = CoinsLiveColors.OnLightMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SectionOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = CoinsLiveColors.OnLightMuted,
    )
}

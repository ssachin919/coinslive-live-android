package `in`.coinslive.live.ui.components

import androidx.compose.foundation.background
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
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.theme.CoinsLiveFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinsLiveTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onDark: Boolean = true,
    actions: @Composable () -> Unit = {},
) {
    val container = if (onDark) CoinsLiveColors.Navy else CoinsLiveColors.Paper
    val content = if (onDark) Color.White else CoinsLiveColors.Ink
    TopAppBar(
        title = { Text(title, maxLines = 1, fontWeight = FontWeight.SemiBold) },
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
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val color = when (status.lowercase()) {
        "live" -> CoinsLiveColors.Live
        "backstage" -> CoinsLiveColors.Tan
        "ended" -> CoinsLiveColors.Slate
        else -> CoinsLiveColors.Brand
    }
    Text(
        text = CoinsLiveFormat.statusLabel(status),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
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
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tint,
            contentColor = if (tint == CoinsLiveColors.Tan) CoinsLiveColors.Navy else Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

/** White rounded card used on navy and paper backgrounds. */
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(CoinsLiveColors.CardElevated)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = CoinsLiveColors.Brand)
            if (label != null) Text(label, color = CoinsLiveColors.OnDarkMuted)
        }
    }
}

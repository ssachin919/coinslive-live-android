package `in`.coinslive.live.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.coinslive.live.R
import `in`.coinslive.live.theme.CoinsLiveColors

@Composable
fun CoinsLiveMark(onDark: Boolean = true, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    if (onDark) {
        Image(
            painter = painterResource(R.drawable.coinslive_mark_white),
            contentDescription = null,
            modifier = modifier.size(size),
        )
    } else {
        Image(
            painter = painterResource(R.drawable.coinslive_mark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(CoinsLiveColors.Navy),
            modifier = modifier.size(size),
        )
    }
}

@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onDark: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
    ) {
        CoinsLiveMark(onDark = onDark, size = if (compact) 28.dp else 40.dp)
        Column {
            Text(
                text = "CoinsLive",
                fontSize = if (compact) 17.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (onDark) CoinsLiveColors.OnDarkPrimary else CoinsLiveColors.OnLightPrimary,
            )
            if (!compact) {
                Text(
                    text = "Live",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (onDark) CoinsLiveColors.OnDarkAccent else CoinsLiveColors.Brand,
                )
            }
        }
    }
}

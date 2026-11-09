package `in`.coinslive.live.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.ui.components.CenteredLoading
import `in`.coinslive.live.ui.components.CoinsLiveTopBar
import `in`.coinslive.live.ui.components.CoinsLiveWordmark
import `in`.coinslive.live.ui.components.ErrorText
import `in`.coinslive.live.ui.components.StatusChip
import `in`.coinslive.live.theme.CoinsLiveFormat

private data class SessionItem(
    val id: String,
    val title: String,
    val status: String,
    val scheduledAt: String?,
    val onOpen: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenWorkshop: (String) -> Unit,
    onOpenAdhyayan: (String) -> Unit,
    onCreateWorkshop: () -> Unit,
    onCreateAdhyayan: () -> Unit,
    viewModel: MeetingsListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    val workshopItems = state.workshops.map { workshop ->
        SessionItem(workshop.id, workshop.title, workshop.status, workshop.scheduledAt) {
            onOpenWorkshop(workshop.slug)
        }
    }
    val adhyayanItems = state.adhyayanSessions.map { session ->
        SessionItem(session.id, session.title, session.status, session.scheduledAt) {
            onOpenAdhyayan(session.slug)
        }
    }

    Scaffold(
        containerColor = CoinsLiveColors.Navy,
        topBar = {
            Box {
                CoinsLiveTopBar(title = "") {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
                CoinsLiveWordmark(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp),
                    compact = true,
                    onDark = true,
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.hasLoadedOnce,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Live sessions",
                            color = CoinsLiveColors.OnDarkPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Host or join a Workshop or Adhyayan room with camera and mic.",
                            color = CoinsLiveColors.OnDarkSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                state.minVersionWarning?.let { warning ->
                    item {
                        Text(
                            warning,
                            color = CoinsLiveColors.Navy,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CoinsLiveColors.Tan.copy(alpha = 0.9f))
                                .padding(12.dp),
                        )
                    }
                }

                item {
                    ActionCard(
                        title = "Start a workshop",
                        subtitle = "You present. Attendees watch.",
                        tint = CoinsLiveColors.Brand,
                        onClick = onCreateWorkshop,
                    )
                }
                item {
                    ActionCard(
                        title = "Start Adhyayan",
                        subtitle = "Study together with camera and mic.",
                        tint = CoinsLiveColors.Tan,
                        onClick = onCreateAdhyayan,
                    )
                }

                sessionSection(
                    title = "Your workshops",
                    empty = "No workshops yet. Create one above.",
                    items = workshopItems,
                    isLoading = state.isLoading,
                )
                sessionSection(
                    title = "Your Adhyayan",
                    empty = "Rooms you host show up here.",
                    items = adhyayanItems,
                    isLoading = state.isLoading,
                )

                state.errorMessage?.let { message ->
                    item { ErrorText(message, onDark = true) }
                }
            }

            if (state.isLoading && !state.hasLoadedOnce) {
                CenteredLoading(Modifier.fillMaxSize(), label = "Loading…")
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sessionSection(
    title: String,
    empty: String,
    items: List<SessionItem>,
    isLoading: Boolean,
) {
    item {
        Text(
            title,
            color = CoinsLiveColors.OnDarkAccent,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    if (items.isEmpty() && !isLoading) {
        item {
            Text(empty, color = CoinsLiveColors.OnDarkMuted, fontSize = 13.sp)
        }
    } else {
        items(items, key = { it.id }) { item -> SessionRow(item) }
    }
}

@Composable
private fun SessionRow(item: SessionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CoinsLiveColors.CardElevated)
            .clickable(onClick = item.onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.title,
                color = CoinsLiveColors.OnLightPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(item.status)
                item.scheduledAt?.let(CoinsLiveFormat::scheduledLabel)?.let { label ->
                    Text(label, color = CoinsLiveColors.OnLightMuted, fontSize = 12.sp)
                }
            }
        }
        Text("›", color = CoinsLiveColors.Brand, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CoinsLiveColors.CardElevated)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = if (tint == CoinsLiveColors.Tan) CoinsLiveColors.Navy else Color.White)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = CoinsLiveColors.OnLightPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(subtitle, color = CoinsLiveColors.OnLightSecondary, fontSize = 12.sp)
        }
    }
}

package `in`.coinslive.live.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.coinslive.live.api.AdhyayanSummary
import `in`.coinslive.live.api.WebinarSummary
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveFormat
import `in`.coinslive.live.theme.SpaceGroteskFamily
import `in`.coinslive.live.ui.components.CenteredLoading
import `in`.coinslive.live.ui.components.CoinsLiveWordmark
import `in`.coinslive.live.ui.components.ErrorText
import `in`.coinslive.live.ui.components.SectionOverline
import `in`.coinslive.live.ui.components.StatusChip

private data class SessionItem(
    val id: String,
    val kind: String,
    val title: String,
    val status: String,
    val scheduledAt: String?,
    val subtitle: String? = null,
    val onOpen: () -> Unit,
)

private fun isLiveStatus(status: String): Boolean =
    status.equals("live", ignoreCase = true) || status.equals("backstage", ignoreCase = true)

private fun isEndedStatus(status: String): Boolean =
    status.equals("ended", ignoreCase = true)

private fun WebinarSummary.toSessionItem(onOpen: (String) -> Unit) = SessionItem(
    id = id,
    kind = "Workshop",
    title = title,
    status = status,
    scheduledAt = scheduledAt,
    onOpen = { onOpen(slug) },
)

private fun AdhyayanSummary.toSessionItem(onOpen: (String) -> Unit) = SessionItem(
    id = id,
    kind = "Adhyayan",
    title = title,
    status = status,
    scheduledAt = scheduledAt,
    subtitle = listOfNotNull(
        topic?.takeIf { it.isNotBlank() },
        accessMode?.let(CoinsLiveFormat::capitalize),
        admittedCount?.let { "$it admitted" },
    ).joinToString(" · ").ifBlank { null },
    onOpen = { onOpen(slug) },
)

@Composable
fun rememberMeetingsListViewModel(): MeetingsListViewModel {
    val activity = LocalContext.current as ComponentActivity
    return viewModel(viewModelStoreOwner = activity)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenWorkshop: (String) -> Unit,
    onOpenAdhyayan: (String) -> Unit,
    onCreateWorkshop: () -> Unit,
    onCreateAdhyayan: () -> Unit,
    viewModel: MeetingsListViewModel = rememberMeetingsListViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.refresh() }

    // Refresh when returning from create / lobby so newly created rooms always appear.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val workshopItems = state.workshops.map { it.toSessionItem(onOpenWorkshop) }
    val adhyayanItems = state.adhyayanSessions.map { it.toSessionItem(onOpenAdhyayan) }

    val liveItems = (workshopItems + adhyayanItems).filter { isLiveStatus(it.status) }
    val upcomingWorkshops = workshopItems.filterNot { isLiveStatus(it.status) || isEndedStatus(it.status) }
    val upcomingAdhyayan = adhyayanItems.filterNot { isLiveStatus(it.status) || isEndedStatus(it.status) }
    val endedItems = (workshopItems + adhyayanItems).filter { isEndedStatus(it.status) }.take(8)

    Scaffold(containerColor = CoinsLiveColors.Canvas) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.hasLoadedOnce,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    HomeHero(onRefresh = { viewModel.refresh() })
                }

                state.minVersionWarning?.let { warning ->
                    item {
                        Text(
                            warning,
                            color = CoinsLiveColors.Navy,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CoinsLiveColors.TanTint)
                                .border(1.dp, CoinsLiveColors.Tan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QuickActionTile(
                            title = "Workshop",
                            subtitle = "Host a live session",
                            icon = Icons.Default.School,
                            modifier = Modifier.weight(1f),
                            onClick = onCreateWorkshop,
                        )
                        QuickActionTile(
                            title = "Adhyayan",
                            subtitle = "Study together",
                            icon = Icons.Default.Groups,
                            modifier = Modifier.weight(1f),
                            accent = CoinsLiveColors.Tan,
                            onClick = onCreateAdhyayan,
                        )
                    }
                }

                if (liveItems.isNotEmpty()) {
                    item { SectionHeader("Live now") }
                    items(liveItems, key = { "live-${it.kind}-${it.id}" }) { SessionRow(it) }
                }

                item { SectionHeader("Your Workshops") }
                if (upcomingWorkshops.isEmpty() && !state.isLoading) {
                    item {
                        EmptyState(
                            "No workshops yet",
                            "Create a Workshop to broadcast to attendees with Q&A.",
                        )
                    }
                } else {
                    items(upcomingWorkshops, key = { "ws-${it.id}" }) { SessionRow(it) }
                }

                item { SectionHeader("Your Adhyayan") }
                if (upcomingAdhyayan.isEmpty() && !state.isLoading) {
                    item {
                        EmptyState(
                            "No Adhyayan rooms yet",
                            "Create a study room — it shows here as soon as it is created.",
                        )
                    }
                } else {
                    items(upcomingAdhyayan, key = { "ad-${it.id}" }) { SessionRow(it) }
                }

                if (endedItems.isNotEmpty()) {
                    item { SectionHeader("Recently ended") }
                    items(endedItems, key = { "end-${it.kind}-${it.id}" }) { SessionRow(it) }
                }

                if (
                    liveItems.isEmpty() &&
                    upcomingWorkshops.isEmpty() &&
                    upcomingAdhyayan.isEmpty() &&
                    endedItems.isEmpty() &&
                    !state.isLoading
                ) {
                    item {
                        EmptyState(
                            "No sessions yet",
                            "Start a Workshop or Adhyayan to go live with camera and mic.",
                        )
                    }
                }

                state.errorMessage?.let { message ->
                    item {
                        ErrorText(message, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }

            if (state.isLoading && !state.hasLoadedOnce) {
                CenteredLoading(Modifier.fillMaxSize(), label = "Loading…")
            }
        }
    }
}

@Composable
private fun HomeHero(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CoinsLiveColors.Navy)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoinsLiveWordmark(compact = true, onDark = true, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }
        Text(
            "Learn together. Build something real.",
            color = Color.White,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        )
        Text(
            "Host or join a Workshop or Adhyayan with camera and mic. Minimise mid-call to keep watching in a small window.",
            color = CoinsLiveColors.OnDarkSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = CoinsLiveColors.Ink,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
    )
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CoinsLiveColors.Border, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = CoinsLiveColors.Ink)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = CoinsLiveColors.OnLightMuted)
    }
}

@Composable
private fun SessionRow(item: SessionItem) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CoinsLiveColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = item.onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionOverline(item.kind)
            Text(
                item.title,
                color = CoinsLiveColors.OnLightPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
            item.subtitle?.let {
                Text(it, color = CoinsLiveColors.OnLightMuted, style = MaterialTheme.typography.bodySmall)
            }
            item.scheduledAt?.let(CoinsLiveFormat::scheduledLabel)?.let { label ->
                Text(label, color = CoinsLiveColors.OnLightMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        StatusChip(item.status)
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = CoinsLiveColors.Brand,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CoinsLiveColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (accent == CoinsLiveColors.Tan) CoinsLiveColors.TanTint else CoinsLiveColors.BrandTint,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (accent == CoinsLiveColors.Tan) CoinsLiveColors.TanText else CoinsLiveColors.BrandText,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = CoinsLiveColors.Ink,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = CoinsLiveColors.OnLightMuted)
        }
    }
}

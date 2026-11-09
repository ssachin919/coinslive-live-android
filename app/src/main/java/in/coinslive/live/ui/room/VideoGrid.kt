package `in`.coinslive.live.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.livekit.android.compose.types.TrackReference
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.Track

fun participantLabel(participant: Participant): String {
    val name = participant.name?.trim().orEmpty()
    if (name.isNotEmpty()) return name
    return participant.identity?.value?.takeIf { it.isNotBlank() } ?: "Participant"
}

private fun TrackReference.hasVideo(): Boolean {
    val pub = publication ?: return false
    return !pub.muted
}

/**
 * Single "stage": prefers a screen share (with camera picture-in-picture) and falls back to the camera.
 * [tracks] should contain only the tracks of one participant.
 */
@Composable
fun ParticipantStage(
    tracks: List<TrackReference>,
    label: String,
    modifier: Modifier = Modifier,
    preferScreenShare: Boolean = true,
    showCameraPip: Boolean = true,
    placeholder: String = "Camera preview",
) {
    val screen = tracks.firstOrNull { it.source == Track.Source.SCREEN_SHARE && it.hasVideo() }
    val camera = tracks.firstOrNull { it.source == Track.Source.CAMERA && it.hasVideo() }
    val primary = if (preferScreenShare && screen != null) screen else camera ?: screen
    val isLocal = primary?.participant is LocalParticipant
    val showPip = showCameraPip && preferScreenShare && screen != null && camera != null

    Box(modifier = modifier.background(Color.Black)) {
        if (primary != null) {
            VideoTrackView(
                trackReference = primary,
                modifier = Modifier.fillMaxSize(),
                mirror = isLocal && primary.source == Track.Source.CAMERA,
            )
        } else {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
        }

        if (showPip && camera != null) {
            VideoTrackView(
                trackReference = camera,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .width(112.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                mirror = camera.participant is LocalParticipant,
            )
        }

        NameTag(
            text = label + if (primary?.source == Track.Source.SCREEN_SHARE) " · Screen" else "",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        )
    }
}

/** Grid of camera tiles (one per participant; placeholders show a "camera off" tile). */
@Composable
fun ParticipantGrid(
    cameraTracks: List<TrackReference>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = cameraTracks,
            key = { ref -> ref.participant.sid.value + ":" + ref.source.name },
        ) { ref ->
            ParticipantTile(ref)
        }
    }
}

@Composable
fun ParticipantTile(ref: TrackReference, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111111)),
    ) {
        if (ref.hasVideo()) {
            VideoTrackView(
                trackReference = ref,
                modifier = Modifier.fillMaxSize(),
                mirror = ref.participant is LocalParticipant,
            )
        } else {
            Text(
                "Camera off",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        val self = if (ref.participant is LocalParticipant) " (You)" else ""
        NameTag(
            text = participantLabel(ref.participant) + self,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        )
    }
}

@Composable
private fun NameTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

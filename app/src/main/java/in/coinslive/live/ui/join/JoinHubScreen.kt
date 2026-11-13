package `in`.coinslive.live.ui.join

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.coinslive.live.CoinsLiveApp
import `in`.coinslive.live.api.LiveRoomKind
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.ui.components.CoinsLivePrimaryButton
import `in`.coinslive.live.ui.components.CoinsLiveTopBar
import `in`.coinslive.live.ui.components.ErrorText
import `in`.coinslive.live.util.nilIfEmpty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JoinUiState(
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
)

class JoinHubViewModel : ViewModel() {
    private val app = CoinsLiveApp.instance
    private val _state = MutableStateFlow(JoinUiState())
    val state: StateFlow<JoinUiState> = _state.asStateFlow()

    fun join(
        kind: LiveRoomKind,
        slug: String,
        displayName: String,
        roomCode: String,
        onJoined: (String, LiveRoomKind) -> Unit,
    ) {
        val trimmedSlug = slug.trim()
        if (trimmedSlug.isEmpty()) return
        viewModelScope.launch {
            _state.update { JoinUiState(isWorking = true) }
            try {
                val token = app.auth.bearerToken()
                when (kind) {
                    LiveRoomKind.WEBINAR -> {
                        val response = app.api.registerForWebinar(
                            slug = trimmedSlug,
                            name = displayName.nilIfEmpty(),
                            bearerToken = token,
                        )
                        val accessToken = response.accessToken
                        if (accessToken.isNullOrEmpty()) {
                            throw IllegalStateException(response.error ?: "Registration failed")
                        }
                        app.accessStore.save(accessToken, trimmedSlug)
                    }

                    LiveRoomKind.ADHYAYAN -> {
                        val join = app.api.joinAdhyayan(
                            slug = trimmedSlug,
                            roomCode = roomCode.nilIfEmpty(),
                            bearerToken = token,
                        )
                        if (join.joinStatus == "pending") {
                            throw IllegalStateException("Waiting for host approval. Try again when admitted.")
                        }
                    }
                }
                _state.update { JoinUiState() }
                onJoined(trimmedSlug, kind)
            } catch (e: Exception) {
                _state.update { JoinUiState(errorMessage = e.message ?: "Could not join") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinHubScreen(
    onJoined: (String, LiveRoomKind) -> Unit,
    viewModel: JoinHubViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var kind by remember { mutableStateOf(LiveRoomKind.WEBINAR) }
    var slug by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }

    CoinsLiveTheme(paper = true) {
        Scaffold(
            containerColor = CoinsLiveColors.Canvas,
            topBar = { CoinsLiveTopBar(title = "Join", onDark = false) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Paste the room slug from your invite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoinsLiveColors.OnLightSecondary,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindChip("Workshop", kind == LiveRoomKind.WEBINAR) { kind = LiveRoomKind.WEBINAR }
                    KindChip("Adhyayan", kind == LiveRoomKind.ADHYAYAN) { kind = LiveRoomKind.ADHYAYAN }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, CoinsLiveColors.Border, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    JoinField(
                        label = "Slug",
                        value = slug,
                        onValueChange = { slug = it },
                        placeholder = "Room slug",
                        capitalization = KeyboardCapitalization.None,
                    )

                    if (kind == LiveRoomKind.WEBINAR) {
                        JoinField(
                            label = "Display name",
                            value = displayName,
                            onValueChange = { displayName = it },
                            placeholder = "Optional name for the host",
                            capitalization = KeyboardCapitalization.Words,
                        )
                        Text(
                            "Registers you, then opens the lobby.",
                            color = CoinsLiveColors.OnLightMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        JoinField(
                            label = "Room code",
                            value = roomCode,
                            onValueChange = { roomCode = it },
                            placeholder = "For private rooms",
                            capitalization = KeyboardCapitalization.Characters,
                        )
                        Text(
                            "Joins the room, then opens the lobby.",
                            color = CoinsLiveColors.OnLightMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                ErrorText(state.errorMessage)

                CoinsLivePrimaryButton(
                    title = if (kind == LiveRoomKind.WEBINAR) "Register and continue" else "Join Adhyayan",
                    isLoading = state.isWorking,
                    enabled = slug.trim().isNotEmpty(),
                    onClick = { viewModel.join(kind, slug, displayName, roomCode, onJoined) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            labelColor = CoinsLiveColors.Body,
            selectedContainerColor = CoinsLiveColors.BrandTint,
            selectedLabelColor = CoinsLiveColors.BrandText,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = CoinsLiveColors.Border,
            selectedBorderColor = CoinsLiveColors.Brand.copy(alpha = 0.35f),
        ),
    )
}

@Composable
private fun JoinField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = CoinsLiveColors.Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(capitalization = capitalization),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CoinsLiveColors.Canvas,
                unfocusedContainerColor = CoinsLiveColors.Canvas,
                focusedTextColor = CoinsLiveColors.Ink,
                unfocusedTextColor = CoinsLiveColors.Ink,
                focusedPlaceholderColor = CoinsLiveColors.Slate,
                unfocusedPlaceholderColor = CoinsLiveColors.Slate,
                focusedBorderColor = CoinsLiveColors.Brand,
                unfocusedBorderColor = CoinsLiveColors.Border,
                cursorColor = CoinsLiveColors.Brand,
            ),
        )
    }
}

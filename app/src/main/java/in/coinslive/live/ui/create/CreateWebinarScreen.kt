package `in`.coinslive.live.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.coinslive.live.CoinsLiveApp
import `in`.coinslive.live.theme.CoinsLiveColors
import `in`.coinslive.live.theme.CoinsLiveTheme
import `in`.coinslive.live.ui.components.CoinsLivePrimaryButton
import `in`.coinslive.live.ui.components.CoinsLiveTopBar
import `in`.coinslive.live.ui.components.ErrorText
import `in`.coinslive.live.theme.CoinsLiveFormat
import `in`.coinslive.live.util.nilIfEmpty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class CreateWebinarViewModel : ViewModel() {
    private val app = CoinsLiveApp.instance
    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    fun create(
        title: String,
        description: String,
        scheduledAtMillis: Long?,
        maxAttendees: Int,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _state.update { CreateUiState(isSaving = true) }
            try {
                val token = app.auth.bearerToken()
                val webinar = app.api.createWebinar(
                    title = title.trim(),
                    description = description.nilIfEmpty(),
                    scheduledAtMillis = scheduledAtMillis,
                    maxAttendees = maxAttendees,
                    bearerToken = token,
                )
                _state.update { CreateUiState() }
                onCreated(webinar.slug)
            } catch (e: Exception) {
                _state.update { CreateUiState(errorMessage = e.message ?: "Could not create workshop") }
            }
        }
    }
}

@Composable
fun CreateWebinarScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateWebinarViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var useSchedule by remember { mutableStateOf(false) }
    var scheduledAt by remember { mutableLongStateOf(System.currentTimeMillis() + 3_600_000L) }
    var maxAttendees by remember { mutableIntStateOf(25) }

    CoinsLiveTheme(paper = true) {
        Scaffold(
            containerColor = CoinsLiveColors.Paper,
            topBar = { CoinsLiveTopBar(title = "New workshop", onBack = onBack, onDark = false) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    "Name the workshop. You can go live right after creating it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoinsLiveColors.OnLightSecondary,
                )

                LabeledField("Title", title, { title = it }, "Workshop title")
                LabeledField(
                    "Description",
                    description,
                    { description = it },
                    "Optional. What will you cover?",
                    singleLine = false,
                    minLines = 3,
                )

                StepperRow("Up to $maxAttendees attendees", maxAttendees, 1..500) { maxAttendees = it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Schedule for later")
                    Switch(checked = useSchedule, onCheckedChange = { useSchedule = it })
                }
                if (useSchedule) {
                    OutlinedButton(
                        onClick = {
                            pickDateTime(context, scheduledAt) { scheduledAt = it }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Starts: ${CoinsLiveFormat.dateTimeLabel(scheduledAt)}")
                    }
                }

                ErrorText(state.errorMessage)

                CoinsLivePrimaryButton(
                    title = "Create workshop",
                    isLoading = state.isSaving,
                    enabled = title.trim().isNotEmpty(),
                    onClick = {
                        viewModel.create(
                            title = title,
                            description = description,
                            scheduledAtMillis = if (useSchedule) scheduledAt else null,
                            maxAttendees = maxAttendees,
                            onCreated = onCreated,
                        )
                    },
                )
            }
        }
    }
}

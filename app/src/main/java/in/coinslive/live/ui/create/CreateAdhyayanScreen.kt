package `in`.coinslive.live.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import `in`.coinslive.live.util.nilIfEmpty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateAdhyayanViewModel : ViewModel() {
    private val app = CoinsLiveApp.instance
    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    fun create(
        title: String,
        topic: String,
        description: String,
        learningGoal: String,
        accessMode: String,
        capacity: Int,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _state.update { CreateUiState(isSaving = true) }
            try {
                val token = app.auth.bearerToken()
                val session = app.api.createAdhyayan(
                    title = title.trim(),
                    topic = topic.nilIfEmpty(),
                    description = description.nilIfEmpty(),
                    learningGoal = learningGoal.nilIfEmpty(),
                    scheduledAtMillis = null,
                    accessMode = accessMode,
                    capacity = capacity,
                    bearerToken = token,
                )
                _state.update { CreateUiState() }
                onCreated(session.slug)
            } catch (e: Exception) {
                _state.update { CreateUiState(errorMessage = e.message ?: "Could not create Adhyayan") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdhyayanScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateAdhyayanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var learningGoal by remember { mutableStateOf("") }
    var accessMode by remember { mutableStateOf("public") }
    var capacity by remember { mutableIntStateOf(12) }

    CoinsLiveTheme(paper = true) {
        Scaffold(
            containerColor = CoinsLiveColors.Canvas,
            topBar = { CoinsLiveTopBar(title = "New Adhyayan", onBack = onBack, onDark = false) },
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
                    "Start a study room. Everyone admitted can share camera and mic.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoinsLiveColors.OnLightSecondary,
                )

                LabeledField("Title", title, { title = it }, "Room title")
                LabeledField("Topic", topic, { topic = it }, "Optional topic")
                LabeledField(
                    "Description",
                    description,
                    { description = it },
                    "Optional. What will you study?",
                    singleLine = false,
                    minLines = 3,
                )
                LabeledField("Learning goal", learningGoal, { learningGoal = it }, "Optional goal")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Access", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("public" to "Public", "private" to "Private", "approval" to "Approval").forEach { (value, label) ->
                            FilterChip(
                                selected = accessMode == value,
                                onClick = { accessMode = value },
                                label = { Text(label) },
                            )
                        }
                    }
                }

                StepperRow("Capacity $capacity", capacity, 2..40) { capacity = it }

                ErrorText(state.errorMessage)

                CoinsLivePrimaryButton(
                    title = "Create Adhyayan",
                    tint = CoinsLiveColors.Tan,
                    isLoading = state.isSaving,
                    enabled = title.trim().isNotEmpty(),
                    onClick = {
                        viewModel.create(
                            title = title,
                            topic = topic,
                            description = description,
                            learningGoal = learningGoal,
                            accessMode = accessMode,
                            capacity = capacity,
                            onCreated = onCreated,
                        )
                    },
                )
            }
        }
    }
}

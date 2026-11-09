package `in`.coinslive.live.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.coinslive.live.BuildConfig
import `in`.coinslive.live.CoinsLiveApp
import `in`.coinslive.live.api.AdhyayanSummary
import `in`.coinslive.live.api.WebinarSummary
import `in`.coinslive.live.theme.CoinsLiveFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeetingsUiState(
    val workshops: List<WebinarSummary> = emptyList(),
    val adhyayanSessions: List<AdhyayanSummary> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val errorMessage: String? = null,
    val minVersionWarning: String? = null,
)

class MeetingsListViewModel : ViewModel() {
    private val app = CoinsLiveApp.instance
    private val api = app.api
    private val auth = app.auth

    private val _state = MutableStateFlow(MeetingsUiState())
    val state: StateFlow<MeetingsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        val token = try {
            auth.bearerToken()
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            return
        }

        coroutineScope {
            val config = async { checkConfig() }
            val workshops = async { runCatching { api.listWebinars(token) } }
            val adhyayan = async { runCatching { api.listAdhyayanSessions(token, mine = true) } }

            val workshopResult = workshops.await()
            val adhyayanResult = adhyayan.await()
            val warning = config.await()

            val error = workshopResult.exceptionOrNull()?.message
                ?: adhyayanResult.exceptionOrNull()?.message

            _state.update {
                it.copy(
                    workshops = workshopResult.getOrDefault(emptyList()),
                    adhyayanSessions = adhyayanResult.getOrDefault(emptyList()),
                    isLoading = false,
                    hasLoadedOnce = true,
                    errorMessage = error,
                    minVersionWarning = warning,
                )
            }
        }
    }

    private suspend fun checkConfig(): String? = try {
        val min = api.fetchMobileConfig().minAppVersion
        if (min != null && CoinsLiveFormat.isVersionOlder(BuildConfig.VERSION_NAME, min)) {
            "Update required. Minimum version is $min."
        } else {
            null
        }
    } catch (_: Exception) {
        null // Config is optional.
    }
}

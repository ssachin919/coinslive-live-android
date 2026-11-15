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
import kotlinx.coroutines.delay
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

    /** Optimistic insert so a freshly created room appears before the next network refresh. */
    fun upsertAdhyayan(session: AdhyayanSummary) {
        _state.update { current ->
            current.copy(
                adhyayanSessions = prependReplacing(current.adhyayanSessions, session) { row, item ->
                    row.id == item.id || row.slug == item.slug
                },
                hasLoadedOnce = true,
            )
        }
    }

    fun upsertWorkshop(webinar: WebinarSummary) {
        _state.update { current ->
            current.copy(
                workshops = prependReplacing(current.workshops, webinar) { row, item ->
                    row.id == item.id || row.slug == item.slug
                },
                hasLoadedOnce = true,
            )
        }
    }

    fun upsertAdhyayanAndRefresh(session: AdhyayanSummary) {
        upsertAdhyayan(session)
        refresh()
    }

    fun upsertWorkshopAndRefresh(webinar: WebinarSummary) {
        upsertWorkshop(webinar)
        refresh()
    }

    private fun <T> prependReplacing(
        items: List<T>,
        item: T,
        isSame: (existing: T, incoming: T) -> Boolean,
    ): List<T> = listOf(item) + items.filterNot { isSame(it, item) }

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
            val workshops = async { fetchWithRetry { api.listWebinars(token) } }
            val adhyayan = async { fetchWithRetry { api.listAdhyayanSessions(token, mine = true) } }

            val workshopResult = workshops.await()
            val adhyayanResult = adhyayan.await()
            val warning = config.await()

            // Prefer a combined message only when both fail; otherwise keep partial lists.
            val error = when {
                workshopResult.isFailure && adhyayanResult.isFailure ->
                    workshopResult.exceptionOrNull()?.message
                        ?: adhyayanResult.exceptionOrNull()?.message

                workshopResult.isFailure -> workshopResult.exceptionOrNull()?.message
                adhyayanResult.isFailure -> adhyayanResult.exceptionOrNull()?.message
                else -> null
            }

            _state.update { current ->
                val nextWorkshops = workshopResult.getOrElse { current.workshops }
                val nextAdhyayan = adhyayanResult.getOrElse { current.adhyayanSessions }
                current.copy(
                    workshops = nextWorkshops,
                    adhyayanSessions = nextAdhyayan,
                    isLoading = false,
                    hasLoadedOnce = true,
                    errorMessage = error,
                    minVersionWarning = warning,
                )
            }
        }
    }

    private suspend fun <T> fetchWithRetry(block: suspend () -> T): Result<T> {
        val first = runCatching { block() }
        if (first.isSuccess) return first
        delay(400)
        return runCatching { block() }
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

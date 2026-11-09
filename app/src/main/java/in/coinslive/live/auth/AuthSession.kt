package `in`.coinslive.live.auth

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.user.delete
import `in`.coinslive.live.api.CoinsLiveApiException
import `in`.coinslive.live.storage.WebinarAccessStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Wraps Clerk for the rest of the app. Session JWT is used as
 * `Authorization: Bearer …` against the Next.js APIs.
 */
class AuthSession(
    scope: CoroutineScope,
    /** False when the publishable key is missing/invalid (Clerk was not initialised). */
    val isConfigured: Boolean,
    val configurationError: String?,
    private val accessStore: WebinarAccessStore,
) {
    /** True once Clerk finished loading its client state. */
    val isInitialized: StateFlow<Boolean> = Clerk.isInitialized

    val isSignedIn: StateFlow<Boolean> =
        Clerk.userFlow
            .map { it != null }
            .stateIn(scope, SharingStarted.Eagerly, Clerk.userFlow.value != null)

    val hasSession: Boolean get() = Clerk.userFlow.value != null

    /** Session JWT for `Authorization: Bearer …`. */
    suspend fun bearerToken(forceRefresh: Boolean = false): String {
        if (Clerk.userFlow.value == null) throw CoinsLiveApiException.unauthorized()
        return when (val result = Clerk.auth.getToken(GetTokenOptions(skipCache = forceRefresh))) {
            is ClerkResult.Success -> result.value.takeIf { it.isNotBlank() }
                ?: throw CoinsLiveApiException("Could not refresh your session. Sign out and sign in again.")

            is ClerkResult.Failure -> throw CoinsLiveApiException(
                result.errorMessage?.takeIf { it.isNotBlank() }
                    ?: "Could not refresh your session. Sign out and sign in again.",
            )
        }
    }

    suspend fun signOut() {
        runCatching { Clerk.auth.signOut() }
        accessStore.clearAll()
    }

    /**
     * Permanently deletes the Clerk account. Backend rows are removed via the
     * `user.deleted` webhook. Requires Clerk Dashboard → "Allow users to delete their accounts".
     */
    suspend fun deleteAccount() {
        val user = Clerk.userFlow.value ?: throw CoinsLiveApiException.unauthorized()
        when (val result = user.delete()) {
            is ClerkResult.Success -> {
                runCatching { Clerk.auth.signOut() }
                accessStore.clearAll()
            }

            is ClerkResult.Failure -> throw CoinsLiveApiException(
                result.errorMessage?.takeIf { it.isNotBlank() }
                    ?: "Could not delete your account. Try again or email sachin@coinslive.in.",
            )
        }
    }
}

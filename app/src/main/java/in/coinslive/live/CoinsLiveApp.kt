package `in`.coinslive.live

import android.app.Application
import com.clerk.api.Clerk
import `in`.coinslive.live.api.CoinsLiveApiClient
import `in`.coinslive.live.auth.AuthSession
import `in`.coinslive.live.config.AppConfig
import `in`.coinslive.live.storage.WebinarAccessStore
import `in`.coinslive.live.util.CrashLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CoinsLiveApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var api: CoinsLiveApiClient
        private set
    lateinit var accessStore: WebinarAccessStore
        private set
    lateinit var auth: AuthSession
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashLog.install(this)

        accessStore = WebinarAccessStore(this)
        api = CoinsLiveApiClient()

        // Must run before any composable reads Clerk state.
        val key = AppConfig.clerkPublishableKey
        var configured = false
        var configError: String? = null
        if (key.isEmpty()) {
            configError = "Sign in is not configured for this build."
        } else {
            try {
                Clerk.initialize(this, key)
                configured = true
            } catch (e: Exception) {
                configError = "Sign in is not configured for this build."
            }
        }
        auth = AuthSession(appScope, configured, configError, accessStore)
    }

    companion object {
        lateinit var instance: CoinsLiveApp
            private set
    }
}

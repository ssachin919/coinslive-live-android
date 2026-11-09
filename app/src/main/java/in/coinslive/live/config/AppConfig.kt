package `in`.coinslive.live.config

import `in`.coinslive.live.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object AppConfig {
    const val DEEP_LINK_SCHEME = "coinslive"
    const val DEFAULT_API_BASE_URL = "https://www.coinslive.in"

    /**
     * Base URL for CoinsLive Next.js APIs (no trailing slash).
     * Always prefer the canonical `www` host — apex → www redirects strip `Authorization`.
     */
    val apiBaseUrl: String
        get() = normalizeBaseUrl(BuildConfig.API_BASE_URL)

    val apiHost: String
        get() = apiBaseUrl.toHttpUrlOrNull()?.host ?: "www.coinslive.in"

    /** Empty when unset / placeholder. */
    val clerkPublishableKey: String
        get() {
            val raw = BuildConfig.CLERK_PUBLISHABLE_KEY.trim()
            if (raw.isEmpty() || raw.contains("REPLACE") || raw.startsWith("\$(")) return ""
            return raw
        }

    fun webinarShareUrl(slug: String): String = "$apiBaseUrl/workshop/$slug"

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.contains("REPLACE")) return DEFAULT_API_BASE_URL
        val url = trimmed.toHttpUrlOrNull() ?: return DEFAULT_API_BASE_URL
        val builder = url.newBuilder()
        if (url.host == "coinslive.in") builder.host("www.coinslive.in")
        val result = builder.build().toString()
        return result.trimEnd('/')
    }
}

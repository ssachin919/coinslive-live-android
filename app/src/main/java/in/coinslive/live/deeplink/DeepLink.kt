package `in`.coinslive.live.deeplink

import android.content.Intent
import android.net.Uri
import `in`.coinslive.live.api.LiveJoinRequest
import `in`.coinslive.live.api.LiveRoomKind
import `in`.coinslive.live.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Parses:
 * - `coinslive://workshop/{slug}/live`, `coinslive://adhyayan/{slug}/live`
 * - `https://www.coinslive.in/workshop/{slug}/live`, `https://www.coinslive.in/adhyayan/{slug}/live`
 */
object DeepLink {
    private val httpsHosts = setOf("www.coinslive.in", "coinslive.in")

    fun parse(uri: Uri?): LiveJoinRequest? {
        uri ?: return null
        return when (uri.scheme?.lowercase()) {
            AppConfig.DEEP_LINK_SCHEME -> parseCustomScheme(uri)
            "https", "http" -> parseHttps(uri)
            else -> null
        }
    }

    fun parse(intent: Intent?): LiveJoinRequest? = parse(intent?.data)

    private fun parseCustomScheme(uri: Uri): LiveJoinRequest? {
        val segments = buildList {
            uri.host?.takeIf { it.isNotEmpty() }?.let(::add)
            addAll(uri.pathSegments.filter { it.isNotEmpty() })
        }
        return fromSegments(segments)
    }

    private fun parseHttps(uri: Uri): LiveJoinRequest? {
        val host = uri.host?.lowercase() ?: return null
        if (host !in httpsHosts) return null
        return fromSegments(uri.pathSegments.filter { it.isNotEmpty() })
    }

    private fun fromSegments(segments: List<String>): LiveJoinRequest? {
        if (segments.size < 2) return null
        val slug = segments[1]
        return when (segments[0].lowercase()) {
            "workshop", "webinars" -> LiveJoinRequest(LiveRoomKind.WEBINAR, slug, asHost = true)
            "adhyayan" -> LiveJoinRequest(LiveRoomKind.ADHYAYAN, slug, asHost = true)
            else -> null
        }
    }
}

/** Holds a deep link until the user is signed in and navigation is ready. */
object DeepLinkInbox {
    private val _pending = MutableStateFlow<LiveJoinRequest?>(null)
    val pending: StateFlow<LiveJoinRequest?> = _pending

    fun offer(request: LiveJoinRequest?) {
        if (request != null) _pending.value = request
    }

    fun consume(request: LiveJoinRequest) {
        if (_pending.value == request) _pending.value = null
    }
}

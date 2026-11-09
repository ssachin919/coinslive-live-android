package `in`.coinslive.live.storage

import android.content.Context
import android.content.SharedPreferences

/** Persists workshop attendee access tokens from `/api/workshop/register/`. */
class WebinarAccessStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(accessToken: String, slug: String) {
        prefs.edit().putString(PREFIX + slug, accessToken).apply()
    }

    fun token(slug: String): String? = prefs.getString(PREFIX + slug, null)?.takeIf { it.isNotBlank() }

    fun clear(slug: String) {
        prefs.edit().remove(PREFIX + slug).apply()
    }

    /** Removes all persisted workshop access tokens (sign-out / account deletion). */
    fun clearAll() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(PREFIX) }.forEach { editor.remove(it) }
        editor.apply()
    }

    private companion object {
        const val PREFS_NAME = "coinslive_webinar_access"
        const val PREFIX = "webinar.accessToken."
    }
}

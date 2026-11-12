package `in`.coinslive.live.util

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Persists breadcrumbs + the last Java crash so internal testers can report
 * failures without adb. Native kills leave only the last breadcrumb.
 */
object CrashLog {
    private const val PREFS = "coinslive_crash_log"
    private const val KEY_STACK = "stack"
    private const val KEY_AT = "at"
    private const val KEY_BREADCRUMB = "breadcrumb"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, error) }
            previous?.uncaughtException(thread, error) ?: run {
                Log.e("CoinsLiveCrash", "Uncaught on ${thread.name}", error)
            }
        }
    }

    /** Synchronous — must flush before a native kill or process death. */
    fun breadcrumb(context: Context, step: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BREADCRUMB, step)
            .putLong(KEY_AT, System.currentTimeMillis())
            .commit()
        Log.i("CoinsLiveCrash", "breadcrumb: $step")
    }

    fun clearBreadcrumb(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_BREADCRUMB)
            .commit()
    }

    fun write(context: Context, error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        val crumb = readBreadcrumb(context)
        val body = buildString {
            if (!crumb.isNullOrBlank()) appendLine("Last step: $crumb")
            append(sw.toString())
        }
        // commit() so the stack survives process death from the default handler.
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STACK, body)
            .putLong(KEY_AT, System.currentTimeMillis())
            .commit()
    }

    fun readBreadcrumb(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BREADCRUMB, null)
            ?.takeIf { it.isNotBlank() }

    fun read(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stack = prefs.getString(KEY_STACK, null)?.takeIf { it.isNotBlank() }
        val crumb = prefs.getString(KEY_BREADCRUMB, null)?.takeIf { it.isNotBlank() }
        return when {
            stack != null -> stack
            crumb != null -> "Process died during: $crumb\n(No Java stack — likely a native crash.)"
            else -> null
        }
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}

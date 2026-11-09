package `in`.coinslive.live.util

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/** Persists the last Java crash so internal testers can report stacks without adb. */
object CrashLog {
    private const val PREFS = "coinslive_crash_log"
    private const val KEY_STACK = "stack"
    private const val KEY_AT = "at"

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

    fun write(context: Context, error: Throwable) {
        val sw = StringWriter()
        error.printStackTrace(PrintWriter(sw))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STACK, sw.toString())
            .putLong(KEY_AT, System.currentTimeMillis())
            .apply()
    }

    fun read(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STACK, null)
            ?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

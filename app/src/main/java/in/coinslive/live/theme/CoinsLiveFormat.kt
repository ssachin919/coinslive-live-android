package `in`.coinslive.live.theme

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CoinsLiveFormat {
    private val parsePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
    )

    fun statusLabel(status: String): String = when (status.lowercase()) {
        "live" -> "Live"
        "backstage" -> "Prep"
        "scheduled" -> "Scheduled"
        "ended" -> "Ended"
        else -> status.replaceFirstChar { it.uppercase() }
    }

    fun capitalize(value: String): String = value.replaceFirstChar { it.uppercase() }

    fun parseIso(raw: String): Date? {
        for (pattern in parsePatterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.isLenient = false
                return format.parse(raw) ?: continue
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    fun scheduledLabel(raw: String): String? {
        val date = parseIso(raw) ?: return null
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
    }

    fun dateTimeLabel(millis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

    /** ISO-8601 UTC with millisecond precision (matches iOS formatter). */
    fun iso8601(millis: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(millis))
    }

    /** True when [current] < [minimum] using numeric dotted comparison. */
    fun isVersionOlder(current: String, minimum: String): Boolean {
        val a = current.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val b = minimum.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }
}

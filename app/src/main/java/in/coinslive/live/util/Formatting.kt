package `in`.coinslive.live.util

/** Trimmed value, or null when blank. */
fun String.nilIfEmpty(): String? = trim().takeIf { it.isNotEmpty() }

fun String.isBlankTrimmed(): Boolean = trim().isEmpty()

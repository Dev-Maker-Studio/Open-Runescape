package org.darkan.migration

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Parses the date strings the legacy servers stored. They came from Gson's default `Date`
 * serializer, i.e. `DateFormat.getDateTimeInstance(DEFAULT, DEFAULT, Locale.US)`, whose output
 * has drifted across JDKs: Java 8 had no comma before the time, Java 9+ added it, and Java 20+
 * puts a narrow no-break space before AM/PM. All variants are accepted; the year is parsed as a
 * calendar year (`yyyy`), not the week-year the legacy reader mistakenly used.
 */
object LegacyDates {
    private val patterns = listOf(
        "MMM d, yyyy, h:mm:ss a",
        "MMM d, yyyy h:mm:ss a",
    )

    fun parse(text: String): Date? {
        val cleaned = text.replace(' ', ' ').replace(' ', ' ').trim()
        for (pattern in patterns) {
            val format = SimpleDateFormat(pattern, Locale.US)
            format.isLenient = false
            try {
                return format.parse(cleaned)
            } catch (_: ParseException) {
            }
        }
        return null
    }

    /**
     * Converts a stored date value to a real BSON date: legacy strings are parsed, real dates
     * pass through, numbers are taken as epoch millis. Returns null when the value is unreadable.
     */
    fun toDate(value: Any?): Date? = when (value) {
        is Date -> value
        is Number -> Date(value.toLong())
        is String -> parse(value)
        else -> null
    }
}

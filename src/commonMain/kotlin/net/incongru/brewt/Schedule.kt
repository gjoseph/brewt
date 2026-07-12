package net.incongru.brewt

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.math.absoluteValue

enum class Freq { daily, weekly, monthly }

interface Schedule {
    override fun toString(): String;

    /**
     * Returns this schedule as a plist Dictionary XML
     */
    fun toDictXML(): String;
    // Minute <integer> The minute (0-59) on which this job will be run.
    // Hour <integer> The hour (0-23) on which this job will be run.
    // Day <integer> The day of the month (1-31) on which this job will be run.
    // Weekday <integer> The weekday on which this job will be run (0 and 7 are Sunday).
    // If both Day and Weekday are specificed [sic], then the job will be started if either one matches the current date.
    // Month <integer> The month (1-12) on which this job will be run.
}

/**
 * day of month is a bit of a silly parameter; something more useful _might_ be first <day-of-week>, Nth <day-of-week>, last <day of week>.
 *
 */
data class MonthlySchedule(val dayOfMonth: Int, val time: LocalTime) : Schedule {
    override fun toString(): String {
        return "monthly on the ${dayOfMonth.toOrdinalString} at ${time}"
    }

    override fun toDictXML(): String {
        TODO("Not yet implemented")
    }
}

data class WeeklySchedule(val dayOfWeek: DayOfWeek, val time: LocalTime) : Schedule {
    override fun toString(): String {
        return "weekly on ${dayOfWeek} at ${time}"
    }

    override fun toDictXML(): String {
        TODO("Not yet implemented")
    }
}

data class DailySchedule(
    val time: LocalTime,
) : Schedule {
    override fun toString(): String {
        return "Daily at ${time}"
    }

    override fun toDictXML(): String {
        // validate values/combos here?
        return """
            <dict>
            <key>Hour</key>
            <integer>${time.hour}</integer>
            <key>Minute</key>
            <integer>${time.minute}</integer>
            </dict>
        """.trimIndent()
    }
}

val Int.toOrdinalString: String
    get() {
        val abs = this.absoluteValue

        // teen exceptions (11, 12, 13)
        if (abs % 100 in 11..13) {
            return "${this}th"
        }

        // standard rule based on last digit
        val suffix = when (abs % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }

        return "$this$suffix"
    }
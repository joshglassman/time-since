package com.joshmermelstein.timesince.presentation.format

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Formats a remaining duration as a short, human-readable string.
 *
 * Examples:
 *   3.days + 4.hours        -> "3d 4h"
 *   5.hours + 30.minutes    -> "5h 30m"
 *   45.minutes              -> "45m"
 *   (-2).hours              -> "2h overdue"
 *   Duration.ZERO           -> "now"
 */
object RemainingTimeFormatter {

    fun format(remaining: Duration): String {
        if (remaining == Duration.ZERO) return "now"
        if (remaining.isNegative()) {
            return "${formatPositive(-remaining)} overdue"
        }
        return formatPositive(remaining)
    }

    private fun formatPositive(duration: Duration): String {
        // Round to at least 1 minute so very small values don't display as "0m"
        val rounded = if (duration < 1.minutes) 1.minutes else duration

        val totalDays = rounded.inWholeDays
        val totalHours = rounded.inWholeHours
        val totalMinutes = rounded.inWholeMinutes

        val hourPart = totalHours - totalDays * 24
        val minutePart = totalMinutes - totalHours * 60

        return when {
            totalDays > 0 && hourPart > 0 -> "${totalDays}d ${hourPart}h"
            totalDays > 0 -> "${totalDays}d"
            totalHours > 0 && minutePart > 0 -> "${totalHours}h ${minutePart}m"
            totalHours > 0 -> "${totalHours}h"
            else -> "${minutePart}m"
        }
    }
}

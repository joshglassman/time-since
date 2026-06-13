package com.scribbles.timesince.presentation.format

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskFrequency
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlin.time.Instant

/**
 * Formats the time elapsed since completion alongside the task's frequency,
 * e.g. `"1w 3d / 3m"`.
 *
 * Both sides read in their natural calendar units. The elapsed side is a
 * calendar-correct breakdown computed in [tz] (so month and year boundaries
 * depend on the real calendar rather than a fixed day count): it shows the most
 * significant non-zero unit, plus the immediately-adjacent next unit only when
 * that one is also non-zero — e.g. `y m`, `m w`, `w d`, `d h`, `h m`, but
 * `1y 0m 0w 1d` collapses to `"1y"`. The frequency side renders the cadence as
 * written — `"1w"`, `"3m"`, `"1y"` rather than `"7d"`, `"90d"`, `"365d"`.
 */
object TimeSinceFormatter {

    fun format(
        lastCompletedAt: Instant,
        now: Instant,
        tz: TimeZone,
        frequency: TaskFrequency,
    ): String = "${formatElapsed(lastCompletedAt, now, tz)} / ${frequencyLabel(frequency)}"

    /**
     * Renders the calendar time between [start] and [now] in [tz]. Shows the
     * most significant non-zero unit (years, months, weeks, days, hours,
     * minutes) plus the immediately-following unit only when it too is non-zero.
     * Non-positive spans clamp to `"0m"`; a positive span under a minute rounds
     * up to `"1m"`.
     */
    fun formatElapsed(start: Instant, now: Instant, tz: TimeZone): String {
        if (now <= start) return "0m"

        val period = start.periodUntil(now, tz)
        val components = listOf(
            period.years to "y",
            period.months to "m",
            period.days / 7 to "w",
            period.days % 7 to "d",
            period.hours to "h",
            period.minutes to "m",
        )
        val top = components.indexOfFirst { (value, _) -> value > 0 }
        if (top == -1) return "1m" // positive span under a minute

        return buildString {
            val (value, suffix) = components[top]
            append("$value$suffix")
            val next = components.getOrNull(top + 1)
            if (next != null && next.first > 0) append(" ${next.first}${next.second}")
        }
    }

    private fun frequencyLabel(frequency: TaskFrequency): String {
        val suffix = when (frequency.unit) {
            FrequencyUnit.HOURS -> "h"
            FrequencyUnit.DAYS -> "d"
            FrequencyUnit.WEEKS -> "w"
            FrequencyUnit.MONTHS -> "m"
            FrequencyUnit.YEARS -> "y"
        }
        return "${frequency.amount}$suffix"
    }
}

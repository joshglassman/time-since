package com.scribbles.timesince.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

data class TaskFrequency(
    val amount: Int,
    val unit: FrequencyUnit,
) {
    init {
        require(amount > 0) { "Frequency amount must be positive" }
    }

    /**
     * Advances [from] by this frequency using calendar arithmetic in [tz].
     *
     * `HOURS` advances by real elapsed time, so crossing a DST boundary may
     * shift the wall-clock time. `DAYS`/`WEEKS`/`MONTHS`/`YEARS` land on the
     * same local wall-clock time regardless of DST, and month/year overflow
     * clamps conventionally (Jan 31 + 1 month -> Feb 28/29).
     */
    fun advance(from: Instant, tz: TimeZone): Instant = when (unit) {
        FrequencyUnit.HOURS -> from + amount.hours
        FrequencyUnit.DAYS -> from.plus(amount, DateTimeUnit.DAY, tz)
        FrequencyUnit.WEEKS -> from.plus(amount * 7, DateTimeUnit.DAY, tz)
        FrequencyUnit.MONTHS -> from.plus(amount, DateTimeUnit.MONTH, tz)
        FrequencyUnit.YEARS -> from.plus(amount, DateTimeUnit.YEAR, tz)
    }

    /**
     * A fixed-duration approximation of this frequency, for **display/labeling
     * only**. Imprecise for calendar units (months are 30 days, years 365) —
     * never feed this into deadline/status/fill math; use [advance] instead.
     */
    fun approxDuration(): Duration = when (unit) {
        FrequencyUnit.HOURS -> amount.hours
        FrequencyUnit.DAYS -> amount.days
        FrequencyUnit.WEEKS -> (amount * 7).days
        FrequencyUnit.MONTHS -> (amount * 30).days
        FrequencyUnit.YEARS -> (amount * 365).days
    }

    override fun toString(): String = "$amount $unit"
}

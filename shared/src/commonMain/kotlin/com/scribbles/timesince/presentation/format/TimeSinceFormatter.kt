package com.scribbles.timesince.presentation.format

import com.scribbles.timesince.domain.model.TaskFrequency
import kotlin.time.Duration

/**
 * Formats an elapsed duration alongside the task's frequency, e.g. `"1d 5h / 5d"`.
 *
 * Negative or zero [elapsed] clamps to `"0m"`. Both parts use the same compact
 * `d/h/m` style as [RemainingTimeFormatter].
 */
object TimeSinceFormatter {

    fun format(elapsed: Duration, frequency: TaskFrequency): String {
        val elapsedPart = if (elapsed.isNegative() || elapsed == Duration.ZERO) {
            "0m"
        } else {
            formatShort(elapsed)
        }
        val frequencyPart = formatShort(frequency.toDuration())
        return "$elapsedPart / $frequencyPart"
    }
}

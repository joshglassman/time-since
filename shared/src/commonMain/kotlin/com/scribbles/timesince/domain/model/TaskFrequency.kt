package com.scribbles.timesince.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

data class TaskFrequency(
    val amount: Int,
    val unit: FrequencyUnit,
) {
    init {
        require(amount > 0) { "Frequency amount must be positive" }
    }

    fun toDuration(): Duration = when (unit) {
        FrequencyUnit.HOURS -> amount.hours
        FrequencyUnit.DAYS -> amount.days
        FrequencyUnit.WEEKS -> (amount * 7).days
        FrequencyUnit.MONTHS -> (amount * 30).days
    }

    override fun toString(): String = "$amount $unit"
}

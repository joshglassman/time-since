package com.scribbles.timesince.domain.model

enum class FrequencyUnit {
    HOURS,
    DAYS,
    WEEKS,
    MONTHS;

    override fun toString(): String = when (this) {
        HOURS -> "hours"
        DAYS -> "days"
        WEEKS -> "weeks"
        MONTHS -> "months"
    }
}

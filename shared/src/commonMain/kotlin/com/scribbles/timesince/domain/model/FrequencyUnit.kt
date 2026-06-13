package com.scribbles.timesince.domain.model

enum class FrequencyUnit {
    HOURS,
    DAYS,
    WEEKS,
    MONTHS,
    YEARS;

    override fun toString(): String = when (this) {
        HOURS -> "hours"
        DAYS -> "days"
        WEEKS -> "weeks"
        MONTHS -> "months"
        YEARS -> "years"
    }
}

package com.scribbles.timesince.presentation.taskedit

import com.scribbles.timesince.domain.model.FrequencyUnit
import kotlin.time.Instant

data class TaskEditUiState(
    val isLoading: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val frequencyAmount: String = "1",
    val frequencyUnit: FrequencyUnit = FrequencyUnit.DAYS,
    val lastCompletedAt: Instant? = null,
    val snoozeAmount: String = "1",
    val snoozeUnit: FrequencyUnit = FrequencyUnit.DAYS,
    val canUndo: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && (frequencyAmount.toIntOrNull() ?: 0) > 0

    /**
     * Snooze is available for an already-saved task with a valid (non-negative)
     * amount. Zero is allowed — it pulls an overdue deadline up to *now*.
     */
    val canSnooze: Boolean
        get() = !isNew && snoozeAmount.toIntOrNull() != null
}

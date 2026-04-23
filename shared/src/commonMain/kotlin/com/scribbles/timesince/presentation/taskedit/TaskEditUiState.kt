package com.scribbles.timesince.presentation.taskedit

import com.scribbles.timesince.domain.model.FrequencyUnit

data class TaskEditUiState(
    val isLoading: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val frequencyAmount: String = "1",
    val frequencyUnit: FrequencyUnit = FrequencyUnit.DAYS,
    val saved: Boolean = false,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && (frequencyAmount.toIntOrNull() ?: 0) > 0
}

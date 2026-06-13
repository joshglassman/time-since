package com.scribbles.timesince.domain.model

import kotlin.time.Instant

/**
 * A user-defined, optional task category. [colorHex] is a raw hex string chosen
 * from [CATEGORY_PALETTE]. [updatedAt] is the last-writer-wins basis for sync
 * (stamped on every local mutation, like [Task.updatedAt]).
 */
data class Category(
    val id: String,
    val name: String,
    val colorHex: String,
    val updatedAt: Instant,
    /** Optional unicode emoji shown as `icon name` and in the corner badge; `""` = none. */
    val icon: String = "",
)

package com.scribbles.timesince.domain.model

import kotlin.time.Instant

/** Records a deleted category so the deletion propagates across devices via sync. */
data class DeletedCategoryTombstone(
    val id: String,
    val deletedAt: Instant,
)

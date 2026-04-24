package com.scribbles.timesince.domain.model

import kotlin.time.Instant

data class DeletedTaskTombstone(
    val id: String,
    val deletedAt: Instant,
)

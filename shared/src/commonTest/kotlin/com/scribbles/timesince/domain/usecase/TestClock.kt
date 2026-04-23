package com.scribbles.timesince.domain.usecase

import kotlin.time.Instant
import kotlin.time.Clock

class TestClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

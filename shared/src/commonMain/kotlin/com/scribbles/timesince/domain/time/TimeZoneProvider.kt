package com.scribbles.timesince.domain.time

import kotlinx.datetime.TimeZone

/**
 * Resolves the [TimeZone] used for calendar-correct deadline arithmetic.
 *
 * `TimeZone` is resolved only at the edge (DI / ViewModel) and threaded into
 * domain functions as a parameter, so tests stay deterministic. The production
 * binding returns [TimeZone.currentSystemDefault]; tests pass a fixed zone.
 */
fun interface TimeZoneProvider {
    fun current(): TimeZone

    companion object {
        val System: TimeZoneProvider = TimeZoneProvider { TimeZone.currentSystemDefault() }
    }
}

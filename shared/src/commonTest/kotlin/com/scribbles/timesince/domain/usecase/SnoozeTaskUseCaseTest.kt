package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.deadline
import com.scribbles.timesince.domain.undo.UndoStore
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SnoozeTaskUseCaseTest {

    private val repository = FakeTaskRepository()
    private val undoStore = UndoStore()
    private val utc = TimeZone.UTC

    private fun useCaseAt(now: Instant) =
        SnoozeTaskUseCase(repository, undoStore, TestClock(now), UTC_PROVIDER)

    @Test
    fun snoozingOverdueTaskPushesDeadlineToNowPlusN() = runTest {
        // 1-day task last completed at BASE_TIME -> natural deadline BASE_TIME + 1d.
        repository.create(taskWith(id = "1", frequencyAmount = 1, frequencyUnit = FrequencyUnit.DAYS))
        val now = BASE_TIME + 5.days // already overdue

        useCaseAt(now)("1", 3, FrequencyUnit.DAYS)

        val task = repository.getById("1")!!
        assertEquals(now + 3.days, task.deadline(utc))
    }

    @Test
    fun snoozeIsFlooredAtNaturalDeadline() = runTest {
        // 10-day task: natural deadline BASE_TIME + 10d, well in the future.
        repository.create(taskWith(id = "1", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS))
        val now = BASE_TIME + 1.days

        // "now + 2d" lands before the natural deadline, so snooze stays zero.
        useCaseAt(now)("1", 2, FrequencyUnit.DAYS)

        val task = repository.getById("1")!!
        assertEquals(Duration.ZERO, task.snooze)
        assertEquals(BASE_TIME + 10.days, task.deadline(utc))
    }

    @Test
    fun reSnoozingReducesExistingSnoozeButNeverBelowNaturalDeadline() = runTest {
        // Existing large snooze on a 1-day task.
        repository.create(
            taskWith(id = "1", frequencyAmount = 1, frequencyUnit = FrequencyUnit.DAYS, snooze = 20.days),
        )
        val now = BASE_TIME + 5.days

        // Snooze 3 days from now -> deadline now + 3d, smaller than the prior snooze.
        useCaseAt(now)("1", 3, FrequencyUnit.DAYS)

        val task = repository.getById("1")!!
        assertEquals(now + 3.days, task.deadline(utc))
        assertTrue(task.snooze >= Duration.ZERO)
    }

    @Test
    fun snoozeAmountUsesCalendarArithmetic() = runTest {
        repository.create(
            taskWith(
                id = "1",
                lastCompletedAt = Instant.parse("2026-01-10T09:00:00Z"),
                frequencyAmount = 1,
                frequencyUnit = FrequencyUnit.DAYS,
            ),
        )
        // now in January; "snooze 1 month" lands on the same wall-clock a month later.
        val now = Instant.parse("2026-01-15T09:00:00Z")

        useCaseAt(now)("1", 1, FrequencyUnit.MONTHS)

        val task = repository.getById("1")!!
        assertEquals(Instant.parse("2026-02-15T09:00:00Z"), task.deadline(utc))
    }

    @Test
    fun zeroAmountClearsAnExistingSnooze() = runTest {
        repository.create(
            taskWith(id = "1", frequencyAmount = 1, frequencyUnit = FrequencyUnit.DAYS, snooze = 5.days),
        )
        val now = BASE_TIME + 5.days // overdue (natural deadline BASE_TIME + 1d)

        useCaseAt(now)("1", 0, FrequencyUnit.DAYS)

        val task = repository.getById("1")!!
        assertEquals(Duration.ZERO, task.snooze)
        // Deadline falls back to the natural deadline (no snooze).
        assertEquals(BASE_TIME + 1.days, task.deadline(utc))
    }

    @Test
    fun zeroAmountSnoozeOnFutureTaskLeavesSnoozeZero() = runTest {
        repository.create(taskWith(id = "1", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS))
        val now = BASE_TIME + 1.days // not yet due

        useCaseAt(now)("1", 0, FrequencyUnit.DAYS)

        assertEquals(Duration.ZERO, repository.getById("1")!!.snooze)
    }

    @Test
    fun snoozingRecordsUndoSnapshot() = runTest {
        repository.create(
            taskWith(id = "1", lastCompletedAt = BASE_TIME, frequencyAmount = 1, snooze = Duration.ZERO),
        )

        useCaseAt(BASE_TIME + 5.days)("1", 3, FrequencyUnit.DAYS)

        val snapshot = undoStore.take("1")
        assertNotNull(snapshot)
        assertEquals(BASE_TIME, snapshot.prevLastCompletedAt)
        assertEquals(Duration.ZERO, snapshot.prevSnooze)
    }
}

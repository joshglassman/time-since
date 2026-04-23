package com.scribbles.timesince.data.sync

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncPayloadTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sample = listOf(
        Task(
            id = "id-1",
            name = "Water plants",
            lastCompletedAt = Instant.parse("2026-04-08T10:30:00Z"),
            frequency = TaskFrequency(3, FrequencyUnit.DAYS),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        ),
        Task(
            id = "id-2",
            name = "Change air filter",
            lastCompletedAt = Instant.parse("2026-01-15T09:00:00Z"),
            frequency = TaskFrequency(3, FrequencyUnit.MONTHS),
            createdAt = Instant.parse("2025-12-01T00:00:00Z"),
        ),
    )

    private val syncTime = Instant.parse("2026-04-10T12:00:00Z")

    @Test
    fun roundTripPreservesAllFields() {
        val payload = SyncPayload.from(sample, syncTime)
        val encoded = json.encodeToString(SyncPayload.serializer(), payload)
        val decoded = json.decodeFromString(SyncPayload.serializer(), encoded)
        assertEquals(sample, decoded.toTasks())
    }

    @Test
    fun emptyTaskListRoundTrips() {
        val payload = SyncPayload.from(emptyList(), syncTime)
        val encoded = json.encodeToString(SyncPayload.serializer(), payload)
        val decoded = json.decodeFromString(SyncPayload.serializer(), encoded)
        assertEquals(emptyList(), decoded.toTasks())
    }

    @Test
    fun malformedTaskDtoIsSkipped() {
        val payload = SyncPayload(
            syncedAt = syncTime.toString(),
            tasks = listOf(
                TaskDto(
                    id = "id-1",
                    name = "Good",
                    lastCompletedAt = "2026-04-08T10:30:00Z",
                    frequencyAmount = 3,
                    frequencyUnit = "DAYS",
                    createdAt = "2026-01-01T00:00:00Z",
                ),
                TaskDto(
                    id = "id-2",
                    name = "Bad unit",
                    lastCompletedAt = "2026-04-08T10:30:00Z",
                    frequencyAmount = 1,
                    frequencyUnit = "FORTNIGHTS",
                    createdAt = "2026-01-01T00:00:00Z",
                ),
            ),
        )
        val tasks = payload.toTasks()
        assertEquals(1, tasks.size)
        assertEquals("Good", tasks.first().name)
    }

    @Test
    fun syncedAtTimestampIsPreserved() {
        val payload = SyncPayload.from(sample, syncTime)
        val encoded = json.encodeToString(SyncPayload.serializer(), payload)
        val decoded = json.decodeFromString(SyncPayload.serializer(), encoded)
        assertEquals(syncTime.toString(), decoded.syncedAt)
    }

    @Test
    fun allFrequencyUnitsRoundTrip() {
        for (unit in FrequencyUnit.entries) {
            val task = Task(
                id = "u-${unit.name}",
                name = "Test $unit",
                lastCompletedAt = syncTime,
                frequency = TaskFrequency(1, unit),
                createdAt = syncTime,
            )
            val payload = SyncPayload.from(listOf(task), syncTime)
            val encoded = json.encodeToString(SyncPayload.serializer(), payload)
            val decoded = json.decodeFromString(SyncPayload.serializer(), encoded)
            assertEquals(listOf(task), decoded.toTasks())
        }
    }
}

package com.joshmermelstein.timesince.data.export

import com.joshmermelstein.timesince.domain.model.FrequencyUnit
import com.joshmermelstein.timesince.domain.model.Task
import com.joshmermelstein.timesince.domain.model.TaskFrequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class MarkdownExportImportTest {

    private val sample = listOf(
        Task(
            id = "550e8400-e29b-41d4-a716-446655440000",
            name = "Water plants",
            lastCompletedAt = Instant.parse("2026-04-08T10:30:00Z"),
            frequency = TaskFrequency(3, FrequencyUnit.DAYS),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        ),
        Task(
            id = "660e8400-e29b-41d4-a716-446655440001",
            name = "Change air filter",
            lastCompletedAt = Instant.parse("2026-01-15T09:00:00Z"),
            frequency = TaskFrequency(3, FrequencyUnit.MONTHS),
            createdAt = Instant.parse("2025-12-01T00:00:00Z"),
        ),
    )

    @Test
    fun emptyListExportsHeaderOnly() {
        val output = MarkdownExporter.export(emptyList())
        assertTrue(output.contains("# Time Since Tasks"))
        assertEquals(emptyList(), MarkdownImporter.import(output))
    }

    @Test
    fun roundTripPreservesAllFields() {
        val markdown = MarkdownExporter.export(sample)
        val parsed = MarkdownImporter.import(markdown)
        assertEquals(sample, parsed)
    }

    @Test
    fun roundTripSingleTask() {
        val single = listOf(sample.first())
        val parsed = MarkdownImporter.import(MarkdownExporter.export(single))
        assertEquals(single, parsed)
    }

    @Test
    fun importTolerantOfExtraBlankLines() {
        val markdown = """
            # Time Since Tasks


            ## Water plants

            - frequency: 3 days
            - last completed: 2026-04-08T10:30:00Z
            - created: 2026-01-01T00:00:00Z
            - id: 550e8400-e29b-41d4-a716-446655440000


        """.trimIndent()
        val parsed = MarkdownImporter.import(markdown)
        assertEquals(1, parsed.size)
        assertEquals("Water plants", parsed.first().name)
    }

    @Test
    fun importSkipsBlocksMissingRequiredFields() {
        val markdown = """
            # Time Since Tasks

            ## Incomplete task
            - frequency: 1 days

            ## Complete task
            - frequency: 2 hours
            - last completed: 2026-04-08T10:30:00Z
            - created: 2026-01-01T00:00:00Z
            - id: abc-123
        """.trimIndent()
        val parsed = MarkdownImporter.import(markdown)
        assertEquals(1, parsed.size)
        assertEquals("Complete task", parsed.first().name)
    }

    @Test
    fun importIgnoresMalformedFrequencyAmount() {
        val markdown = """
            # Time Since Tasks

            ## Bad
            - frequency: 0 days
            - last completed: 2026-04-08T10:30:00Z
            - created: 2026-01-01T00:00:00Z
            - id: x
        """.trimIndent()
        val parsed = MarkdownImporter.import(markdown)
        assertEquals(0, parsed.size)
    }

    @Test
    fun importIgnoresUnknownFrequencyUnit() {
        val markdown = """
            # Time Since Tasks

            ## Bad
            - frequency: 5 fortnights
            - last completed: 2026-04-08T10:30:00Z
            - created: 2026-01-01T00:00:00Z
            - id: x
        """.trimIndent()
        val parsed = MarkdownImporter.import(markdown)
        assertEquals(0, parsed.size)
    }

    @Test
    fun importIgnoresMalformedInstant() {
        val markdown = """
            # Time Since Tasks

            ## Bad
            - frequency: 1 days
            - last completed: not-a-date
            - created: 2026-01-01T00:00:00Z
            - id: x
        """.trimIndent()
        val parsed = MarkdownImporter.import(markdown)
        assertEquals(0, parsed.size)
    }

    @Test
    fun nameWithSpecialCharactersRoundTrips() {
        val task = Task(
            id = "id-1",
            name = "Take out trash & recycling (Tuesday!)",
            lastCompletedAt = Instant.parse("2026-04-07T08:00:00Z"),
            frequency = TaskFrequency(1, FrequencyUnit.WEEKS),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val parsed = MarkdownImporter.import(MarkdownExporter.export(listOf(task)))
        assertEquals(listOf(task), parsed)
    }

    @Test
    fun completelyMalformedInputProducesEmptyList() {
        val parsed = MarkdownImporter.import("This is not a Time Since markdown file at all.")
        assertEquals(emptyList(), parsed)
    }
}

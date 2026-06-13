package com.scribbles.timesince.data.export

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Parses Markdown produced by [MarkdownExporter] back into [Task] instances.
 *
 * Tolerates extra blank lines, leading/trailing whitespace, and the document
 * header. Each task block must contain `frequency`, `last completed`,
 * `created`, and `id` fields. Blocks missing any required field are skipped.
 */
object MarkdownImporter {

    private val frequencyRegex = Regex("""^- frequency:\s*(\d+)\s+(\w+)\s*$""")
    private val lastCompletedRegex = Regex("""^- last completed:\s*(\S+)\s*$""")
    private val createdRegex = Regex("""^- created:\s*(\S+)\s*$""")
    private val snoozeRegex = Regex("""^- snooze:\s*(\d+)\s*$""")
    private val idRegex = Regex("""^- id:\s*(\S+)\s*$""")
    private val nameRegex = Regex("""^##\s+(.+?)\s*$""")

    fun import(markdown: String): List<Task> {
        val lines = markdown.lines()
        val tasks = mutableListOf<Task>()

        var name: String? = null
        var frequency: TaskFrequency? = null
        var lastCompleted: Instant? = null
        var created: Instant? = null
        var snooze: Duration = Duration.ZERO
        var id: String? = null

        fun flush() {
            val n = name ?: return
            val f = frequency ?: return
            val l = lastCompleted ?: return
            val c = created ?: return
            val i = id ?: return
            tasks += Task(
                id = i,
                name = n,
                lastCompletedAt = l,
                frequency = f,
                createdAt = c,
                snooze = snooze,
            )
        }

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("# ")) continue

            val nameMatch = nameRegex.matchEntire(line)
            if (nameMatch != null) {
                flush()
                name = nameMatch.groupValues[1]
                frequency = null
                lastCompleted = null
                created = null
                snooze = Duration.ZERO
                id = null
                continue
            }

            frequencyRegex.matchEntire(line)?.let { match ->
                val amount = match.groupValues[1].toIntOrNull() ?: return@let
                val unit = parseUnit(match.groupValues[2]) ?: return@let
                if (amount > 0) frequency = TaskFrequency(amount, unit)
                return@let
            }

            lastCompletedRegex.matchEntire(line)?.let { match ->
                lastCompleted = parseInstant(match.groupValues[1])
            }

            createdRegex.matchEntire(line)?.let { match ->
                created = parseInstant(match.groupValues[1])
            }

            snoozeRegex.matchEntire(line)?.let { match ->
                match.groupValues[1].toLongOrNull()?.let { snooze = it.milliseconds }
            }

            idRegex.matchEntire(line)?.let { match ->
                id = match.groupValues[1]
            }
        }
        flush()
        return tasks
    }

    private fun parseUnit(text: String): FrequencyUnit? = when (text.uppercase()) {
        "HOURS" -> FrequencyUnit.HOURS
        "DAYS" -> FrequencyUnit.DAYS
        "WEEKS" -> FrequencyUnit.WEEKS
        "MONTHS" -> FrequencyUnit.MONTHS
        "YEARS" -> FrequencyUnit.YEARS
        else -> null
    }

    private fun parseInstant(text: String): Instant? = try {
        Instant.parse(text)
    } catch (_: IllegalArgumentException) {
        null
    }
}

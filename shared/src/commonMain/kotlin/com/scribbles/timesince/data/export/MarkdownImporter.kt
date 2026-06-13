package com.scribbles.timesince.data.export

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** Tasks and categories parsed from a Markdown document. */
data class ImportResult(
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
)

/**
 * Parses Markdown produced by [MarkdownExporter] back into tasks and categories.
 *
 * Tolerates extra blank lines, leading/trailing whitespace, and the document
 * header. Task blocks must contain `frequency`, `last completed`, `created`, and
 * `id`; blocks missing a required field are skipped. The `## Categories` section
 * lists categories as `- <id> | <colorHex> | <updatedAt> | <name>`.
 */
object MarkdownImporter {

    private val frequencyRegex = Regex("""^- frequency:\s*(\d+)\s+(\w+)\s*$""")
    private val lastCompletedRegex = Regex("""^- last completed:\s*(\S+)\s*$""")
    private val createdRegex = Regex("""^- created:\s*(\S+)\s*$""")
    private val snoozeRegex = Regex("""^- snooze:\s*(\d+)\s*$""")
    private val pausedRegex = Regex("""^- paused:\s*(\d+)\s*$""")
    private val archivedRegex = Regex("""^- archived:\s*(\S+)\s*$""")
    private val categoryRefRegex = Regex("""^- category:\s*(\S+)\s*$""")
    private val categoryLineRegex =
        Regex("""^-\s*(\S+)\s*\|\s*(\S+)\s*\|\s*(\S+)\s*\|\s*(\S*)\s*\|\s*(.+?)\s*$""")
    private val idRegex = Regex("""^- id:\s*(\S+)\s*$""")
    private val nameRegex = Regex("""^##\s+(.+?)\s*$""")

    /** Back-compat single-list entry point: returns only tasks. */
    fun import(markdown: String): List<Task> = importAll(markdown).tasks

    fun importAll(markdown: String): ImportResult {
        val lines = markdown.lines()
        val tasks = mutableListOf<Task>()
        val categories = mutableListOf<Category>()
        var inCategories = false

        var name: String? = null
        var frequency: TaskFrequency? = null
        var lastCompleted: Instant? = null
        var created: Instant? = null
        var snooze: Duration = Duration.ZERO
        var pausedAt: Instant? = null
        var archived = false
        var categoryId: String? = null
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
                pausedAt = pausedAt,
                archived = archived,
                categoryId = categoryId,
            )
        }

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("# ")) continue

            val nameMatch = nameRegex.matchEntire(line)
            if (nameMatch != null) {
                flush()
                val heading = nameMatch.groupValues[1]
                if (heading == CATEGORIES_HEADING) {
                    inCategories = true
                    name = null
                    continue
                }
                inCategories = false
                name = heading
                frequency = null
                lastCompleted = null
                created = null
                snooze = Duration.ZERO
                pausedAt = null
                archived = false
                categoryId = null
                id = null
                continue
            }

            if (inCategories) {
                categoryLineRegex.matchEntire(line)?.let { match ->
                    val updatedAt = parseInstant(match.groupValues[3]) ?: return@let
                    categories += Category(
                        id = match.groupValues[1],
                        name = match.groupValues[5],
                        colorHex = match.groupValues[2],
                        updatedAt = updatedAt,
                        icon = match.groupValues[4],
                    )
                }
                continue
            }

            frequencyRegex.matchEntire(line)?.let { match ->
                val amount = match.groupValues[1].toIntOrNull() ?: return@let
                val unit = parseUnit(match.groupValues[2]) ?: return@let
                if (amount > 0) frequency = TaskFrequency(amount, unit)
                return@let
            }
            lastCompletedRegex.matchEntire(line)?.let { lastCompleted = parseInstant(it.groupValues[1]) }
            createdRegex.matchEntire(line)?.let { created = parseInstant(it.groupValues[1]) }
            snoozeRegex.matchEntire(line)?.let { m -> m.groupValues[1].toLongOrNull()?.let { snooze = it.milliseconds } }
            pausedRegex.matchEntire(line)?.let { m ->
                m.groupValues[1].toLongOrNull()?.let { pausedAt = Instant.fromEpochMilliseconds(it) }
            }
            archivedRegex.matchEntire(line)?.let { archived = it.groupValues[1].equals("true", ignoreCase = true) }
            categoryRefRegex.matchEntire(line)?.let { categoryId = it.groupValues[1] }
            idRegex.matchEntire(line)?.let { id = it.groupValues[1] }
        }
        flush()
        return ImportResult(tasks = tasks, categories = categories)
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

    private const val CATEGORIES_HEADING = "Categories"
}

package com.scribbles.timesince.data.export

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.Task

/**
 * Exports tasks (and categories) to a stable, line-oriented Markdown format.
 *
 * Format:
 * ```
 * # Time Since Tasks
 *
 * ## Categories
 * - <id> | <colorHex> | <updatedAt> | <icon> | <name>
 *
 * ## <task name>
 * - frequency: <amount> <unit>
 * - last completed: <ISO-8601 instant>
 * - created: <ISO-8601 instant>
 * - snooze: <millis>        (only when > 0)
 * - paused: <millis>        (only when paused)
 * - archived: true          (only when archived)
 * - category: <category id> (only when categorized)
 * - id: <uuid>
 * ```
 *
 * Round-trips cleanly through [MarkdownImporter].
 */
object MarkdownExporter {

    fun export(tasks: List<Task>, categories: List<Category> = emptyList()): String = buildString {
        appendLine("# Time Since Tasks")

        if (categories.isNotEmpty()) {
            appendLine()
            appendLine("## Categories")
            for (c in categories) {
                appendLine("- ${c.id} | ${c.colorHex} | ${c.updatedAt} | ${c.icon} | ${c.name}")
            }
        }

        for (task in tasks) {
            appendLine()
            appendLine("## ${task.name}")
            appendLine("- frequency: ${task.frequency.amount} ${task.frequency.unit}")
            appendLine("- last completed: ${task.lastCompletedAt}")
            appendLine("- created: ${task.createdAt}")
            if (task.snooze.isPositive()) {
                appendLine("- snooze: ${task.snooze.inWholeMilliseconds}")
            }
            task.pausedAt?.let { appendLine("- paused: ${it.toEpochMilliseconds()}") }
            if (task.archived) {
                appendLine("- archived: true")
            }
            task.categoryId?.let { appendLine("- category: $it") }
            appendLine("- id: ${task.id}")
        }
    }
}

package com.joshmermelstein.timesince.data.export

import com.joshmermelstein.timesince.domain.model.Task

/**
 * Exports a list of tasks to a stable, line-oriented Markdown format.
 *
 * Format:
 * ```
 * # Time Since Tasks
 *
 * ## <task name>
 * - frequency: <amount> <unit>
 * - last completed: <ISO-8601 instant>
 * - created: <ISO-8601 instant>
 * - id: <uuid>
 * ```
 *
 * The format is designed to round-trip cleanly through [MarkdownImporter].
 */
object MarkdownExporter {

    fun export(tasks: List<Task>): String = buildString {
        appendLine("# Time Since Tasks")
        if (tasks.isEmpty()) return@buildString
        for (task in tasks) {
            appendLine()
            appendLine("## ${task.name}")
            appendLine("- frequency: ${task.frequency.amount} ${task.frequency.unit}")
            appendLine("- last completed: ${task.lastCompletedAt}")
            appendLine("- created: ${task.createdAt}")
            appendLine("- id: ${task.id}")
        }
    }
}

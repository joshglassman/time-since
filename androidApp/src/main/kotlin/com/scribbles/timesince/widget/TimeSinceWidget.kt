package com.scribbles.timesince.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.scribbles.timesince.MainActivity
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.time.TimeZoneProvider
import com.scribbles.timesince.domain.usecase.GetCategoriesUseCase
import com.scribbles.timesince.domain.usecase.GetSortedTasksUseCase
import com.scribbles.timesince.presentation.tasklist.TaskListItem
import com.scribbles.timesince.presentation.tasklist.toListItems
import com.scribbles.timesince.ui.components.parseHexColor
import com.scribbles.timesince.ui.theme.statusDueSoon
import com.scribbles.timesince.ui.theme.statusOk
import com.scribbles.timesince.ui.theme.statusOverdue
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import kotlin.time.Clock

/**
 * Home-screen widget mirroring the app's active task list. Reuses the same
 * use-cases and `toListItems` mapping as `TaskListViewModel`; the row UI is
 * rebuilt in Glance (a separate composition system). Tapping a row opens the app
 * and scrolls the list to that task — there is no tap-to-complete here.
 */
class TimeSinceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val getSortedTasks = koin.get<GetSortedTasksUseCase>()
        val getCategories = koin.get<GetCategoriesUseCase>()
        val clock = koin.get<Clock>()
        val timeZoneProvider = koin.get<TimeZoneProvider>()

        val now = clock.now()
        val tz = timeZoneProvider.current()
        val items = getSortedTasks().first()
            .filter { !it.archived }
            .toListItems(now, tz, getCategories().first())

        provideContent {
            GlanceTheme {
                WidgetBody(items, context)
            }
        }
    }
}

class TimeSinceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimeSinceWidget()
}

@androidx.compose.runtime.Composable
private fun WidgetBody(items: List<TaskListItem>, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(12.dp),
    ) {
        Text(
            text = "Time Since",
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            modifier = GlanceModifier
                .padding(bottom = 6.dp)
                .clickable(actionStartActivity(openAppIntent(context))),
        )
        if (items.isEmpty()) {
            Text(
                text = "No active tasks",
                style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 13.sp),
            )
        } else {
            LazyColumn {
                items(items, itemId = { it.id.hashCode().toLong() }) { item ->
                    TaskRow(item, context)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TaskRow(item: TaskListItem, context: Context) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(actionStartActivity(openTaskIntent(context, item.id))),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(10.dp)
                .cornerRadius(5.dp)
                .background(statusColor(item.status)),
            content = {},
        )
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = item.name,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Medium),
            )
            Text(
                text = item.displayText + indicatorSuffix(item),
                maxLines = 1,
                style = TextStyle(color = statusColor(item.status), fontSize = 12.sp),
            )
        }
        val categoryColor = item.categoryColorHex
        if (categoryColor != null) {
            Spacer(GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .cornerRadius(6.dp)
                    .background(ColorProvider(parseHexColor(categoryColor)))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.categoryIcon.orEmpty().ifEmpty { " " },
                    style = TextStyle(color = ColorProvider(onColor(parseHexColor(categoryColor))), fontSize = 13.sp),
                )
            }
        }
    }
}

private fun indicatorSuffix(item: TaskListItem): String = buildString {
    if (item.isSnoozed) append(" 💤")
    if (item.isPaused) append(" ⏸")
}

private fun statusColor(status: TaskStatus): ColorProvider = when (status) {
    TaskStatus.OK -> ColorProvider(statusOk)
    TaskStatus.DUE_SOON -> ColorProvider(statusDueSoon)
    TaskStatus.OVERDUE -> ColorProvider(statusOverdue)
}

private fun onColor(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White

private fun openAppIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun openTaskIntent(context: Context, taskId: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        // Unique data so each row's PendingIntent is distinct (extras are ignored
        // by PendingIntent.filterEquals).
        data = Uri.parse("timesince://task/$taskId")
        putExtra(MainActivity.EXTRA_TASK_ID, taskId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

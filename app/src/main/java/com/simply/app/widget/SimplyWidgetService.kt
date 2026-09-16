package com.simply.app.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.simply.app.R
import com.simply.app.data.activeTasks
import com.simply.app.data.inMatrixOrder
import com.simply.app.data.AppData
import com.simply.app.data.Repository
import com.simply.app.data.iso
import com.simply.app.data.today
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Список внутри виджета. Обычный ListView в RemoteViews умеет прокрутку,
 * поэтому строк помещается столько, сколько влезает в размер виджета,
 * а не жёсткие четыре, как было раньше.
 */
class SimplyWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val tab = intent.getStringExtra(SimplyWidget.EXTRA_TAB)
            ?.let { runCatching { WidgetTab.valueOf(it) }.getOrNull() }
            ?: WidgetTab.TASKS
        return SimplyWidgetFactory(applicationContext, tab)
    }
}

/** Одна строка списка: что нарисовать и что произойдёт по нажатию. */
private data class WidgetRow(
    val id: String?,
    val title: String,
    val meta: String?,
    val value: String?,
    val done: Boolean,
    val action: String?
)

private val hhmm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private class SimplyWidgetFactory(
    private val context: Context,
    private val tab: WidgetTab
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<WidgetRow> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        rows = buildRows(Repository.get(context).data.value)
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows.getOrNull(position) ?: return RemoteViews(
            context.packageName, R.layout.widget_row
        )
        val views = RemoteViews(context.packageName, R.layout.widget_row)
        views.setTextViewText(R.id.row_title, row.title)
        views.setImageViewResource(
            R.id.row_check,
            when {
                tab == WidgetTab.FOCUS -> R.drawable.ic_widget_timer
                row.done -> R.drawable.ic_widget_check
                tab == WidgetTab.HABITS -> R.drawable.ic_widget_flame
                else -> R.drawable.ic_widget_circle
            }
        )
        if (row.meta.isNullOrBlank()) {
            views.setViewVisibility(R.id.row_meta, View.GONE)
        } else {
            views.setViewVisibility(R.id.row_meta, View.VISIBLE)
            views.setTextViewText(R.id.row_meta, row.meta)
        }
        if (row.value.isNullOrBlank()) {
            views.setViewVisibility(R.id.row_value, View.GONE)
        } else {
            views.setViewVisibility(R.id.row_value, View.VISIBLE)
            views.setTextViewText(R.id.row_value, row.value)
        }

        if (row.action != null && row.id != null) {
            val fillIn = Intent()
                .setAction(row.action)
                .putExtra(SimplyWidget.EXTRA_ID, row.id)
            views.setOnClickFillInIntent(R.id.row_root, fillIn)
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        rows.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun buildRows(data: AppData): List<WidgetRow> = when (tab) {
        WidgetTab.TASKS -> data.activeTasks()
            .filter { !it.done && it.dueDate != null && !it.dueDate!!.isAfter(today()) }
            .inMatrixOrder()
            .map { task ->
                val overdue = task.dueDate?.isBefore(today()) == true
                WidgetRow(
                    id = task.id,
                    title = task.title,
                    meta = when {
                        overdue -> tr("просрочено")
                        else -> task.dueTime
                    },
                    value = if (task.subtasks.isEmpty()) null
                    else "${task.doneSubtasks}/${task.subtasks.size}",
                    done = false,
                    action = SimplyWidget.ACTION_TOGGLE_TASK
                )
            }

        WidgetTab.HABITS -> data.habits.map { habit ->
            val count = habit.countOn(today())
            WidgetRow(
                id = habit.id,
                title = habit.name,
                meta = if (habit.perDay > 1) trf("цель %1\$d в день", habit.perDay) else null,
                value = if (habit.perDay > 1) "$count/${habit.perDay}" else null,
                done = habit.isDoneOn(today()),
                action = SimplyWidget.ACTION_STEP_HABIT
            )
        }

        // Фокус — витрина: последние отрезки, править их можно в приложении.
        WidgetTab.FOCUS -> data.sessions
            .sortedByDescending { it.startedAt }
            .take(20)
            .map { session ->
                val startedAt = runCatching {
                    Instant.ofEpochMilli(session.startedAt)
                        .atZone(ZoneId.systemDefault()).format(hhmm)
                }.getOrDefault("")
                WidgetRow(
                    id = session.id,
                    title = session.targetTitle.ifBlank { tr("Без задачи") },
                    meta = if (session.date == today().iso()) startedAt
                    else trf("%1\$s · %2\$s", session.date, startedAt),
                    value = trf("%1\$d мин", session.minutes),
                    done = false,
                    action = null
                )
            }
    }
}

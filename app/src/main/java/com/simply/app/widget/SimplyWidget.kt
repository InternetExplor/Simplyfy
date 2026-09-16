package com.simply.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.core.net.toUri
import com.simply.app.MainActivity
import com.simply.app.R
import com.simply.app.data.Repository
import com.simply.app.data.dayMonthLabel
import com.simply.app.data.iso
import com.simply.app.data.today
import com.simply.app.pomodoro.PomodoroEngine
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Что показывает виджет. Вкладка запоминается для каждого экземпляра отдельно. */
enum class WidgetTab { TASKS, HABITS, FOCUS }

/**
 * Виджет Simply: задачи, привычки и фокус в одной карточке.
 * Данные читаются из того же репозитория, что и приложение, поэтому
 * отдельного хранилища виджету не нужно, а отметки видны сразу в обоих местах.
 */
class SimplyWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, id)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action !in handledActions) return

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        // Отметку надо успеть дописать на диск: процесс приёмника живёт недолго,
        // а автосохранение репозитория ждёт своей паузы.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching {
                val repo = Repository.get(context)
                when (action) {
                    ACTION_TAB -> {
                        val tab = intent.getStringExtra(EXTRA_TAB) ?: WidgetTab.TASKS.name
                        setTab(context, widgetId, WidgetTab.valueOf(tab))
                    }

                    ACTION_TOGGLE_TASK -> {
                        val id = intent.getStringExtra(EXTRA_ID) ?: return@runCatching
                        repo.update { d ->
                            d.copy(tasks = d.tasks.map {
                                if (it.id != id) it
                                else it.copy(
                                    done = !it.done,
                                    completedAt = if (!it.done) System.currentTimeMillis() else null
                                )
                            })
                        }
                        repo.flush()
                    }

                    ACTION_STEP_HABIT -> {
                        val id = intent.getStringExtra(EXTRA_ID) ?: return@runCatching
                        val key = today().iso()
                        repo.update { d ->
                            d.copy(habits = d.habits.map { h ->
                                if (h.id != id) h else {
                                    val current = h.progress[key] ?: 0
                                    // Цель в один раз — обычная галочка,
                                    // счётчик просто растёт дальше.
                                    val next = if (h.perDay <= 1) (if (current >= 1) 0 else 1)
                                    else current + 1
                                    h.copy(
                                        progress = if (next <= 0) h.progress - key
                                        else h.progress + (key to next)
                                    )
                                }
                            })
                        }
                        repo.flush()
                    }

                    ACTION_FOCUS_TOGGLE -> PomodoroEngine.get(context).toggle()
                }
                refresh(context)
            }
            pending.finish()
        }
    }

    companion object {

        const val ACTION_TAB = "com.simply.app.widget.TAB"
        const val ACTION_TOGGLE_TASK = "com.simply.app.widget.TOGGLE_TASK"
        const val ACTION_STEP_HABIT = "com.simply.app.widget.STEP_HABIT"
        const val ACTION_FOCUS_TOGGLE = "com.simply.app.widget.FOCUS_TOGGLE"
        const val EXTRA_ID = "id"
        const val EXTRA_TAB = "tab"

        private val handledActions = setOf(
            ACTION_TAB, ACTION_TOGGLE_TASK, ACTION_STEP_HABIT, ACTION_FOCUS_TOGGLE
        )

        private const val PREFS = "simply_widget"

        fun tabOf(context: Context, widgetId: Int): WidgetTab {
            val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("tab_$widgetId", null) ?: return WidgetTab.TASKS
            return runCatching { WidgetTab.valueOf(name) }.getOrDefault(WidgetTab.TASKS)
        }

        private fun setTab(context: Context, widgetId: Int, tab: WidgetTab) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit { putString("tab_$widgetId", tab.name) }
        }

        /**
         * Перерисовать все размещённые виджеты вместе с их списками.
         * Зовём только с фонового потока: updateAppWidget уходит в лаунчер
         * через binder, а тот на слабом устройстве отвечает не мгновенно —
         * с главного потока это оборачивалось ANR при работающем таймере.
         */
        @WorkerThread
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, SimplyWidget::class.java))
            if (ids.isEmpty()) return
            ids.forEach { manager.updateAppWidget(it, buildViews(context, it)) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        private fun buildViews(context: Context, widgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_today)
            val state = Repository.get(context).data.value
            val tab = tabOf(context, widgetId)

            // ---- шапка ----
            val openTasks = state.tasks.filter {
                !it.done && it.dueDate != null && !it.dueDate!!.isAfter(today())
            }
            val habitsDone = state.habits.count { it.isDoneOn(today()) }
            val focusMinutes = state.sessions.filter { it.date == today().iso() }.sumOf { it.minutes }
            val timer = PomodoroEngine.get(context).state.value

            when (tab) {
                WidgetTab.TASKS -> {
                    views.setTextViewText(R.id.widget_title, tr("Сегодня"))
                    views.setTextViewText(R.id.widget_subtitle, today().dayMonthLabel())
                    views.setTextViewText(R.id.widget_count, openTasks.size.toString())
                    views.setViewVisibility(
                        R.id.widget_count,
                        if (openTasks.isEmpty()) View.GONE else View.VISIBLE
                    )
                    views.setTextViewText(R.id.widget_empty, tr("На сегодня всё чисто"))
                }

                WidgetTab.HABITS -> {
                    views.setTextViewText(R.id.widget_title, tr("Привычки"))
                    views.setTextViewText(
                        R.id.widget_subtitle,
                        trf("%1\$d из %2\$d сегодня", habitsDone, state.habits.size)
                    )
                    views.setTextViewText(R.id.widget_count, habitsDone.toString())
                    views.setViewVisibility(
                        R.id.widget_count,
                        if (state.habits.isEmpty()) View.GONE else View.VISIBLE
                    )
                    views.setTextViewText(R.id.widget_empty, tr("Привычек пока нет"))
                }

                WidgetTab.FOCUS -> {
                    views.setTextViewText(R.id.widget_title, tr("Фокус"))
                    views.setTextViewText(
                        R.id.widget_subtitle,
                        if (timer.running) trf("%1\$s · %2\$s", tr(timer.phase.title), timer.timeLabel)
                        else trf("Сегодня %1\$d мин", focusMinutes)
                    )
                    views.setTextViewText(R.id.widget_count, timer.timeLabel)
                    views.setViewVisibility(R.id.widget_count, View.VISIBLE)
                    views.setTextViewText(R.id.widget_empty, tr("Записей пока нет"))
                }
            }

            // ---- вкладки ----
            val tabViews = listOf(
                WidgetTab.TASKS to R.id.widget_tab_tasks,
                WidgetTab.HABITS to R.id.widget_tab_habits,
                WidgetTab.FOCUS to R.id.widget_tab_focus
            )
            tabViews.forEach { (value, viewId) ->
                val selected = value == tab
                views.setInt(
                    viewId, "setBackgroundResource",
                    if (selected) R.drawable.widget_tab_on else R.drawable.widget_tab_off
                )
                views.setTextColor(
                    viewId,
                    context.getColor(if (selected) R.color.widget_accent else R.color.widget_muted)
                )
                views.setOnClickPendingIntent(
                    viewId,
                    tabIntent(context, widgetId, value)
                )
            }

            // ---- список ----
            val serviceIntent = Intent(context, SimplyWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(EXTRA_TAB, tab.name)
                // Без уникальных данных система переиспользует прежнюю фабрику
                // и вкладка не меняется.
                data = toUri(Intent.URI_INTENT_SCHEME).toUri()
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)
            views.setPendingIntentTemplate(R.id.widget_list, rowTemplate(context, widgetId))

            // ---- кнопки ----
            views.setOnClickPendingIntent(R.id.widget_header, openIntent(context, null))
            if (tab == WidgetTab.FOCUS) {
                views.setTextViewText(R.id.widget_add, if (timer.running) "❚❚" else "▶")
                views.setOnClickPendingIntent(R.id.widget_add, focusIntent(context, widgetId))
            } else {
                views.setTextViewText(R.id.widget_add, "+")
                views.setOnClickPendingIntent(
                    R.id.widget_add,
                    openIntent(context, MainActivity.ACTION_NEW_TASK)
                )
            }
            return views
        }

        private fun tabIntent(context: Context, widgetId: Int, tab: WidgetTab): PendingIntent {
            val intent = Intent(context, SimplyWidget::class.java).apply {
                action = ACTION_TAB
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(EXTRA_TAB, tab.name)
                data = "simply://tab/$widgetId/${tab.name}".toUri()
            }
            return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun focusIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, SimplyWidget::class.java).apply {
                action = ACTION_FOCUS_TOGGLE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = "simply://focus/$widgetId".toUri()
            }
            return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        /** Шаблон для строк списка: конкретное действие дописывает сама строка. */
        private fun rowTemplate(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, SimplyWidget::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = "simply://row/$widgetId".toUri()
            }
            // Заготовку под fill-in нельзя делать неизменяемой — иначе строка
            // не сможет дописать в неё свой id.
            return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun openIntent(context: Context, action: String?): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (action != null) intent.action = action
            return PendingIntent.getActivity(
                context,
                if (action == null) 0 else 1,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}

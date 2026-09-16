package com.simply.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.core.net.toUri
import com.simply.app.data.AppData
import com.simply.app.data.Habit
import com.simply.app.data.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Напоминания о задачах со сроком и ежедневные напоминания о привычках.
 * Будильники неточные (окно в пять минут) — так не нужны особые разрешения,
 * а для напоминания такой точности достаточно.
 *
 * Поставленные будильники запоминаются списком ключей в SharedPreferences:
 * без этого их нечем было бы снять, когда задача выполнена, удалена,
 * перенесена или напоминания выключены целиком.
 */
object ReminderScheduler {

    private const val WINDOW_MILLIS = 5 * 60_000L
    private const val HORIZON_DAYS = 14L
    private const val PREFS = "simply_reminders"
    private const val KEY_SCHEDULED = "scheduled_ids"

    fun reschedule(context: Context, data: AppData) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val known = prefs.getStringSet(KEY_SCHEDULED, emptySet())?.toSet().orEmpty()

        val scheduled = mutableSetOf<String>()
        val now = System.currentTimeMillis()

        if (data.settings.deadlineReminders) {
            val horizon = now + HORIZON_DAYS * 24 * 3600_000L
            data.tasks.forEach { task ->
                if (task.done || task.dueTime == null) return@forEach
                val at = triggerAt(task, data.settings.reminderOffsetMinutes) ?: return@forEach
                if (at < now || at > horizon) return@forEach
                if (setAlarm(manager, context, taskKey(task.id), at)) scheduled += taskKey(task.id)
            }
        }

        // Привычки напоминают о себе каждый день и не зависят от настройки дедлайнов.
        data.habits.forEach { habit ->
            val at = nextHabitTrigger(habit) ?: return@forEach
            if (setAlarm(manager, context, habitKey(habit.id), at)) scheduled += habitKey(habit.id)
        }

        // Всё, что было поставлено раньше и больше не нужно, снимаем поимённо.
        (known - scheduled).forEach { key ->
            runCatching { manager.cancel(pendingIntent(context, key)) }
        }
        prefs.edit { putStringSet(KEY_SCHEDULED, scheduled) }
    }

    private fun setAlarm(
        manager: AlarmManager,
        context: Context,
        key: String,
        at: Long
    ): Boolean = runCatching {
        manager.setWindow(
            AlarmManager.RTC_WAKEUP,
            at,
            WINDOW_MILLIS,
            pendingIntent(context, key)
        )
    }.isSuccess

    fun triggerAt(task: Task, offsetMinutes: Int): Long? {
        val deadline = task.deadline ?: return null
        return deadline
            .minusMinutes(offsetMinutes.toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /** Ближайшее срабатывание напоминания привычки: сегодня или завтра. */
    fun nextHabitTrigger(habit: Habit, from: LocalDateTime = LocalDateTime.now()): Long? {
        val time = habit.reminderLocalTime ?: return null
        var moment = LocalDateTime.of(from.toLocalDate(), time)
        if (!moment.isAfter(from)) moment = moment.plusDays(1)
        return moment.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun taskKey(id: String) = "t:$id"
    fun habitKey(id: String) = "h:$id"

    /**
     * У каждого напоминания свой intent: ключ уходит и в data, и в extra.
     * Так два разных id с одинаковым hashCode не затрут будильники друг друга —
     * PendingIntent сравнивается по данным, а не только по коду запроса.
     */
    private fun pendingIntent(context: Context, key: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .setData("simply://reminder/$key".toUri())
        val id = key.substringAfter(':')
        if (key.startsWith("h:")) intent.putExtra(EXTRA_HABIT_ID, id)
        else intent.putExtra(EXTRA_TASK_ID, id)
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    const val ACTION_REMIND = "com.simply.app.REMIND"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_HABIT_ID = "habit_id"
}

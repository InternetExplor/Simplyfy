package com.simply.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.simply.app.R
import com.simply.app.data.Repository
import com.simply.app.data.dueLabelForNotification
import com.simply.app.data.today
import com.simply.app.pomodoro.PomodoroNotifications
import com.simply.app.ui.i18n.trf

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMIND) return
        val data = Repository.get(context).data.value

        intent.getStringExtra(ReminderScheduler.EXTRA_HABIT_ID)?.let { habitId ->
            val habit = data.habits.firstOrNull { it.id == habitId } ?: return
            // Сначала переставляем будильник на завтра, потом решаем, показывать ли.
            ReminderScheduler.reschedule(context, data)
            if (habit.isDoneOn(today()) || habit.isSkippedOn(today())) return
            PomodoroNotifications.ensureChannels(context)
            val progress = habit.countOn(today())
            val notification = NotificationCompat.Builder(
                context, PomodoroNotifications.CHANNEL_REMINDERS
            )
                .setSmallIcon(R.drawable.ic_stat_simply)
                .setContentTitle(habit.name)
                .setContentText(
                    if (habit.perDay > 1) trf("%1\$d из %2\$d сегодня", progress, habit.perDay)
                    else trf("Отметь привычку: %1\$s", habit.name)
                )
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(PomodoroNotifications.openIntent(context))
                .build()
            PomodoroNotifications.post(context, ("h" + habit.id).hashCode(), notification)
            return
        }

        val id = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID) ?: return
        if (!data.settings.deadlineReminders) return
        val task = data.tasks.firstOrNull { it.id == id } ?: return
        if (task.done) return

        PomodoroNotifications.ensureChannels(context)
        if (!PomodoroNotifications.canPost(context)) return

        val project = data.projects.firstOrNull { it.id == task.projectId }?.name
        val notification = NotificationCompat.Builder(context, PomodoroNotifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_simply)
            .setContentTitle(task.title)
            .setContentText(
                listOfNotNull(task.dueLabelForNotification(), project).joinToString(" · ")
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(PomodoroNotifications.openIntent(context))
            .build()

        PomodoroNotifications.post(context, task.id.hashCode(), notification)
    }
}

package com.simply.app.pomodoro

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.simply.app.MainActivity
import com.simply.app.R

object PomodoroNotifications {

    const val CHANNEL_TIMER = "pomodoro_timer"
    const val CHANNEL_DONE = "pomodoro_done"
    const val CHANNEL_REMINDERS = "task_reminders"

    const val NOTIFICATION_TIMER = 1001
    private const val NOTIFICATION_DONE = 1002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TIMER,
                tr("Таймер помодоро"),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = tr("Отсчёт текущего отрезка")
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DONE,
                tr("Конец отрезка"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = tr("Сообщение о завершении работы или перерыва") }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                tr("Напоминания о задачах"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = tr("Задачи со временем «выполнить до»") }
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Единственная точка показа уведомлений: сначала проверяем разрешение,
     * потом отдаём в систему. Lint не видит проверку внутри canPost, поэтому
     * подавление стоит здесь — а не на каждом вызове по всему коду.
     */
    @SuppressLint("MissingPermission")
    fun post(context: Context, id: Int, notification: Notification) {
        if (!canPost(context)) return
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    fun openIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** Постоянное уведомление с отсчётом, пока таймер работает. */
    fun buildTimer(context: Context, state: PomodoroState, target: String?): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_simply)
            .setContentTitle("${state.phase.title} · ${state.timeLabel}")
            .setContentText(target ?: tr("Без задачи"))
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openIntent(context))
            .addAction(
                0,
                if (state.running) tr("Пауза") else tr("Продолжить"),
                servicePendingIntent(context, PomodoroService.ACTION_TOGGLE, 11)
            )
            .addAction(
                0,
                tr("Стоп"),
                servicePendingIntent(context, PomodoroService.ACTION_STOP_TIMER, 12)
            )
        return builder.build()
    }

    private fun servicePendingIntent(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            context,
            code,
            Intent(context, PomodoroService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    /**
     * Разовое уведомление о завершении отрезка. Если движок оставил вопрос
     * «продолжать ли» (pending), следующая фаза ещё не выбрана — не обещаем
     * перерыв, а зовём ответить.
     */
    fun notifyPhaseDone(
        context: Context,
        wasWork: Boolean,
        next: PomodoroPhase,
        pending: Boolean = false
    ) {
        if (!canPost(context)) return
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_simply)
            .setContentTitle(if (wasWork) tr("Отрезок завершён") else tr("Перерыв закончился"))
            .setContentText(
                when {
                    pending -> tr("Продолжить работу или на перерыв?")
                    wasWork -> trf("Дальше: %1\$s", tr(next.title).lowercase())
                    else -> tr("Можно снова браться за дело")
                }
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent(context))
            .build()
        post(context, NOTIFICATION_DONE, notification)
    }
}

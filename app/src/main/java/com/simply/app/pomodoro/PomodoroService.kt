package com.simply.app.pomodoro

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import com.simply.app.widget.SimplyWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Держит процесс живым, пока идёт отсчёт, и показывает уведомление с оставшимся временем.
 * Логика таймера остаётся в [PomodoroEngine] — сервис только отображает состояние.
 */
class PomodoroService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = PomodoroEngine.get(this)
        PomodoroNotifications.ensureChannels(this)

        when (intent?.action) {
            ACTION_TOGGLE -> engine.toggle()
            ACTION_STOP_TIMER -> {
                engine.stop()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        promote(engine)
        observe(engine)
        return START_STICKY
    }

    private fun promote(engine: PomodoroEngine) {
        val notification = PomodoroNotifications.buildTimer(
            this, engine.state.value, engine.targetTitle
        )
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(
                this, PomodoroNotifications.NOTIFICATION_TIMER, notification, type
            )
            started = true
        }
    }

    private fun observe(engine: PomodoroEngine) {
        if (watcher != null) return
        watcher = scope.launch {
            // Виджет обновляем редко: посекундная перерисовка ему не по карману,
            // а замерший на 25:00 отсчёт выглядит как сломанный.
            var lastWidgetTick = 0L
            engine.state.collectLatest { state ->
                if (!state.running) {
                    refreshWidget()
                    stopSelf()
                    return@collectLatest
                }
                val now = System.currentTimeMillis()
                if (now - lastWidgetTick >= WIDGET_TICK_MS) {
                    lastWidgetTick = now
                    refreshWidget()
                }
                if (!started) promote(engine) else {
                    PomodoroNotifications.post(
                        this@PomodoroService,
                        PomodoroNotifications.NOTIFICATION_TIMER,
                        PomodoroNotifications.buildTimer(
                            this@PomodoroService, state, engine.targetTitle
                        )
                    )
                }
            }
        }
    }

    /** Виджет живёт в лаунчере — трогаем его с фонового потока. */
    private fun refreshWidget() {
        scope.launch(Dispatchers.Default) { SimplyWidget.refresh(this@PomodoroService) }
    }

    override fun onDestroy() {
        watcher?.cancel()
        watcher = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        const val ACTION_SYNC = "com.simply.app.POMODORO_SYNC"
        const val ACTION_STOP = "com.simply.app.POMODORO_STOP"
        const val ACTION_TOGGLE = "com.simply.app.POMODORO_TOGGLE"
        const val ACTION_STOP_TIMER = "com.simply.app.POMODORO_STOP_TIMER"

        /** Как часто виджет переспрашивает время у работающего таймера. */
        private const val WIDGET_TICK_MS = 30_000L
    }
}

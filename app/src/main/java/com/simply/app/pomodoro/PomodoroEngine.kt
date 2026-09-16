package com.simply.app.pomodoro

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.simply.app.data.FocusSession
import com.simply.app.data.PomodoroSettings
import com.simply.app.data.Repository
import com.simply.app.data.iso
import com.simply.app.data.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PomodoroPhase(val title: String) {
    WORK("Фокус"),
    SHORT_BREAK("Перерыв"),
    LONG_BREAK("Длинный перерыв")
}

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.WORK,
    val running: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val completedInCycle: Int = 0,
    /** id задачи или привычки, над которой идёт работа. */
    val linkedId: String? = null,
    val linkedIsHabit: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else 1f - remainingSeconds.toFloat() / totalSeconds

    val timeLabel: String
        get() = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
}

/**
 * Что спросить после отрезка работы: «продолжаем эту цель или хватит».
 * Ответ выбирает следующую фазу: продолжаем — таймер снова в начале фокуса,
 * закончили — цель закрывается и человек уходит отдыхать. Живёт в движке,
 * а не в экране, потому что отрезок может закончиться, пока приложение
 * свёрнуто — вопрос дождётся возвращения.
 */
data class FocusPrompt(
    val targetId: String,
    val isHabit: Boolean,
    val title: String,
    val minutes: Int
)

/**
 * Вопрос по кнопке «Закончить»: человек уходит раньше времени, и только он
 * знает, засчитывать ли наработанное. Таймер к этому моменту уже на паузе.
 */
data class StopPrompt(
    val minutes: Int,
    val targetTitle: String?
)

/**
 * Таймер живёт в процессе, а не в экране: пока идёт отсчёт, его держит
 * сервис переднего плана, поэтому сворачивание приложения не сбивает время.
 */
class PomodoroEngine private constructor(private val app: Context) {

    private val repo = Repository.get(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(
        repo.data.value.pomodoro.workMinutes.let {
            PomodoroState(remainingSeconds = it * 60, totalSeconds = it * 60)
        }
    )
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    private val _prompt = MutableStateFlow<FocusPrompt?>(null)

    /** Незаданный вопрос об итоге отрезка. null — спрашивать нечего. */
    val prompt: StateFlow<FocusPrompt?> = _prompt.asStateFlow()

    private fun dismissPrompt() {
        _prompt.value = null
    }

    /**
     * «Продолжаю»: следующая фаза не меняется — таймер уже стоит в начале
     * фокуса, остаётся нажать «Старт». Перерыв не навязываем: человек сам
     * сказал, что не закончил.
     */
    fun continueAfterPrompt() {
        dismissPrompt()
    }

    /**
     * «Закончил»: цель к этому моменту уже закрыта вью-моделью, а между
     * задачами положено отдохнуть — уводим на перерыв. Отрезок посчитан
     * в [awaitAnswer], поэтому здесь его не считаем повторно.
     */
    fun breakAfterPrompt() {
        dismissPrompt()
        moveToNextPhase(countCompleted = false)
        syncService()
    }

    private val _stopPrompt = MutableStateFlow<StopPrompt?>(null)

    /** Незаданный вопрос «записать ли прошедшее». null — спрашивать нечего. */
    val stopPrompt: StateFlow<StopPrompt?> = _stopPrompt.asStateFlow()

    /** Название текущей цели — показывается в уведомлении. */
    val targetTitle: String?
        get() {
            val s = _state.value
            val id = s.linkedId ?: return null
            val d = repo.data.value
            return if (s.linkedIsHabit) d.habits.firstOrNull { it.id == id }?.name
            else d.tasks.firstOrNull { it.id == id }?.title
        }

    private var timerJob: Job? = null

    fun linkTarget(id: String?, isHabit: Boolean = false) {
        _state.value = _state.value.copy(linkedId = id, linkedIsHabit = id != null && isHabit)
        if (_state.value.running) syncService()
    }

    fun toggle() {
        if (_state.value.running) pause() else start()
    }

    fun start() {
        if (_state.value.running) return
        // Запустил отрезок, не ответив на вопрос (например, из уведомления) —
        // это и есть ответ «продолжаю».
        dismissPrompt()
        _state.value = _state.value.copy(running = true)
        syncService()
        timerJob?.cancel()
        timerJob = scope.launch {
            // Считаем по системным часам: задержки корутины не съедают время.
            var lastTick = System.currentTimeMillis()
            while (_state.value.running && _state.value.remainingSeconds > 0) {
                delay(200)
                val now = System.currentTimeMillis()
                val elapsed = ((now - lastTick) / 1000).toInt()
                if (elapsed >= 1) {
                    lastTick += elapsed * 1000L
                    val left = (_state.value.remainingSeconds - elapsed).coerceAtLeast(0)
                    _state.value = _state.value.copy(remainingSeconds = left)
                }
            }
            if (_state.value.remainingSeconds <= 0) finishPhase()
        }
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
        _state.value = _state.value.copy(running = false)
        syncService()
    }

    /**
     * Стоп — это конец сессии, а не отмена: помодоро не обязано длиться
     * ровно свои 25 минут. Отработанное засчитывается как полноценный отрезок,
     * а дальше человека спрашивают, продолжать ли цель: перерыв ставится
     * только по ответу «закончил». Выбросить отрезок целиком по-прежнему
     * можно кнопкой «Пропустить».
     */
    fun stop() {
        pause()
        val wasWork = _state.value.phase == PomodoroPhase.WORK
        val minutes = recordSession(completed = true)
        if (minutes < 1) {
            discardSegment()
            return
        }
        if (wasWork && askAboutTarget(minutes)) awaitAnswer()
        else moveToNextPhase(countCompleted = wasWork)
        syncService()
    }

    /**
     * Кнопка «Закончить» в приложении не решает за человека: таймер встаёт,
     * а дальше он сам говорит, засчитать ли пройденное время или уйти без
     * записи. Спрашивать есть о чём только на рабочем отрезке длиннее минуты —
     * перерыв и случайный «старт-стоп» просто возвращаются в начало.
     */
    fun requestStop() {
        pause()
        val state = _state.value
        val minutes = (state.totalSeconds - state.remainingSeconds) / 60
        if (state.phase != PomodoroPhase.WORK || minutes < 1) {
            discardSegment()
            return
        }
        _stopPrompt.value = StopPrompt(minutes = minutes, targetTitle = targetTitle)
    }

    /** Ответ на вопрос: записать наработанное или уйти, будто отрезка не было. */
    fun confirmStop(record: Boolean) {
        _stopPrompt.value = null
        if (record) stop() else discardSegment()
    }

    /** Передумал заканчивать — вопрос убираем, таймер остаётся на паузе. */
    fun dismissStopPrompt() {
        _stopPrompt.value = null
    }

    /**
     * Уйти без записи: отрезок возвращается в начало и статистика его не видит.
     * Так же поступаем с недоработанной минутой, иначе случайный «старт-стоп»
     * ломал бы цикл.
     */
    private fun discardSegment() {
        val total = durationFor(_state.value.phase, repo.data.value.pomodoro) * 60
        _state.value = _state.value.copy(remainingSeconds = total, totalSeconds = total)
        syncService()
    }

    /** Пропустить отрезок. Отработанные минуты всё равно попадут в статистику. */
    fun skip() {
        pause()
        recordSession(completed = false)
        moveToNextPhase(countCompleted = false)
        syncService()
    }

    fun applySettings(settings: PomodoroSettings) {
        if (!_state.value.running) {
            val total = durationFor(_state.value.phase, settings) * 60
            _state.value = _state.value.copy(remainingSeconds = total, totalSeconds = total)
        }
    }

    fun resetAll() {
        pause()
        _prompt.value = null
        _stopPrompt.value = null
        val total = repo.data.value.pomodoro.workMinutes * 60
        _state.value = PomodoroState(remainingSeconds = total, totalSeconds = total)
    }

    private fun finishPhase() {
        pause()
        vibrate()
        val wasWork = _state.value.phase == PomodoroPhase.WORK
        val asked = wasWork && askAboutTarget(recordSession(completed = true))
        if (asked) awaitAnswer() else moveToNextPhase(countCompleted = true)
        PomodoroNotifications.notifyPhaseDone(app, wasWork, _state.value.phase, pending = asked)
        syncService()
    }

    /**
     * Отрезок кончился, но следующую фазу выберет ответ на вопрос. Пока
     * таймер стоит в начале фокуса: «продолжаю» просто запустит его снова,
     * «закончил» уведёт на перерыв. Отработанный отрезок засчитываем сразу,
     * иначе длинный перерыв не придёт вовремя.
     */
    private fun awaitAnswer() {
        val state = _state.value
        val total = repo.data.value.pomodoro.workMinutes * 60
        _state.value = state.copy(
            phase = PomodoroPhase.WORK,
            running = false,
            remainingSeconds = total,
            totalSeconds = total,
            completedInCycle = state.completedInCycle + 1
        )
    }

    private fun moveToNextPhase(countCompleted: Boolean) {
        val settings = repo.data.value.pomodoro
        val state = _state.value
        val (nextPhase, nextCycle) = when (state.phase) {
            PomodoroPhase.WORK -> {
                val done = if (countCompleted) state.completedInCycle + 1 else state.completedInCycle
                if (done >= settings.cyclesBeforeLongBreak) PomodoroPhase.LONG_BREAK to 0
                else PomodoroPhase.SHORT_BREAK to done
            }

            else -> PomodoroPhase.WORK to state.completedInCycle
        }
        val total = durationFor(nextPhase, settings) * 60
        _state.value = state.copy(
            phase = nextPhase,
            running = false,
            remainingSeconds = total,
            totalSeconds = total,
            completedInCycle = nextCycle
        )
    }

    private fun durationFor(phase: PomodoroPhase, s: PomodoroSettings): Int = when (phase) {
        PomodoroPhase.WORK -> s.workMinutes
        PomodoroPhase.SHORT_BREAK -> s.shortBreakMinutes
        PomodoroPhase.LONG_BREAK -> s.longBreakMinutes
    }

    /**
     * Спрашиваем только когда было над чем работать: без привязанной цели
     * и после случайного касания «старт-стоп» вопрос был бы шумом. Возвращает
     * true, если вопрос задан — тогда следующую фазу выберет ответ.
     */
    private fun askAboutTarget(minutes: Int): Boolean {
        if (minutes < 1) return false
        val state = _state.value
        val id = state.linkedId ?: return false
        val data = repo.data.value
        val title = if (state.linkedIsHabit) data.habits.firstOrNull { it.id == id }?.name
        else data.tasks.firstOrNull { it.id == id }?.title
        _prompt.value = FocusPrompt(
            targetId = id,
            isHabit = state.linkedIsHabit,
            title = title ?: return false,
            minutes = minutes
        )
        return true
    }

    /**
     * Записывает отработанное время и возвращает засчитанные минуты.
     * Отрезки короче минуты не учитываем.
     */
    private fun recordSession(completed: Boolean): Int {
        val state = _state.value
        if (state.phase != PomodoroPhase.WORK) return 0
        val minutes = (state.totalSeconds - state.remainingSeconds) / 60
        if (minutes < 1) return 0

        val data = repo.data.value
        val task = if (!state.linkedIsHabit) data.tasks.firstOrNull { it.id == state.linkedId } else null
        val habit = if (state.linkedIsHabit) data.habits.firstOrNull { it.id == state.linkedId } else null

        val session = FocusSession(
            date = today().iso(),
            startedAt = System.currentTimeMillis() - minutes * 60_000L,
            minutes = minutes,
            completed = completed,
            targetId = state.linkedId,
            targetIsHabit = state.linkedIsHabit,
            targetTitle = task?.title ?: habit?.name ?: "Без задачи",
            projectId = task?.projectId
        )
        repo.update { it.copy(sessions = it.sessions + session) }
        return minutes
    }

    private fun vibrate() {
        if (!repo.data.value.pomodoro.vibrate) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        runCatching {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 220, 140, 220, 140, 380), -1)
            )
        }
    }

    /** Запускает или гасит сервис переднего плана под текущее состояние. */
    private fun syncService() {
        val wanted = _state.value.running && repo.data.value.settings.pomodoroNotification
        val intent = Intent(app, PomodoroService::class.java)
        runCatching {
            if (wanted) {
                intent.action = PomodoroService.ACTION_SYNC
                ContextCompat.startForegroundService(app, intent)
            } else {
                intent.action = PomodoroService.ACTION_STOP
                app.startService(intent)
            }
        }
    }

    companion object {
        // Синглтон держит applicationContext, а не активность — утечки нет.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: PomodoroEngine? = null

        fun get(context: Context): PomodoroEngine =
            instance ?: synchronized(this) {
                instance ?: PomodoroEngine(context.applicationContext).also { instance = it }
            }
    }
}

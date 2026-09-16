package com.simply.app.data

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/** Квадрант матрицы Эйзенхауэра. */
@Serializable
enum class Quadrant {
    /** Важно и срочно — делать сейчас. */
    DO,
    /** Важно, не срочно — запланировать. */
    PLAN,
    /** Срочно, не важно — делегировать/быстро закрыть. */
    DELEGATE,
    /** Ни то, ни другое — «когда-нибудь». */
    DROP;

    val isImportant: Boolean get() = this == DO || this == PLAN
    val isUrgent: Boolean get() = this == DO || this == DELEGATE

    companion object {
        fun of(important: Boolean, urgent: Boolean): Quadrant = when {
            important && urgent -> DO
            important && !urgent -> PLAN
            !important && urgent -> DELEGATE
            else -> DROP
        }
    }
}

/** Проект по умолчанию: сюда попадает всё, для чего проект не выбран. Его нельзя удалить. */
const val GENERAL_PROJECT_ID = "general"

/** Раздел внутри проекта: «Бэклог», «В работе», «Идеи» — как угодно. */
@Serializable
data class ProjectSection(
    val id: String = newId(),
    val name: String,
    val order: Int = 0
)

@Serializable
data class Project(
    val id: String = newId(),
    val name: String,
    val colorIndex: Int = 0,
    /** Ключ иконки из ProjectIcons. */
    val icon: String = "folder",
    /** Свободное описание проекта. */
    val note: String = "",
    /** Свои разделы внутри проекта. */
    val sections: List<ProjectSection> = emptyList(),
    /**
     * Проект доведён до конца и убран из работы. В файле поле по-прежнему
     * зовётся «archived» — так читаются данные, записанные до появления
     * завершения, и миграция не нужна.
     */
    @SerialName("archived")
    val completed: Boolean = false,
    /** Когда проект завершили. У старых записей неизвестно — null. */
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isGeneral: Boolean get() = id == GENERAL_PROJECT_ID

    /** Проект в работе: только такие показываются в списках и выборе проекта. */
    val isActive: Boolean get() = !completed

    companion object {
        fun general() = Project(
            id = GENERAL_PROJECT_ID,
            name = "Общий",
            colorIndex = 0,
            icon = "inbox",
            createdAt = 0L
        )
    }
}

/** Шаг внутри задачи: маленький чек-лист вместо дробления на отдельные задачи. */
@Serializable
data class Subtask(
    val id: String = newId(),
    val title: String,
    val done: Boolean = false
)

@Serializable
data class Task(
    val id: String = newId(),
    val title: String,
    val note: String = "",
    val done: Boolean = false,
    /** Проект задачи. Пустым не бывает: при загрузке всё приводится к «Общему». */
    val projectId: String? = null,
    /** Раздел внутри проекта, если он задан. */
    val sectionId: String? = null,
    /** ISO-дата дедлайна (yyyy-MM-dd) или null, если срока нет. */
    val due: String? = null,
    /** Время дедлайна «HH:mm» — только вместе с датой. null = весь день. */
    val dueTime: String? = null,
    /** Квадрант матрицы. null — задача ещё не разобрана и лежит во «Входящих». */
    val quadrant: Quadrant? = null,
    /** Шаблон, из которого создана задача — связывает одинаковые задачи между собой. */
    val templateId: String? = null,
    /** Шаги задачи. Пустой список — обычная задача без чек-листа. */
    val subtasks: List<Subtask> = emptyList(),
    /** Ручной порядок внутри дня: меньше — выше. */
    val sortIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    /** Когда задачу удалили. Заполнено только у того, что лежит в корзине. */
    val deletedAt: Long? = null
) {
    val doneSubtasks: Int get() = subtasks.count { it.done }

    val dueDate: LocalDate? get() = due?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    val dueLocalTime: LocalTime?
        get() = dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    /**
     * День, когда задачу закрыли. Им живёт выполненная задача без срока:
     * на временной линии она встаёт в тот день, когда её отметили.
     * У записей, сделанных до появления отметки, берём день создания.
     * Для незакрытой задачи значение смысла не имеет.
     */
    val completedDate: LocalDate
        get() = Instant.ofEpochMilli(completedAt ?: createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

    /**
     * День временной линии, в котором задаче место. Незакрытая задача живёт
     * своим сроком (без срока — нигде, у неё подвал сегодняшнего дня).
     * Выполненная остаётся в дне срока, даже если закрыли её позже,
     * а без срока встаёт в день, когда её отметили.
     */
    val timelineDate: LocalDate?
        get() = if (done) dueDate ?: completedDate else dueDate

    /** Момент, до которого задачу нужно выполнить. Без времени — конец дня. */
    val deadline: LocalDateTime?
        get() = dueDate?.atTime(dueLocalTime ?: LocalTime.MAX)

    fun isOverdue(now: LocalDateTime = LocalDateTime.now()): Boolean =
        !done && deadline?.isBefore(now) == true
}

/** Короткая подпись срока для уведомления: «Сегодня, до 18:00». */
fun Task.dueLabelForNotification(): String? {
    val date = dueDate ?: return null
    val time = dueLocalTime ?: return date.relativeLabel()
    return trf("%1\$s, до %2\$s", date.relativeLabel(), time.toString())
}

/** Как часто шаблон сам ставит задачу. */
@Serializable
enum class RepeatMode(val title: String) {
    NONE("Без повтора"),
    DAILY("Каждый день"),
    WEEKDAYS("По будням"),
    WEEKLY("По дням недели"),
    EVERY_N_DAYS("Каждые N дней"),
    MONTHLY("Раз в месяц")
}

/**
 * Заготовка задачи. Можно поставить вручную на нужный день,
 * а можно включить повтор — тогда задача появится сама.
 */
@Serializable
data class TaskTemplate(
    val id: String = newId(),
    val title: String,
    val note: String = "",
    val projectId: String = GENERAL_PROJECT_ID,
    /** Квадрант создаваемых задач. null — они попадут во «Входящие» матрицы. */
    val quadrant: Quadrant? = null,
    /** Время «выполнить до» для создаваемых задач, «HH:mm» или null. */
    val dueTime: String? = null,
    val repeat: RepeatMode = RepeatMode.NONE,
    /** Повтор включён. Выключенный шаблон остаётся, но задач не ставит. */
    val active: Boolean = true,
    /** Дни недели для WEEKLY: 1 = понедельник … 7 = воскресенье. */
    val weekDays: Set<Int> = emptySet(),
    val intervalDays: Int = 2,
    val monthDay: Int = 1,
    /** Ставить в последний день месяца вместо конкретного числа. */
    val monthLastDay: Boolean = false,
    /** Раздел проекта для создаваемых задач. */
    val sectionId: String? = null,
    /** Повтор действует не раньше этой даты. */
    val startDate: String? = null,
    /** Повтор заканчивается после этой даты. null — бессрочно. */
    val endDate: String? = null,
    /** На сколько дней вперёд создавать задачи. */
    val leadDays: Int = 0,
    /** Не ставить новую, пока предыдущая не выполнена. */
    val skipIfPending: Boolean = false,
    /** Точка отсчёта для «каждые N дней» и дата последней автопостановки. */
    val anchorDate: String? = null,
    val lastGenerated: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val start: LocalDate? get() = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val end: LocalDate? get() = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Ставит ли шаблон задачу в этот день. */
    fun matches(date: LocalDate): Boolean {
        if (!active || repeat == RepeatMode.NONE) return false
        start?.let { if (date.isBefore(it)) return false }
        end?.let { if (date.isAfter(it)) return false }
        return matchesRule(date)
    }

    private fun matchesRule(date: LocalDate): Boolean = when (repeat) {
        RepeatMode.NONE -> false
        RepeatMode.DAILY -> true
        RepeatMode.WEEKDAYS -> date.dayOfWeek.value <= 5
        RepeatMode.WEEKLY -> date.dayOfWeek.value in weekDays
        RepeatMode.EVERY_N_DAYS -> {
            val anchor = anchorDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val step = intervalDays.coerceAtLeast(1)
            anchor == null || !date.isBefore(anchor) &&
                ChronoUnit.DAYS.between(anchor, date) % step == 0L
        }
        // Если в месяце меньше дней, задача ставится в последний день месяца.
        RepeatMode.MONTHLY ->
            if (monthLastDay) date.dayOfMonth == date.lengthOfMonth()
            else date.dayOfMonth == monthDay.coerceAtMost(date.lengthOfMonth())
    }

    /** Ближайшие даты постановки — для предпросмотра в редакторе. */
    fun nextDates(from: LocalDate, count: Int = 5): List<LocalDate> {
        if (repeat == RepeatMode.NONE) return emptyList()
        val result = mutableListOf<LocalDate>()
        var day = from
        var guard = 0
        while (result.size < count && guard < 400) {
            if (matches(day)) result += day
            day = day.plusDays(1)
            guard++
        }
        return result
    }

    /** Человеческое описание расписания. */
    val scheduleLabel: String
        get() = when (repeat) {
            RepeatMode.NONE -> tr("Ставится вручную")
            RepeatMode.DAILY -> tr("Каждый день")
            RepeatMode.WEEKDAYS -> tr("По будням")
            RepeatMode.WEEKLY ->
                if (weekDays.isEmpty()) tr("По дням недели")
                else weekDays.sorted().joinToString(", ") { weekDayShortName(it) }
            RepeatMode.EVERY_N_DAYS -> trf("Каждые %1\$s", daysLabel(intervalDays))
            RepeatMode.MONTHLY ->
                if (monthLastDay) tr("В последний день месяца") else trf("%1\$d числа", monthDay)
        }
}

fun weekDayShortName(value: Int): String =
    DayOfWeek.of(value.coerceIn(1, 7)).getDisplayName(TextStyle.SHORT, RU).removeSuffix(".")

/** Потолок дневной цели привычки: сто отжиманий и двести страниц должны влезать. */
const val MAX_DAILY_TARGET = 999

@Serializable
data class Habit(
    val id: String = newId(),
    val name: String,
    /** Ключ иконки из HabitIcons. */
    val icon: String = "spark",
    /** Устаревшее поле старых версий данных — мигрирует в [icon]. */
    val emoji: String = "",
    val colorIndex: Int = 0,
    /** Сколько дней в неделю — цель. */
    val targetPerWeek: Int = 7,
    /** Сколько раз нужно отметить за день. 1 — обычная галочка. */
    val dailyTarget: Int = 1,
    /** Ежедневное напоминание «HH:mm» или null. */
    val reminderTime: String? = null,
    /** Дни, отмеченные как пропущенные: серию не рвут и в счёт не идут. */
    val skipped: Set<String> = emptySet(),
    /** Сколько раз отмечено в каждый день: yyyy-MM-dd -> количество. */
    val progress: Map<String, Int> = emptyMap(),
    /** Устаревшее поле старых версий данных — мигрирует в [progress]. */
    val doneDates: Set<String> = emptySet(),
    /** Проект, к которому относится привычка. null — привычка сама по себе. */
    val projectId: String? = null,
    /** Группа на экране привычек: «Утро», «Спорт». null — вне групп. */
    val groupId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Цель на день, приведённая к разумному значению. */
    val perDay: Int get() = dailyTarget.coerceIn(1, MAX_DAILY_TARGET)

    /** Сколько раз отмечено в этот день. */
    fun countOn(date: LocalDate): Int = progress[date.iso()] ?: 0

    /** Выполнена ли привычка в этот день целиком. */
    fun isDoneOn(date: LocalDate): Boolean = countOn(date) >= perDay

    /** День отмечен как пропущенный — болезнь, отпуск, выходной. */
    fun isSkippedOn(date: LocalDate): Boolean = date.iso() in skipped

    val reminderLocalTime: LocalTime?
        get() = reminderTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    /**
     * Ставит счётчик дня. Ноль убирает день из прогресса целиком, а любая
     * отметка снимает с него пропуск: день, в который привычку сделали,
     * пропущенным быть не может — иначе прочерк пропуска так и висел бы
     * поверх набранного числа.
     */
    fun withCount(date: LocalDate, value: Int): Habit {
        val key = date.iso()
        val next = value.coerceIn(0, MAX_DAILY_TARGET)
        return copy(
            progress = if (next <= 0) progress - key else progress + (key to next),
            skipped = if (next > 0) skipped - key else skipped
        )
    }
}

/**
 * Группа привычек — только способ сложить длинный список в стопки.
 * Своей цели и статистики у группы нет: она сворачивается и разворачивается,
 * а привычки внутри остаются самостоятельными.
 */
@Serializable
data class HabitGroup(
    val id: String = newId(),
    val name: String,
    val colorIndex: Int = 0,
    val order: Int = 0,
    /** Свёрнута ли группа. Хранится в данных, чтобы пережить перезапуск. */
    val collapsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class PomodoroSettings(
    val workMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val cyclesBeforeLongBreak: Int = 4,
    val vibrate: Boolean = true,
    val keepScreenOn: Boolean = true
)

/**
 * Один отрезок работы в помодоро. Название и проект сохраняются снимком,
 * чтобы статистика не терялась после удаления задачи.
 */
@Serializable
data class FocusSession(
    val id: String = newId(),
    /** yyyy-MM-dd */
    val date: String,
    val startedAt: Long,
    val minutes: Int,
    /** true — отрезок доработан до конца, false — прерван вручную. */
    val completed: Boolean = true,
    val targetId: String? = null,
    val targetIsHabit: Boolean = false,
    val targetTitle: String = "",
    val projectId: String? = null
)

@Serializable
enum class ThemeMode(val label: String) {
    SYSTEM("Как в системе"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

@Serializable
data class UiSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Индекс акцентной темы приложения (см. AccentTheme). */
    val accent: Int = 0,
    /** Брать цвета из обоев системы (Material You, Android 12+). */
    val dynamicColor: Boolean = false,
    val showCompleted: Boolean = true,
    /** Автоматически переносить просроченные задачи в «Сегодня». */
    val rollOverdue: Boolean = true,
    /** Показывать уведомление с таймером, пока идёт помодоро. */
    val pomodoroNotification: Boolean = true,
    /** Напоминать о задачах с временем «выполнить до». */
    val deadlineReminders: Boolean = false,
    /** За сколько минут до дедлайна напоминать. */
    val reminderOffsetMinutes: Int = 0,
    /** Раз в неделю сохранять копию данных в выбранную папку. */
    val autoBackup: Boolean = false,
    /** SAF-адрес папки для автокопии. */
    val backupFolder: String? = null,
    /** Когда автокопия сохранялась в последний раз. */
    val lastBackupAt: Long = 0L,
    /** Вводный гид уже показывали — сам больше не открывается. */
    val guideSeen: Boolean = false
)

@Serializable
data class AppData(
    val tasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitGroups: List<HabitGroup> = emptyList(),
    val templates: List<TaskTemplate> = emptyList(),
    val pomodoro: PomodoroSettings = PomodoroSettings(),
    val settings: UiSettings = UiSettings(),
    val sessions: List<FocusSession> = emptyList(),
    /** Удалённые задачи: хранятся 30 дней, потом исчезают сами. */
    val trash: List<Task> = emptyList(),
    val seeded: Boolean = false
)

/** Сколько дней держим удалённое. */
const val TRASH_DAYS = 30L

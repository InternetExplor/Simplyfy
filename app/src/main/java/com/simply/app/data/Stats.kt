package com.simply.app.data

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import com.simply.app.ui.i18n.isRussian
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

/** Итоги по одной цели фокуса — задаче или привычке. */
data class TargetStat(
    val id: String?,
    val title: String,
    val isHabit: Boolean,
    val projectId: String?,
    val minutes: Int,
    val sessions: Int,
    val completed: Int,
    val lastDate: LocalDate?
) {
    val averageMinutes: Int get() = if (sessions == 0) 0 else minutes / sessions
}

/** Сводка по проекту. */
data class ProjectStat(
    val project: Project?,
    val minutes: Int,
    val sessions: Int
)

/**
 * Разбор истории помодоро. Считается на лету из списка сессий —
 * отдельных счётчиков в данных нет, поэтому цифры не разъезжаются.
 */
class FocusStats(private val sessions: List<FocusSession>) {

    val totalMinutes: Int = sessions.sumOf { it.minutes }
    val totalSessions: Int = sessions.size
    val completedSessions: Int = sessions.count { it.completed }
    val interruptedSessions: Int = totalSessions - completedSessions

    val averageSessionMinutes: Int = if (totalSessions == 0) 0 else totalMinutes / totalSessions

    val longestSessionMinutes: Int = sessions.maxOfOrNull { it.minutes } ?: 0

    private val byDate: Map<String, Int> =
        sessions.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.minutes } }

    private val sessionsByDate: Map<String, Int> =
        sessions.groupBy { it.date }.mapValues { it.value.size }

    fun minutesOn(date: LocalDate): Int = byDate[date.iso()] ?: 0
    fun sessionsOn(date: LocalDate): Int = sessionsByDate[date.iso()] ?: 0

    fun minutesInRange(from: LocalDate, to: LocalDate): Int =
        generateSequence(from) { d -> d.plusDays(1).takeIf { !it.isAfter(to) } }
            .sumOf { minutesOn(it) }

    /** Минуты по дням за последние [days] суток, включая сегодня. */
    fun lastDays(days: Int, until: LocalDate = today()): List<Pair<LocalDate, Int>> =
        (days - 1 downTo 0).map { offset ->
            val date = until.minusDays(offset.toLong())
            date to minutesOn(date)
        }

    /** Цели, отсортированные по времени. */
    val byTarget: List<TargetStat> = sessions
        .groupBy { it.targetId to it.targetIsHabit }
        .map { (key, list) ->
            TargetStat(
                id = key.first,
                title = tr(list.last().targetTitle.ifBlank { "Без задачи" }),
                isHabit = key.second,
                projectId = list.lastOrNull { it.projectId != null }?.projectId,
                minutes = list.sumOf { it.minutes },
                sessions = list.size,
                completed = list.count { it.completed },
                lastDate = list.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.maxOrNull()
            )
        }
        .sortedByDescending { it.minutes }

    val tasks: List<TargetStat> get() = byTarget.filter { !it.isHabit && it.id != null }
    val habits: List<TargetStat> get() = byTarget.filter { it.isHabit }
    val untargeted: TargetStat? get() = byTarget.firstOrNull { it.id == null }

    /** Проекты считаем только по задачам — привычки к проектам не относятся. */
    fun byProject(projects: List<Project>): List<ProjectStat> = sessions
        .filterNot { it.targetIsHabit }
        .groupBy { it.projectId }
        .map { (id, list) ->
            ProjectStat(
                project = projects.firstOrNull { it.id == id },
                minutes = list.sumOf { it.minutes },
                sessions = list.size
            )
        }
        .sortedByDescending { it.minutes }

    /** День недели, в который в среднем работается больше всего. */
    val bestWeekday: DayOfWeek? = sessions
        .mapNotNull { s -> runCatching { LocalDate.parse(s.date) }.getOrNull()?.let { it to s.minutes } }
        .groupBy { it.first.dayOfWeek }
        .mapValues { (_, list) -> list.sumOf { it.second } }
        .maxByOrNull { it.value }
        ?.key

    /** Сколько всего было дней с фокусом. */
    val activeDays: Int = byDate.keys.size

    private val activeDateSet: Set<LocalDate> =
        byDate.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()

    /** Самая длинная серия дней подряд с фокусом. */
    val bestStreak: Int = run {
        if (activeDateSet.isEmpty()) return@run 0
        val sorted = activeDateSet.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            current = if (sorted[i - 1].plusDays(1) == sorted[i]) current + 1 else 1
            if (current > best) best = current
        }
        best
    }

    /** Текущая серия: считается от сегодня или от вчера, если сегодня ещё не работали. */
    val currentStreak: Int = run {
        var day = if (today() in activeDateSet) today() else today().minusDays(1)
        if (day !in activeDateSet) return@run 0
        var count = 0
        while (day in activeDateSet) {
            count++
            day = day.minusDays(1)
        }
        count
    }

    val busiestDay: Pair<LocalDate, Int>? = byDate
        .mapNotNull { (key, minutes) ->
            runCatching { LocalDate.parse(key) }.getOrNull()?.let { it to minutes }
        }
        .maxByOrNull { it.second }
}

/** «2 ч 15 мин», «45 мин», «—». */
fun formatMinutes(minutes: Int): String = when {
    minutes <= 0 -> trf("%1\$s мин", 0)
    minutes < 60 -> trf("%1\$s мин", minutes)
    minutes % 60 == 0 -> trf("%1\$s ч", minutes / 60)
    else -> trf("%1\$s ч %2\$s мин", minutes / 60, minutes % 60)
}

/** Компактно: «2:15» для плиток. */
fun formatMinutesShort(minutes: Int): String = formatMinutes(minutes)

fun DayOfWeek.fullName(): String = getDisplayName(TextStyle.FULL, RU).capitalizeFirst()

fun sessionsLabel(n: Int): String =
    if (isRussian) "$n ${plural(n, "сессия", "сессии", "сессий")}"
    else "$n ${if (n == 1) "session" else "sessions"}"

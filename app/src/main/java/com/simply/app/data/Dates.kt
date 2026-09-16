package com.simply.app.data

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.isRussian
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Форматирование дат следует языку системы или выбранному языку приложения. */
val RU: Locale get() = Locale.getDefault()

fun today(): LocalDate = LocalDate.now()

fun LocalDate.iso(): String = toString()

private val dayMonthFmt: DateTimeFormatter get() = DateTimeFormatter.ofPattern("d MMMM", RU)
private val dayMonthShortFmt: DateTimeFormatter get() = DateTimeFormatter.ofPattern("d MMM", RU)
private val monthYearFmt: DateTimeFormatter get() = DateTimeFormatter.ofPattern("LLLL yyyy", RU)

fun String.capitalizeFirst(): String =
    if (isEmpty()) this else substring(0, 1).uppercase(RU) + substring(1)

fun LocalDate.monthYearLabel(): String = format(monthYearFmt).capitalizeFirst()

fun LocalDate.dayMonthLabel(): String = format(dayMonthFmt)

/** «Сегодня» / «Завтра» / «Вчера» / «12 авг». */
fun LocalDate.relativeLabel(from: LocalDate = today()): String = when (this) {
    from -> tr("Сегодня")
    from.plusDays(1) -> tr("Завтра")
    from.minusDays(1) -> tr("Вчера")
    else -> format(dayMonthShortFmt).removeSuffix(".")
}

fun LocalDate.weekdayShort(): String =
    dayOfWeek.getDisplayName(TextStyle.SHORT, RU).removeSuffix(".").lowercase(RU)

/** Понедельник текущей недели. */
fun LocalDate.startOfWeek(): LocalDate =
    with(WeekFields.of(DayOfWeek.MONDAY, 4).dayOfWeek(), 1L)

fun LocalDate.isWeekend(): Boolean =
    dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

/** Число дней в подписи: «3 дня», «5 дней», «1 день». */
fun plural(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}

/** «пятница, 28 августа» — заголовок дня в списке задач. */
fun LocalDate.dayHeaderLabel(from: LocalDate = today()): String = when (this) {
    from -> tr("Сегодня")
    from.plusDays(1) -> tr("Завтра")
    from.minusDays(1) -> tr("Вчера")
    else -> {
        val weekday = dayOfWeek.getDisplayName(TextStyle.FULL, RU)
        "${dayMonthLabel()}, $weekday"
    }
}

fun daysLabel(n: Int): String =
    if (isRussian) "$n ${plural(n, "день", "дня", "дней")}"
    else "$n ${if (n == 1) "day" else "days"}"

fun tasksLabel(n: Int): String =
    if (isRussian) "$n ${plural(n, "задача", "задачи", "задач")}"
    else "$n ${if (n == 1) "task" else "tasks"}"

package com.simply.app.data

import java.time.LocalDate

/**
 * Разбор привычки: серии и итоги. Вынесено из UI, чтобы считать
 * одинаково в карточке, в напоминаниях и в тестах.
 */

/** Текущая серия. Пропущенный день серию не рвёт, но и не засчитывается. */
fun Habit.streak(from: LocalDate = today()): Int {
    var day = if (isDoneOn(from) || isSkippedOn(from)) from else from.minusDays(1)
    if (!isDoneOn(day) && !isSkippedOn(day)) return 0
    var count = 0
    while (isDoneOn(day) || isSkippedOn(day)) {
        if (isDoneOn(day)) count++
        day = day.minusDays(1)
    }
    return count
}

/** Лучшая серия за всю историю привычки. */
fun Habit.bestStreak(): Int {
    val days = progress.keys
        .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        .filter { isDoneOn(it) }
        .sorted()
    if (days.isEmpty()) return 0
    var best = 1
    var current = 1
    for (i in 1 until days.size) {
        var gapOk = days[i - 1].plusDays(1) == days[i]
        if (!gapOk) {
            // Разрыв допустим, если все дни между отметками помечены пропуском.
            var day = days[i - 1].plusDays(1)
            gapOk = true
            while (day.isBefore(days[i])) {
                if (!isSkippedOn(day)) {
                    gapOk = false
                    break
                }
                day = day.plusDays(1)
            }
        }
        current = if (gapOk) current + 1 else 1
        if (current > best) best = current
    }
    return best
}

/** Сколько дней выполнено за отрезок. */
fun Habit.doneInRange(from: LocalDate, to: LocalDate): Int {
    var day = from
    var count = 0
    while (!day.isAfter(to)) {
        if (isDoneOn(day)) count++
        day = day.plusDays(1)
    }
    return count
}

package com.simply.app.data

import java.time.LocalDate
import java.time.LocalTime

/** Что удалось вытащить из строки быстрого ввода. */
data class ParsedQuickTask(
    val title: String,
    val date: LocalDate?,
    val time: LocalTime?
)

/**
 * Разбор быстрого ввода: «завтра в 18:00 купить молоко» превращается
 * в задачу «Купить молоко» на завтра к 18:00. Разбираем только уверенные
 * случаи — если слово не распознано, оно остаётся частью названия.
 */
object QuickParse {

    private val weekDays = mapOf(
        "понедельник" to 1, "пн" to 1, "monday" to 1, "mon" to 1,
        "вторник" to 2, "вт" to 2, "tuesday" to 2, "tue" to 2,
        "среду" to 3, "среда" to 3, "ср" to 3, "wednesday" to 3, "wed" to 3,
        "четверг" to 4, "чт" to 4, "thursday" to 4, "thu" to 4,
        "пятницу" to 5, "пятница" to 5, "пт" to 5, "friday" to 5, "fri" to 5,
        "субботу" to 6, "суббота" to 6, "сб" to 6, "saturday" to 6, "sat" to 6,
        "воскресенье" to 7, "вс" to 7, "sunday" to 7, "sun" to 7
    )

    // \b в Java-регекспах считает буквами только латиницу, поэтому границы
    // слов у русских шаблонов задаём пробелами явно.
    //
    // Двоеточие внутри набора никогда не пишем первым: на Android регекспы
    // разбирает ICU, а там «[:» открывает свойство Unicode («[:alpha:]»).
    // Из-за «[:.]» весь класс падал в ExceptionInInitializerError, и любой
    // быстрый ввод задачи убивал приложение. На десктопном JVM это работало,
    // поэтому юнит-тесты ничего не замечали.
    private val timeRegex = Regex("""(?<![\d:])([01]?\d|2[0-3])[.:]([0-5]\d)(?![\d:])""")
    private val hourRegex = Regex("""(?:^|\s)в\s+([01]?\d|2[0-3])(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val inDaysRegex = Regex("""(?:^|\s)через\s+(\d{1,2})\s*(дн\S*|day\S*)""", RegexOption.IGNORE_CASE)
    private val dayMonthRegex = Regex("""(?<!\d)(\d{1,2})[./](\d{1,2})(?!\d)""")

    /** Исходники шаблонов — по ним тест следит, что «[:» не вернётся. */
    internal val patternSources: List<String>
        get() = listOf(timeRegex, hourRegex, inDaysRegex, dayMonthRegex).map { it.pattern }

    fun parse(raw: String, from: LocalDate = today()): ParsedQuickTask {
        var text = " ${raw.trim()} "
        var date: LocalDate? = null
        var time: LocalTime? = null

        fun cut(range: IntRange) {
            text = text.removeRange(range).replace("  ", " ")
        }

        // 1. Время: «18:30», «в 18»
        timeRegex.find(text)?.let { m ->
            val h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            time = LocalTime.of(h, min)
            cut(m.range)
        }
        if (time == null) {
            hourRegex.find(text)?.let { m ->
                time = LocalTime.of(m.groupValues[1].toInt(), 0)
                cut(m.range)
            }
        }

        // 2. Дата словами
        val lower = text.lowercase()
        val wordDates = listOf(
            "послезавтра" to 2L, "day after tomorrow" to 2L,
            "завтра" to 1L, "tomorrow" to 1L,
            "сегодня" to 0L, "today" to 0L
        )
        for ((word, plus) in wordDates) {
            val at = lower.indexOf(" $word ")
            if (at >= 0) {
                date = from.plusDays(plus)
                cut(at..(at + word.length))
                break
            }
        }

        // 3. «через N дней»
        if (date == null) {
            inDaysRegex.find(text)?.let { m ->
                date = from.plusDays(m.groupValues[1].toLong().coerceAtMost(365))
                cut(m.range)
            }
        }

        // 4. День недели: «в пятницу» — ближайшая будущая
        if (date == null) {
            val words = text.lowercase().split(' ', ',').filter { it.isNotBlank() }
            for (word in words) {
                val target = weekDays[word] ?: continue
                var day = from.plusDays(1)
                while (day.dayOfWeek.value != target) day = day.plusDays(1)
                date = day
                val at = text.lowercase().indexOf(word)
                if (at >= 0) cut(at until (at + word.length))
                break
            }
        }

        // 5. Дата числом: «14.08»
        if (date == null) {
            dayMonthRegex.find(text)?.let { m ->
                val day = m.groupValues[1].toInt()
                val month = m.groupValues[2].toInt()
                val parsed = runCatching { LocalDate.of(from.year, month, day) }.getOrNull()
                if (parsed != null) {
                    date = if (parsed.isBefore(from)) parsed.plusYears(1) else parsed
                    cut(m.range)
                }
            }
        }

        // Хвосты предлогов, оставшиеся от вырезанных кусков.
        var title = text.trim()
            .removePrefix("в ").removePrefix("к ").removePrefix("на ")
            .removeSuffix(" в").removeSuffix(" к").removeSuffix(" на")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
        if (title.isEmpty()) title = raw.trim()

        // Время без даты почти всегда значит «сегодня».
        if (time != null && date == null) date = from

        return ParsedQuickTask(
            title = title.replaceFirstChar { it.uppercase() },
            date = date,
            time = time
        )
    }
}

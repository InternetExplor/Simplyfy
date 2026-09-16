package com.simply.app.data

/** Насколько заранее напоминать о дедлайне. */
enum class ReminderOffset(val minutes: Int, val label: String) {
    AT_TIME(0, "В момент"),
    M15(15, "За 15 минут"),
    H1(60, "За час"),
    H3(180, "За 3 часа");

    companion object {
        fun of(minutes: Int): ReminderOffset =
            entries.firstOrNull { it.minutes == minutes } ?: AT_TIME
    }
}

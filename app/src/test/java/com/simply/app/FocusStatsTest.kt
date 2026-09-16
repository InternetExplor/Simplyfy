package com.simply.app

import com.simply.app.data.FocusSession
import com.simply.app.data.FocusStats
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FocusStatsTest {

    private val day = LocalDate.of(2026, 8, 24)

    private fun session(date: LocalDate, minutes: Int, completed: Boolean = true) =
        FocusSession(
            date = date.toString(),
            startedAt = 0L,
            minutes = minutes,
            completed = completed,
            targetTitle = "Работа"
        )

    @Test
    fun `итоги складываются`() {
        val stats = FocusStats(listOf(session(day, 25), session(day, 15), session(day.minusDays(1), 30)))
        assertEquals(70, stats.totalMinutes)
        assertEquals(3, stats.totalSessions)
        assertEquals(40, stats.minutesOn(day))
        assertEquals(2, stats.sessionsOn(day))
    }

    @Test
    fun `прерванные отделяются от доведённых`() {
        val stats = FocusStats(listOf(session(day, 25), session(day, 10, completed = false)))
        assertEquals(1, stats.completedSessions)
        assertEquals(1, stats.interruptedSessions)
    }

    @Test
    fun `серия дней с фокусом`() {
        val stats = FocusStats(
            listOf(session(day, 10), session(day.minusDays(1), 10), session(day.minusDays(3), 10))
        )
        assertEquals(2, stats.bestStreak)
        assertEquals(3, stats.activeDays)
    }

    @Test
    fun `диапазон и средние`() {
        val stats = FocusStats(listOf(session(day, 20), session(day.minusDays(2), 40)))
        assertEquals(60, stats.minutesInRange(day.minusDays(6), day))
        assertEquals(30, stats.averageSessionMinutes)
        assertEquals(40, stats.longestSessionMinutes)
    }

    @Test
    fun `пустая история не падает`() {
        val stats = FocusStats(emptyList())
        assertEquals(0, stats.totalMinutes)
        assertEquals(0, stats.averageSessionMinutes)
        assertEquals(0, stats.bestStreak)
    }
}

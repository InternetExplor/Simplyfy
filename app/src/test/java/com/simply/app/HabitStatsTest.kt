package com.simply.app

import com.simply.app.data.Habit
import com.simply.app.data.bestStreak
import com.simply.app.data.doneInRange
import com.simply.app.data.streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitStatsTest {

    private val day = LocalDate.of(2026, 8, 24)

    private fun habit(vararg done: LocalDate, target: Int = 1, skipped: Set<LocalDate> = emptySet()) =
        Habit(
            name = "Вода",
            dailyTarget = target,
            progress = done.associate { it.toString() to target },
            skipped = skipped.map { it.toString() }.toSet()
        )

    @Test
    fun `дневная цель считается по счётчику`() {
        val h = Habit(name = "Вода", dailyTarget = 8, progress = mapOf(day.toString() to 5))
        assertFalse(h.isDoneOn(day))
        assertEquals(5, h.countOn(day))
        val full = h.copy(progress = mapOf(day.toString() to 8))
        assertTrue(full.isDoneOn(day))
    }

    @Test
    fun `серия считает подряд идущие дни`() {
        val h = habit(day, day.minusDays(1), day.minusDays(2))
        assertEquals(3, h.streak(day))
    }

    @Test
    fun `пропуск не рвёт серию и не считается`() {
        val h = habit(day, day.minusDays(2), skipped = setOf(day.minusDays(1)))
        assertEquals(2, h.streak(day))
    }

    @Test
    fun `серия держится от вчера`() {
        val h = habit(day.minusDays(1), day.minusDays(2))
        assertEquals(2, h.streak(day))
    }

    @Test
    fun `лучшая серия ищется по всей истории`() {
        val h = habit(
            day, day.minusDays(1),
            day.minusDays(5), day.minusDays(6), day.minusDays(7), day.minusDays(8)
        )
        assertEquals(4, h.bestStreak())
    }

    @Test
    fun `выполнено за отрезок`() {
        val h = habit(day, day.minusDays(3))
        assertEquals(2, h.doneInRange(day.minusDays(6), day))
    }

    @Test
    fun `пустая привычка не даёт серии`() {
        assertEquals(0, habit().streak(day))
        assertEquals(0, habit().bestStreak())
    }
}

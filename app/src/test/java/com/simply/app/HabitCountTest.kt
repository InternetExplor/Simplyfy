package com.simply.app

import com.simply.app.data.Habit
import com.simply.app.data.MAX_DAILY_TARGET
import com.simply.app.data.iso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitCountTest {

    private val day: LocalDate = LocalDate.of(2026, 8, 31)

    private fun habit(vararg skipped: LocalDate) = Habit(
        name = "Вода",
        dailyTarget = 8,
        skipped = skipped.map { it.iso() }.toSet()
    )

    @Test
    fun `отметка снимает с дня пропуск`() {
        val marked = habit(day).withCount(day, 3)
        assertEquals(3, marked.countOn(day))
        // Раньше прочерк пропуска оставался и закрывал собой набранное число.
        assertFalse(marked.isSkippedOn(day))
    }

    @Test
    fun `ноль убирает день из прогресса, но пропуск не ставит`() {
        val cleared = habit().withCount(day, 5).withCount(day, 0)
        assertEquals(0, cleared.countOn(day))
        assertFalse(cleared.isSkippedOn(day))
        assertTrue(cleared.progress.isEmpty())
    }

    @Test
    fun `пропуск пустого дня остаётся на месте`() {
        val skipped = habit(day).withCount(day, 0)
        assertTrue(skipped.isSkippedOn(day))
    }

    @Test
    fun `счётчик не уходит за границы`() {
        assertEquals(0, habit().withCount(day, -4).countOn(day))
        assertEquals(MAX_DAILY_TARGET, habit().withCount(day, 5000).countOn(day))
    }

    @Test
    fun `соседние дни не задеваются`() {
        val h = habit(day, day.plusDays(1)).withCount(day, 2)
        assertTrue(h.isSkippedOn(day.plusDays(1)))
        assertEquals(0, h.countOn(day.plusDays(1)))
    }
}

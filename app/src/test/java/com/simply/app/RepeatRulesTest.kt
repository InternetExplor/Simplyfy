package com.simply.app

import com.simply.app.data.RepeatMode
import com.simply.app.data.TaskTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RepeatRulesTest {

    private val monday = LocalDate.of(2026, 8, 24)

    @Test
    fun `будни не включают выходные`() {
        val t = TaskTemplate(title = "Отчёт", repeat = RepeatMode.WEEKDAYS)
        assertTrue(t.matches(monday))
        assertFalse(t.matches(monday.plusDays(5))) // суббота
        assertFalse(t.matches(monday.plusDays(6))) // воскресенье
    }

    @Test
    fun `дни недели`() {
        val t = TaskTemplate(title = "Бассейн", repeat = RepeatMode.WEEKLY, weekDays = setOf(2, 4))
        assertFalse(t.matches(monday))
        assertTrue(t.matches(monday.plusDays(1)))
        assertTrue(t.matches(monday.plusDays(3)))
    }

    @Test
    fun `каждые N дней считаются от якоря`() {
        val t = TaskTemplate(
            title = "Полив",
            repeat = RepeatMode.EVERY_N_DAYS,
            intervalDays = 3,
            anchorDate = monday.toString()
        )
        assertTrue(t.matches(monday))
        assertFalse(t.matches(monday.plusDays(1)))
        assertTrue(t.matches(monday.plusDays(3)))
    }

    @Test
    fun `последний день месяца`() {
        val t = TaskTemplate(title = "Итоги", repeat = RepeatMode.MONTHLY, monthLastDay = true)
        assertTrue(t.matches(LocalDate.of(2026, 8, 31)))
        assertFalse(t.matches(LocalDate.of(2026, 8, 30)))
        assertTrue(t.matches(LocalDate.of(2026, 2, 28)))
    }

    @Test
    fun `период ограничивает повтор`() {
        val t = TaskTemplate(
            title = "Курс",
            repeat = RepeatMode.DAILY,
            startDate = monday.toString(),
            endDate = monday.plusDays(2).toString()
        )
        assertFalse(t.matches(monday.minusDays(1)))
        assertTrue(t.matches(monday.plusDays(2)))
        assertFalse(t.matches(monday.plusDays(3)))
    }

    @Test
    fun `предпросмотр дат`() {
        val t = TaskTemplate(title = "Зарядка", repeat = RepeatMode.DAILY)
        assertEquals(
            listOf(monday, monday.plusDays(1), monday.plusDays(2)),
            t.nextDates(monday, 3)
        )
    }

    @Test
    fun `выключенный повтор ничего не ставит`() {
        val t = TaskTemplate(title = "Пауза", repeat = RepeatMode.DAILY, active = false)
        assertFalse(t.matches(monday))
        assertTrue(t.nextDates(monday, 3).isEmpty())
    }
}

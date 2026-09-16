package com.simply.app

import com.simply.app.data.QuickParse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class QuickParseTest {

    private val monday = LocalDate.of(2026, 8, 24) // понедельник

    @Test
    fun `завтра с временем`() {
        val parsed = QuickParse.parse("завтра в 18:00 забрать посылку", monday)
        assertEquals("Забрать посылку", parsed.title)
        assertEquals(monday.plusDays(1), parsed.date)
        assertEquals(LocalTime.of(18, 0), parsed.time)
    }

    @Test
    fun `сегодня без времени`() {
        val parsed = QuickParse.parse("сегодня позвонить маме", monday)
        assertEquals("Позвонить маме", parsed.title)
        assertEquals(monday, parsed.date)
        assertNull(parsed.time)
    }

    @Test
    fun `время без даты значит сегодня`() {
        val parsed = QuickParse.parse("созвон 9:30", monday)
        assertEquals(monday, parsed.date)
        assertEquals(LocalTime.of(9, 30), parsed.time)
    }

    @Test
    fun `день недели уходит вперёд`() {
        val parsed = QuickParse.parse("в пятницу отчёт", monday)
        assertEquals(LocalDate.of(2026, 8, 28), parsed.date)
        assertEquals("Отчёт", parsed.title)
    }

    @Test
    fun `через сколько-то дней`() {
        val parsed = QuickParse.parse("через 3 дня оплатить счёт", monday)
        assertEquals(monday.plusDays(3), parsed.date)
    }

    @Test
    fun `без подсказок текст не портится`() {
        val parsed = QuickParse.parse("купить молоко", monday)
        assertEquals("Купить молоко", parsed.title)
        assertNull(parsed.date)
        assertNull(parsed.time)
    }

    /**
     * На Android регекспы разбирает ICU, где «[:» открывает свойство Unicode.
     * Шаблон «[:.]» валил инициализацию QuickParse и вместе с ней весь быстрый
     * ввод, а на десктопном JVM компилировался молча. Сторожим границу.
     */
    @Test
    fun `patterns keep colon out of the first place in a set`() {
        QuickParse.patternSources.forEach { pattern ->
            assertFalse("«[:» в шаблоне $pattern — ICU примет его за свойство Unicode",
                pattern.contains("[:"))
        }
    }
}

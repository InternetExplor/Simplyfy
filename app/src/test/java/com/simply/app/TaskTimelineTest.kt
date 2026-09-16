package com.simply.app

import com.simply.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** В каком дне задача показывается на временной линии. */
class TaskTimelineTest {

    private val yesterday = LocalDate.of(2026, 8, 25)
    private val today = LocalDate.of(2026, 8, 26)

    private fun millis(date: LocalDate): Long =
        date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `незакрытая задача живёт своим сроком`() {
        val task = Task(title = "Оплатить аренду", due = yesterday.toString())
        assertEquals(yesterday, task.timelineDate)
    }

    @Test
    fun `незакрытая задача без срока не принадлежит дню`() {
        val task = Task(title = "Разобрать архив")
        assertNull(task.timelineDate)
    }

    @Test
    fun `выполненная сегодня остаётся в дне своего срока`() {
        val task = Task(
            title = "Оплатить интернет",
            due = yesterday.toString(),
            done = true,
            completedAt = millis(today)
        )
        assertEquals(yesterday, task.timelineDate)
        assertEquals(today, task.completedDate)
    }

    @Test
    fun `выполненная без срока встаёт в день выполнения`() {
        val task = Task(title = "Починить кран", done = true, completedAt = millis(today))
        assertEquals(today, task.timelineDate)
    }

    @Test
    fun `у старой записи без отметки времени берём день создания`() {
        val task = Task(
            title = "Забрать посылку",
            done = true,
            completedAt = null,
            createdAt = millis(yesterday)
        )
        assertEquals(yesterday, task.timelineDate)
    }
}

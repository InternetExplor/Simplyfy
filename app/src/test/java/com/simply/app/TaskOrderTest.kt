package com.simply.app

import com.simply.app.data.AppData
import com.simply.app.data.Project
import com.simply.app.data.Quadrant
import com.simply.app.data.Task
import com.simply.app.data.activeTasks
import com.simply.app.data.inMatrixOrder
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskOrderTest {

    private fun task(
        title: String,
        quadrant: Quadrant? = null,
        due: String? = null,
        sortIndex: Int = 0,
        projectId: String = "p"
    ) = Task(
        title = title,
        quadrant = quadrant,
        due = due,
        sortIndex = sortIndex,
        projectId = projectId
    )

    @Test
    fun `квадранты идут срочно-важно, срочно, важно, не срочно, входящие`() {
        val tasks = listOf(
            task("входящие"),
            task("не срочно", Quadrant.DROP),
            task("важно", Quadrant.PLAN),
            task("срочно", Quadrant.DELEGATE),
            task("сейчас", Quadrant.DO)
        )
        assertEquals(
            listOf("сейчас", "срочно", "важно", "не срочно", "входящие"),
            tasks.inMatrixOrder().map { it.title }
        )
    }

    @Test
    fun `внутри квадранта раньше идёт ближний срок`() {
        val tasks = listOf(
            task("послезавтра", Quadrant.DO, due = "2026-09-02"),
            task("сегодня", Quadrant.DO, due = "2026-08-31")
        )
        assertEquals(
            listOf("сегодня", "послезавтра"),
            tasks.inMatrixOrder().map { it.title }
        )
    }

    @Test
    fun `задача без срока не обгоняет датированную из своего квадранта`() {
        val tasks = listOf(
            task("когда-нибудь", Quadrant.PLAN),
            task("в пятницу", Quadrant.PLAN, due = "2026-09-04")
        )
        assertEquals(
            listOf("в пятницу", "когда-нибудь"),
            tasks.inMatrixOrder().map { it.title }
        )
    }

    @Test
    fun `ручной порядок работает внутри квадранта, но не поверх него`() {
        val tasks = listOf(
            task("руками наверх", Quadrant.DROP, sortIndex = -5),
            task("второй срочный", Quadrant.DO, sortIndex = 2),
            task("первый срочный", Quadrant.DO, sortIndex = 1)
        )
        assertEquals(
            listOf("первый срочный", "второй срочный", "руками наверх"),
            tasks.inMatrixOrder().map { it.title }
        )
    }

    @Test
    fun `задачи завершённого проекта уходят из активных списков`() {
        val data = AppData(
            projects = listOf(
                Project(id = "live", name = "В работе"),
                Project(id = "done", name = "Закрыт", completed = true)
            ),
            tasks = listOf(
                task("остаётся", projectId = "live"),
                task("прячется", projectId = "done")
            )
        )
        assertEquals(listOf("остаётся"), data.activeTasks().map { it.title })
        // Из данных ничего не пропало — задача просто не показывается.
        assertEquals(2, data.tasks.size)
    }

    @Test
    fun `старое поле archived читается как завершение`() {
        val json = Json { ignoreUnknownKeys = true }
        val raw = """{"id":"x","name":"Ремонт","archived":true}"""
        val project = json.decodeFromString<Project>(raw)
        assertTrue(project.completed)
        assertFalse(project.isActive)
        // Записываем тем же именем, иначе старая версия приложения потеряет флаг.
        assertTrue(json.encodeToString(project).contains("\"archived\":true"))
    }
}

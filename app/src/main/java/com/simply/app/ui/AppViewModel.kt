package com.simply.app.ui

import com.simply.app.ui.i18n.tr
import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simply.app.data.AppData
import com.simply.app.data.FocusSession
import com.simply.app.data.GENERAL_PROJECT_ID
import com.simply.app.data.Habit
import com.simply.app.data.HabitGroup
import com.simply.app.data.MAX_DAILY_TARGET
import com.simply.app.data.PomodoroSettings
import com.simply.app.data.Project
import com.simply.app.data.ProjectSection
import com.simply.app.data.Quadrant
import com.simply.app.data.RepeatMode
import com.simply.app.ThemePrefs
import com.simply.app.pomodoro.FocusPrompt
import com.simply.app.pomodoro.PomodoroEngine
import com.simply.app.reminders.ReminderScheduler
import com.simply.app.pomodoro.PomodoroState
import com.simply.app.pomodoro.StopPrompt
import com.simply.app.data.TaskTemplate
import com.simply.app.data.Repository
import com.simply.app.widget.SimplyWidget
import com.simply.app.data.Task
import com.simply.app.data.UiSettings
import com.simply.app.ui.components.HabitIcons
import com.simply.app.data.iso
import com.simply.app.data.today
import com.simply.app.ui.i18n.trf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.simply.app.data.AutoBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.simply.app.data.QuickParse
import com.simply.app.data.TRASH_DAYS
import java.time.format.DateTimeFormatter
import java.time.LocalDate

private val hhmm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.get(app)
    val data: StateFlow<AppData> = repo.data

    private val engine = PomodoroEngine.get(app)
    val pomodoro: StateFlow<PomodoroState> = engine.state

    init {
        if (!repo.data.value.seeded) seedDemoContent()
        normalizeProjects()
        purgeOldTrash()
        generateRepeatingTasks()
        syncThemePref()

        // Виджет живёт вне приложения — перерисовываем его на изменения данных.
        repo.data
            .map { d -> d.tasks.filter { !it.done }.map { listOf(it.id, it.title, it.due) } }
            .distinctUntilChanged()
            .debounce(500)
            // Виджет рисуется через лаунчер: с главного потока такой binder
            // при живом таймере успевал упереться в ANR.
            .onEach { withContext(Dispatchers.Default) { SimplyWidget.refresh(app) } }
            .launchIn(viewModelScope)

        // Автокопия: если неделя прошла, тихо сохраняем файл в выбранную папку.
        viewModelScope.launch(Dispatchers.IO) {
            val settings = repo.data.value.settings
            if (!AutoBackup.isDue(settings)) return@launch
            val folder = settings.backupFolder ?: return@launch
            val at = AutoBackup.write(app, folder, repo.exportJson()) ?: return@launch
            repo.update { it.copy(settings = it.settings.copy(lastBackupAt = at)) }
        }

        // Будильники трогаем, только когда изменилось то, что на них влияет:
        // сами настройки напоминаний и сроки незакрытых задач. Иначе каждое
        // нажатие на чекбокс привычки перетряхивало бы весь список будильников.
        repo.data
            .map { d -> reminderKey(d) }
            .distinctUntilChanged()
            .debounce(400)
            .onEach { ReminderScheduler.reschedule(app, repo.data.value) }
            .launchIn(viewModelScope)
    }

    // ---------- Отмена последнего разрушительного действия ----------

    private var undoSnapshot: AppData? = null
    private val _undoMessage = MutableStateFlow<String?>(null)

    /** Подпись действия, которое можно отменить. null — отменять нечего. */
    val undoMessage: StateFlow<String?> = _undoMessage.asStateFlow()

    /**
     * Перед удалением запоминаем всё состояние целиком: данных мало,
     * зато отмена получается точной и не зависит от того, что именно удалили.
     */
    private fun rememberUndo(message: String) {
        undoSnapshot = repo.data.value
        _undoMessage.value = message
    }

    fun undo() {
        val snapshot = undoSnapshot ?: return
        repo.update { snapshot }
        undoSnapshot = null
        _undoMessage.value = null
    }

    /** Плашка отмены пропала — забываем снимок. */
    fun dismissUndo() {
        undoSnapshot = null
        _undoMessage.value = null
    }

    /** Слепок данных, от которых зависят будильники напоминаний. */
    private fun reminderKey(d: AppData): Any = listOf(
        d.settings.deadlineReminders,
        d.settings.reminderOffsetMinutes,
        d.tasks.filter { it.dueTime != null }.map { listOf(it.id, it.due, it.dueTime, it.done) },
        d.habits.filter { it.reminderTime != null }.map { listOf(it.id, it.reminderTime) }
    )

    /**
     * Гарантирует, что «Общий» проект существует и стоит первым,
     * а каждая задача принадлежит какому-то из существующих проектов.
     */
    private fun normalizeProjects() = repo.update { d ->
        val general = d.projects.firstOrNull { it.isGeneral } ?: Project.general()
        val projects = listOf(general) + d.projects.filterNot { it.isGeneral }
        val known = projects.mapTo(mutableSetOf()) { it.id }
        val sectionIds = projects.flatMap { p -> p.sections.map { it.id } }.toSet()
        val tasks = d.tasks.map { task ->
            val fixedProject =
                if (task.projectId != null && task.projectId in known) task
                else task.copy(projectId = GENERAL_PROJECT_ID, sectionId = null)
            if (fixedProject.sectionId == null || fixedProject.sectionId in sectionIds) fixedProject
            else fixedProject.copy(sectionId = null)
        }
        val habits = d.habits.map { habit ->
            var fixed = habit
            if (fixed.emoji.isNotBlank()) {
                fixed = fixed.copy(icon = HabitIcons.fromLegacyEmoji(fixed.emoji), emoji = "")
            }
            // Старые данные хранили просто набор дат — переводим их в счётчик,
            // где отмеченный день сразу равен дневной цели.
            if (fixed.doneDates.isNotEmpty()) {
                fixed = fixed.copy(
                    progress = fixed.doneDates.associateWith { fixed.perDay } + fixed.progress,
                    doneDates = emptySet()
                )
            }
            fixed
        }
        if (projects == d.projects && tasks == d.tasks && habits == d.habits) d
        else d.copy(projects = projects, tasks = tasks, habits = habits)
    }

    // ---------- Задачи ----------

    /** Добавляет уже собранную задачу (из редактора). */
    fun addTask(task: Task) = repo.update { d ->
        val clean = task.title.trim()
        if (clean.isEmpty()) d
        else d.copy(
            tasks = d.tasks + task.copy(
                title = clean,
                note = task.note.trim(),
                projectId = task.projectId ?: GENERAL_PROJECT_ID
            )
        )
    }

    fun addTask(
        title: String,
        projectId: String? = GENERAL_PROJECT_ID,
        due: LocalDate? = null,
        quadrant: Quadrant? = null,
        note: String = ""
    ) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        repo.update { d ->
            d.copy(tasks = d.tasks + Task(
                title = clean,
                note = note.trim(),
                projectId = projectId ?: GENERAL_PROJECT_ID,
                due = due?.iso(),
                quadrant = quadrant
            ))
        }
    }

    /**
     * Быстрый ввод: дата и время вынимаются прямо из текста
     * («завтра в 18:00 забрать посылку»). Возвращает день, на который встала задача.
     */
    fun addQuickTask(
        raw: String,
        projectId: String? = GENERAL_PROJECT_ID,
        sectionId: String? = null,
        quadrant: Quadrant? = null,
        defaultDate: LocalDate? = null
    ): LocalDate? {
        val parsed = QuickParse.parse(raw)
        if (parsed.title.isBlank()) return null
        val date = parsed.date ?: defaultDate
        repo.update { d ->
            d.copy(tasks = d.tasks + Task(
                title = parsed.title,
                projectId = projectId ?: GENERAL_PROJECT_ID,
                sectionId = sectionId,
                due = date?.iso(),
                dueTime = if (date == null) null else parsed.time?.format(hhmm),
                quadrant = quadrant
            ))
        }
        return date
    }

    /** Переключить шаг внутри задачи. */
    fun toggleSubtask(taskId: String, subtaskId: String) = repo.update { d ->
        d.copy(tasks = d.tasks.map { task ->
            if (task.id != taskId) task
            else task.copy(subtasks = task.subtasks.map {
                if (it.id == subtaskId) it.copy(done = !it.done) else it
            })
        })
    }

    fun toggleTask(id: String) = repo.update { d ->
        d.copy(tasks = d.tasks.map {
            if (it.id == id) it.copy(
                done = !it.done,
                completedAt = if (!it.done) System.currentTimeMillis() else null
            ) else it
        })
    }

    /** Ручной порядок: задачам присваиваются номера по позиции в списке. */
    fun reorderTasks(orderedIds: List<String>) = repo.update { d ->
        val positions = orderedIds.withIndex().associate { (i, id) -> id to i }
        d.copy(tasks = d.tasks.map { task ->
            positions[task.id]?.let { task.copy(sortIndex = it) } ?: task
        })
    }

    /** Перетаскивание задачи на другой день календаря. */
    fun moveTaskToDate(id: String, date: LocalDate) = repo.update { d ->
        d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(due = date.iso()) else it })
    }

    fun updateTask(task: Task) = repo.update { d ->
        val fixed = task.copy(projectId = task.projectId ?: GENERAL_PROJECT_ID)
        d.copy(tasks = d.tasks.map { if (it.id == fixed.id) fixed else it })
    }

    fun deleteTask(id: String) {
        rememberUndo(tr("Задача удалена"))
        repo.update { d ->
            val task = d.tasks.firstOrNull { it.id == id } ?: return@update d
            d.copy(
                tasks = d.tasks.filterNot { it.id == id },
                trash = d.trash + task.copy(deletedAt = System.currentTimeMillis())
            )
        }
    }

    // ---------- Корзина ----------

    /** Вернуть задачу из корзины. */
    fun restoreFromTrash(id: String) {
        repo.update { d ->
            val task = d.trash.firstOrNull { it.id == id } ?: return@update d
            d.copy(
                tasks = d.tasks + task.copy(deletedAt = null),
                trash = d.trash.filterNot { it.id == id }
            )
        }
        normalizeProjects()
    }

    /** Удалить одну задачу из корзины насовсем. */
    fun purgeFromTrash(id: String) = repo.update { d ->
        d.copy(trash = d.trash.filterNot { it.id == id })
    }

    fun clearTrash() {
        if (repo.data.value.trash.isEmpty()) return
        rememberUndo(tr("Корзина очищена"))
        repo.update { d -> d.copy(trash = emptyList()) }
    }

    /** Всё, что пролежало в корзине дольше срока, удаляется само. */
    private fun purgeOldTrash() = repo.update { d ->
        if (d.trash.isEmpty()) return@update d
        val edge = System.currentTimeMillis() - TRASH_DAYS * 24 * 3600_000L
        val kept = d.trash.filter { (it.deletedAt ?: 0L) > edge }
        if (kept.size == d.trash.size) d else d.copy(trash = kept)
    }

    fun clearCompleted(projectId: String? = null, onlyWithinProject: Boolean = false) =
        clearCompletedWhere { it.done && (!onlyWithinProject || it.projectId == projectId) }

    /**
     * Убирает ровно то выполненное, что человек видит на экране: блок «Выполнено»
     * теперь дневной, и кнопка под ним не должна трогать соседние дни.
     */
    fun clearShownCompleted(ids: List<String>) {
        if (ids.isEmpty()) return
        val shown = ids.toSet()
        clearCompletedWhere { it.done && it.id in shown }
    }

    private fun clearCompletedWhere(match: (Task) -> Boolean) {
        val removed = repo.data.value.tasks.count(match)
        if (removed == 0) return
        rememberUndo(trf("Удалено задач: %1\$d", removed))
        repo.update { d ->
            val (moved, kept) = d.tasks.partition(match)
            val now = System.currentTimeMillis()
            d.copy(
                tasks = kept,
                trash = d.trash + moved.map { it.copy(deletedAt = now) }
            )
        }
    }

    /**
     * Свайп влево: задача уезжает на следующий день от своего срока.
     * Просроченная и сегодняшняя — на завтра, будущая — ещё на день вперёд.
     */
    fun postponeTask(id: String) = repo.update { d ->
        d.copy(tasks = d.tasks.map { task ->
            if (task.id != id) task else {
                val from = task.dueDate?.takeIf { !it.isBefore(today()) } ?: today()
                task.copy(due = from.plusDays(1).iso())
            }
        })
    }

    /** Все просроченные незакрытые задачи переезжают на сегодня. */
    fun rollOverdueToToday() {
        val today = today()
        val count = repo.data.value.tasks.count {
            !it.done && it.dueDate?.isBefore(today) == true
        }
        if (count == 0) return
        rememberUndo(trf("Перенесено задач: %1\$d", count))
        repo.update { d ->
            d.copy(tasks = d.tasks.map {
                if (!it.done && it.dueDate?.isBefore(today) == true) it.copy(due = today.iso())
                else it
            })
        }
    }

    // ---------- Проекты ----------

    fun addProject(name: String, colorIndex: Int = 0, icon: String = "folder"): String? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val project = Project(name = clean, colorIndex = colorIndex, icon = icon)
        repo.update { d -> d.copy(projects = d.projects + project) }
        return project.id
    }

    /** Разделы внутри проекта. */
    fun addSection(projectId: String, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        repo.update { d ->
            d.copy(projects = d.projects.map { p ->
                if (p.id != projectId) p
                else p.copy(
                    sections = p.sections + ProjectSection(
                        name = clean,
                        order = p.sections.size
                    )
                )
            })
        }
    }

    fun renameSection(projectId: String, sectionId: String, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        repo.update { d ->
            d.copy(projects = d.projects.map { p ->
                if (p.id != projectId) p
                else p.copy(sections = p.sections.map {
                    if (it.id == sectionId) it.copy(name = clean) else it
                })
            })
        }
    }

    /** Раздел удаляется, его задачи остаются в проекте без раздела. */
    fun deleteSection(projectId: String, sectionId: String) = repo.update { d ->
        d.copy(
            projects = d.projects.map { p ->
                if (p.id != projectId) p
                else p.copy(sections = p.sections.filterNot { it.id == sectionId })
            },
            tasks = d.tasks.map {
                if (it.sectionId == sectionId) it.copy(sectionId = null) else it
            }
        )
    }

    fun moveTaskToSection(taskId: String, sectionId: String?) = repo.update { d ->
        d.copy(tasks = d.tasks.map { if (it.id == taskId) it.copy(sectionId = sectionId) else it })
    }

    /**
     * Завершение проекта: он уходит из работы вместе со своими задачами,
     * но ничего не теряет — вернули в работу, и всё на месте.
     * «Общий» завершить нельзя: в него складывается всё бесхозное.
     */
    fun setProjectCompleted(projectId: String, completed: Boolean) = repo.update { d ->
        d.copy(projects = d.projects.map {
            if (it.id != projectId || it.isGeneral) it
            else it.copy(
                completed = completed,
                completedAt = if (completed) System.currentTimeMillis() else null
            )
        })
    }

    fun updateProject(project: Project) = repo.update { d ->
        d.copy(projects = d.projects.map { if (it.id == project.id) project else it })
    }

    /** «Общий» удалить нельзя; задачи удалённого проекта переезжают в него. */
    fun deleteProject(id: String) {
        if (id == GENERAL_PROJECT_ID) return
        rememberUndo(tr("Проект удалён"))
        repo.update { d ->
            d.copy(
                projects = d.projects.filterNot { it.id == id },
                tasks = d.tasks.map {
                    if (it.projectId == id) it.copy(
                        projectId = GENERAL_PROJECT_ID,
                        sectionId = null
                    ) else it
                },
                templates = d.templates.map {
                    if (it.projectId == id) it.copy(projectId = GENERAL_PROJECT_ID) else it
                },
                // Привычка не переезжает в «Общий», а просто остаётся сама по себе:
                // она была привязана к теме, которой больше нет.
                habits = d.habits.map {
                    if (it.projectId == id) it.copy(projectId = null) else it
                }
            )
        }
    }

    // ---------- Шаблоны ----------

    fun addTemplate(template: TaskTemplate) {
        val clean = template.title.trim()
        if (clean.isEmpty()) return
        repo.update { d ->
            d.copy(
                templates = d.templates + template.copy(
                    title = clean,
                    note = template.note.trim(),
                    anchorDate = template.anchorDate ?: today().iso(),
                    lastGenerated = null
                )
            )
        }
        // Новый повтор должен сработать сразу, а не после перезапуска.
        generateRepeatingTasks()
    }

    fun updateTemplate(template: TaskTemplate) {
        repo.update { d ->
            d.copy(templates = d.templates.map { old ->
                if (old.id != template.id) old
                // Расписание поменяли — забываем отметку о постановке за сегодня,
                // иначе новое правило заработает только завтра.
                else if (old.scheduleKey() != template.scheduleKey())
                    template.copy(lastGenerated = null)
                else template
            })
        }
        generateRepeatingTasks()
    }

    /** Поля, от которых зависит, в какие дни шаблон ставит задачу. */
    private fun TaskTemplate.scheduleKey(): List<Any?> = listOf(
        repeat, active, weekDays, intervalDays, monthDay, monthLastDay,
        startDate, endDate, leadDays, skipIfPending, anchorDate, dueTime
    )

    /** Шаблон удаляется, уже созданные задачи остаются и просто теряют связь. */
    fun deleteTemplate(id: String) {
        rememberUndo(tr("Шаблон удалён"))
        repo.update { d ->
            d.copy(
                templates = d.templates.filterNot { it.id == id },
                tasks = d.tasks.map { if (it.templateId == id) it.copy(templateId = null) else it }
            )
        }
    }

    /** Ставит задачу по шаблону на выбранный день (null — без срока). */
    fun applyTemplate(templateId: String, date: LocalDate?) = repo.update { d ->
        val t = d.templates.firstOrNull { it.id == templateId } ?: return@update d
        d.copy(tasks = d.tasks + taskFrom(t, date))
    }

    /** Сохраняет существующую задачу как шаблон и связывает её с ним. */
    fun saveTaskAsTemplate(task: Task): String {
        val template = TaskTemplate(
            title = task.title.trim(),
            note = task.note.trim(),
            projectId = task.projectId ?: GENERAL_PROJECT_ID,
            quadrant = task.quadrant,
            dueTime = task.dueTime,
            anchorDate = today().iso()
        )
        repo.update { d ->
            d.copy(
                templates = d.templates + template,
                tasks = d.tasks.map {
                    if (it.id == task.id) it.copy(templateId = template.id) else it
                }
            )
        }
        return template.id
    }

    private fun taskFrom(t: TaskTemplate, date: LocalDate?) = Task(
        title = t.title,
        note = t.note,
        projectId = t.projectId,
        sectionId = t.sectionId,
        due = date?.iso(),
        dueTime = if (date == null) null else t.dueTime,
        quadrant = t.quadrant,
        templateId = t.id
    )

    /**
     * Автопостановка повторяющихся задач на сегодня.
     * Пропущенные дни не догоняются — иначе после долгого перерыва
     * список завалило бы просроченными копиями.
     */
    private fun generateRepeatingTasks() = repo.update { d ->
        val today = today()
        val key = today.iso()
        var tasks = d.tasks
        var changed = false

        val templates = d.templates.map { t ->
            if (t.repeat == RepeatMode.NONE || !t.active || t.lastGenerated == key) return@map t

            // Горизонт: сегодня плюс «заранее». Пропущенные дни не догоняем.
            var added = false
            for (offset in 0..t.leadDays.coerceIn(0, 30)) {
                val day = today.plusDays(offset.toLong())
                if (!t.matches(day)) continue
                val dayKey = day.iso()
                val exists = tasks.any { it.templateId == t.id && it.due == dayKey }
                if (exists) continue
                if (t.skipIfPending && tasks.any { it.templateId == t.id && !it.done }) continue
                tasks = tasks + taskFrom(t, day)
                added = true
            }
            // Отметку о постановке двигаем только вместе с реальной задачей:
            // иначе каждый запуск переписывал бы файл данных впустую.
            if (!added) t else {
                changed = true
                t.copy(lastGenerated = key)
            }
        }

        if (!changed) d else d.copy(tasks = tasks, templates = templates)
    }

    // ---------- Привычки ----------

    fun addHabit(
        name: String,
        icon: String,
        colorIndex: Int,
        targetPerWeek: Int,
        dailyTarget: Int = 1,
        reminderTime: String? = null,
        projectId: String? = null,
        groupId: String? = null
    ) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        repo.update { d ->
            d.copy(habits = d.habits + Habit(
                name = clean,
                icon = icon.ifBlank { "spark" },
                colorIndex = colorIndex,
                targetPerWeek = targetPerWeek.coerceIn(1, 7),
                dailyTarget = dailyTarget.coerceIn(1, MAX_DAILY_TARGET),
                reminderTime = reminderTime,
                projectId = projectId,
                groupId = groupId
            ))
        }
    }

    fun updateHabit(habit: Habit) = repo.update { d ->
        d.copy(habits = d.habits.map { if (it.id == habit.id) habit else it })
    }

    /**
     * Касание кружка дня: плюс одна отметка. Счётчик не упирается в дневную цель —
     * перевыполнение видно как «12 из 8», а не теряется. Сбрасывают долгими
     * нажатиями (по одной) — иначе случайный тап стирал бы весь набранный день.
     * У привычки с целью 1 это по-прежнему обычная галочка вкл/выкл.
     */
    fun toggleHabit(id: String, date: LocalDate) = stepHabit(id, date) { current, target ->
        if (target <= 1) (if (current >= 1) 0 else 1)
        else (current + 1).coerceAtMost(MAX_DAILY_TARGET)
    }

    /**
     * Ставит счётчик дня целиком — этим живёт колесо в карточке привычки,
     * которому незачем шагать по одному от текущего значения.
     */
    fun setHabitCount(id: String, date: LocalDate, value: Int) =
        stepHabit(id, date) { _, _ -> value }

    private fun stepHabit(id: String, date: LocalDate, next: (Int, Int) -> Int) = repo.update { d ->
        d.copy(habits = d.habits.map { h ->
            if (h.id != id) h
            else h.withCount(date, next(h.countOn(date), h.perDay))
        })
    }

    /** Пропуск дня: серия не рвётся, но день не засчитывается. */
    fun toggleSkip(id: String, date: LocalDate) = repo.update { d ->
        d.copy(habits = d.habits.map { h ->
            if (h.id != id) h else {
                val key = date.iso()
                if (key in h.skipped) h.copy(skipped = h.skipped - key)
                else h.copy(skipped = h.skipped + key, progress = h.progress - key)
            }
        })
    }

    /** Время ежедневного напоминания о привычке. null — не напоминать. */
    fun setHabitReminder(id: String, time: String?) = repo.update { d ->
        d.copy(habits = d.habits.map { if (it.id == id) it.copy(reminderTime = time) else it })
    }

    fun deleteHabit(id: String) {
        rememberUndo(tr("Привычка удалена"))
        repo.update { d -> d.copy(habits = d.habits.filterNot { it.id == id }) }
    }

    // ---------- Группы привычек ----------

    /** Заводит группу и возвращает её id — редактор сразу кладёт в неё привычку. */
    fun addHabitGroup(name: String, colorIndex: Int = 0): String? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val group = HabitGroup(
            name = clean,
            colorIndex = colorIndex,
            order = data.value.habitGroups.size
        )
        repo.update { d -> d.copy(habitGroups = d.habitGroups + group) }
        return group.id
    }

    fun renameHabitGroup(id: String, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        repo.update { d ->
            d.copy(habitGroups = d.habitGroups.map {
                if (it.id == id) it.copy(name = clean) else it
            })
        }
    }

    /** Свернуть или развернуть группу. Состояние живёт в данных, а не в экране. */
    fun toggleHabitGroup(id: String) = repo.update { d ->
        d.copy(habitGroups = d.habitGroups.map {
            if (it.id == id) it.copy(collapsed = !it.collapsed) else it
        })
    }

    /** Группу убираем, привычки остаются — просто выходят из группы. */
    fun deleteHabitGroup(id: String) {
        rememberUndo(tr("Группа удалена"))
        repo.update { d ->
            d.copy(
                habitGroups = d.habitGroups.filterNot { it.id == id },
                habits = d.habits.map { if (it.groupId == id) it.copy(groupId = null) else it }
            )
        }
    }

    // ---------- Настройки ----------

    fun updateSettings(settings: UiSettings) {
        repo.update { it.copy(settings = settings) }
        syncThemePref(settings)
    }

    /** Гид больше не открывается сам — только кнопкой в настройках. */
    fun markGuideSeen() {
        if (repo.data.value.settings.guideSeen) return
        repo.update { it.copy(settings = it.settings.copy(guideSeen = true)) }
    }

    /** Дублируем выбор темы в SharedPreferences: MainActivity читает его до Compose. */
    private fun syncThemePref(settings: UiSettings = repo.data.value.settings) {
        getApplication<Application>()
            .getSharedPreferences(ThemePrefs.NAME, android.content.Context.MODE_PRIVATE)
            .edit { putString(ThemePrefs.KEY_MODE, settings.themeMode.name) }
    }

    /** Резервная копия всех данных одним JSON. */
    fun exportJson(): String = repo.exportJson()

    /** Восстановление из копии: данные заменяются целиком. */
    fun importJson(raw: String): Boolean {
        if (!repo.importJson(raw)) return false
        normalizeProjects()
        generateRepeatingTasks()
        return true
    }

    /** Полный сброс: данные удаляются, демо-контент не возвращается. */
    fun resetEverything() {
        repo.update { AppData(seeded = true, settings = it.settings) }
        engine.resetAll()
        normalizeProjects()
    }

    // ---------- Помодоро ----------

    fun updatePomodoroSettings(settings: PomodoroSettings) {
        repo.update { it.copy(pomodoro = settings) }
        engine.applySettings(settings)
    }

    /**
     * Запись сессии фокуса — и новая «задним числом», и правка существующей.
     * Название цели и проект держим снимком, как и движок: удалили задачу —
     * история всё равно читается.
     */
    fun saveSession(
        id: String?,
        minutes: Int,
        date: LocalDate,
        targetId: String? = null,
        targetIsHabit: Boolean = false
    ) {
        if (minutes <= 0) return
        repo.update { d ->
            val task = if (!targetIsHabit) d.tasks.firstOrNull { it.id == targetId } else null
            val habit = if (targetIsHabit) d.habits.firstOrNull { it.id == targetId } else null
            val old = id?.let { key -> d.sessions.firstOrNull { it.id == key } }
            val built = FocusSession(
                id = id ?: com.simply.app.data.newId(),
                date = date.iso(),
                // День не меняли — оставляем настоящее время начала,
                // иначе история сползла бы на полдень при любой правке минут.
                startedAt = if (old != null && old.date == date.iso()) old.startedAt
                else date.atTime(12, 0)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                minutes = minutes.coerceIn(1, 24 * 60),
                completed = old?.completed ?: true,
                targetId = targetId,
                targetIsHabit = targetIsHabit,
                targetTitle = task?.title ?: habit?.name ?: tr("Без задачи"),
                projectId = task?.projectId
            )
            if (old == null) d.copy(sessions = d.sessions + built)
            else d.copy(sessions = d.sessions.map { if (it.id == id) built else it })
        }
    }

    fun deleteSession(id: String) {
        rememberUndo(tr("Запись удалена"))
        repo.update { d -> d.copy(sessions = d.sessions.filterNot { it.id == id }) }
    }

    fun linkTarget(id: String?, isHabit: Boolean = false) = engine.linkTarget(id, isHabit)

    fun toggleTimer() = engine.toggle()

    /** Стоп — конец сессии: отработанное засчитывается, дальше перерыв. */
    /** Кнопка «Закончить»: сначала спрашиваем, записывать ли наработанное. */
    fun stopTimer() = engine.requestStop()

    /** Незаданный вопрос по кнопке «Закончить». */
    val stopPrompt: StateFlow<StopPrompt?> = engine.stopPrompt

    fun confirmStop(record: Boolean) = engine.confirmStop(record)

    fun dismissStopPrompt() = engine.dismissStopPrompt()

    fun skipPhase() = engine.skip()

    /** Вопрос «продолжаем ли», который движок оставил после отрезка работы. */
    val focusPrompt: StateFlow<FocusPrompt?> = engine.prompt

    /**
     * «Продолжаю»: цель остаётся привязанной, таймер уже стоит в начале
     * фокуса — перерыв не навязываем, работа ещё не закончена.
     */
    fun continueFocusTarget() = engine.continueAfterPrompt()

    /**
     * «Закончил»: задачу закрываем, привычке добавляем одну отметку — и то
     * и другое только вперёд: снимать уже отмеченное ответом на вопрос было
     * бы сюрпризом. Закрытую задачу отвязываем, чтобы следующий отрезок не
     * целился в неё же, и уводим человека на перерыв — между задачами надо
     * отдохнуть.
     */
    fun completeFocusTarget(prompt: FocusPrompt) {
        if (prompt.isHabit) {
            stepHabit(prompt.targetId, today()) { current, _ ->
                (current + 1).coerceAtMost(MAX_DAILY_TARGET)
            }
        } else {
            repo.update { d ->
                d.copy(tasks = d.tasks.map {
                    if (it.id == prompt.targetId && !it.done)
                        it.copy(done = true, completedAt = System.currentTimeMillis())
                    else it
                })
            }
            engine.linkTarget(null)
        }
        engine.breakAfterPrompt()
    }

    fun flush() = viewModelScope.launch { repo.flush() }

    // ---------- Демо-контент при первом запуске ----------

    private fun seedDemoContent() {
        val general = Project.general()
        val personal = Project(name = tr("Личное"), colorIndex = 3, icon = "home")
        val work = Project(name = tr("Работа"), colorIndex = 4, icon = "work")
        val t = today()
        repo.update { d ->
            d.copy(
                seeded = true,
                projects = listOf(general, personal, work),
                tasks = listOf(
                    Task(title = tr("Разобрать входящие"), projectId = work.id, due = t.iso(), quadrant = Quadrant.DO),
                    Task(title = tr("Прогулка 30 минут"), projectId = personal.id, due = t.iso(), quadrant = Quadrant.PLAN),
                    Task(title = tr("Спланировать неделю"), projectId = work.id, due = t.plusDays(1).iso(), quadrant = Quadrant.PLAN),
                    Task(title = tr("Оплатить счета"), projectId = personal.id, due = t.plusDays(2).iso(), quadrant = Quadrant.DELEGATE),
                    // Без квадранта — сразу видно, как работают «Входящие» матрицы.
                    Task(title = tr("Записаться к врачу"), projectId = general.id)
                ),
                habits = listOf(
                    Habit(
                        name = tr("Вода"),
                        icon = "water",
                        colorIndex = 4,
                        targetPerWeek = 7,
                        dailyTarget = 8
                    ),
                    Habit(name = tr("Чтение"), icon = "book", colorIndex = 5, targetPerWeek = 5),
                    Habit(name = tr("Зарядка"), icon = "run", colorIndex = 1, targetPerWeek = 4)
                )
            )
        }
    }
}

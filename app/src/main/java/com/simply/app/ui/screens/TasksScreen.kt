package com.simply.app.ui.screens

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.ChecklistRtl
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.simply.app.data.AppData
import com.simply.app.data.Quadrant
import com.simply.app.data.Task
import com.simply.app.data.dayHeaderLabel
import com.simply.app.data.GENERAL_PROJECT_ID
import com.simply.app.data.startOfWeek
import com.simply.app.data.activeTasks
import com.simply.app.data.inMatrixOrder
import com.simply.app.data.tasksLabel
import com.simply.app.data.today
import com.simply.app.data.weekdayShort
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.ScreenHeader
import com.simply.app.ui.components.QuickAddBar
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.contrastOn
import com.simply.app.ui.components.quickAddBottomPadding
import com.simply.app.ui.theme.Accents
import com.simply.app.ui.theme.Motion

/**
 * Главный экран — один день. Неделя сверху, свайп вбок листает даты.
 * Отдельных режимов «все дни» и «просрочено» нет: просроченное поднимается
 * плашкой в сегодняшнем дне, а задачи без срока лежат в его же подвале.
 */
@Composable
fun TasksScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    addRequested: Boolean = false,
    onAddHandled: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenSearch: () -> Unit
) {
    var selectedDay by remember { mutableStateOf(today()) }
    // Направление листания: по нему выбираем, с какой стороны уезжает старый день.
    var forward by remember { mutableStateOf(true) }
    var projectFilter by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun goToDay(date: LocalDate) {
        forward = date.isAfter(selectedDay)
        selectedDay = date
    }

    // Ярлык «Новая задача» открывает редактор сразу при запуске.
    LaunchedEffect(addRequested) {
        if (addRequested) {
            creating = true
            onAddHandled()
        }
    }

    val rollOverdue = data.settings.rollOverdue
    val isToday = selectedDay == today()

    // Фильтр по проекту общий для всех блоков дня.
    // Завершённый проект уходит и из фильтра: фильтровать по нему нечего.
    val activeProjects = remember(data.projects) { data.projects.filter { it.isActive } }

    // Проект завершили, пока он стоял в фильтре — возвращаемся ко всем задачам,
    // иначе экран остался бы пустым без всякого объяснения.
    LaunchedEffect(activeProjects, projectFilter) {
        if (projectFilter != null && activeProjects.none { it.id == projectFilter }) {
            projectFilter = null
        }
    }

    val scoped = remember(data.tasks, data.projects, projectFilter) {
        data.activeTasks().filter { projectFilter == null || it.projectId == projectFilter }
    }

    // Незакрытые задачи дня: со сроком на этот день, плюс просроченное,
    // если ему разрешено всплывать в сегодня — иначе оно исчезает из виду
    // вместе со своей датой.
    val activeDay = remember(scoped, selectedDay, rollOverdue) {
        scoped
            .filter { task ->
                if (task.done) return@filter false
                val d = task.dueDate
                d == selectedDay ||
                    (rollOverdue && selectedDay == today() && d != null && d.isBefore(today()))
            }
            .inMatrixOrder()
    }
    // «Без срока» живёт в подвале сегодняшнего дня и больше нигде.
    val activeNoDate = remember(scoped, isToday) {
        if (!isToday) emptyList()
        else scoped.filter { !it.done && it.dueDate == null }.inMatrixOrder()
    }
    // Выполненное показывается ровно в одном дне и никуда не переезжает:
    // задача со сроком остаётся в дне срока, даже если закрыли её позже,
    // а задача без срока встаёт в тот день, когда её отметили.
    val done = remember(scoped, selectedDay, data.settings.showCompleted) {
        if (!data.settings.showCompleted) emptyList()
        else scoped
            .filter { it.done && it.timelineDate == selectedDay }
            .inMatrixOrder()
    }

    val todayCount = scoped.count { !it.done && it.dueDate == today() }
    val overdueCount = scoped.count { !it.done && it.dueDate?.isBefore(today()) == true }
    val showOverdueBanner = overdueCount > 0 && isToday

    // Неделя выбранного дня и сколько незакрытых задач в каждом её дне:
    // выполненное из значка уходит, иначе он никогда не гаснет.
    val week = remember(selectedDay) {
        val start = selectedDay.startOfWeek()
        (0..6).map { start.plusDays(it.toLong()) }
    }
    val weekCounts = remember(scoped, week) {
        week.associateWith { date -> scoped.count { !it.done && it.dueDate == date } }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                // Свайп по свободному месту листает дни: вправо — назад, влево — вперёд.
                // Строки задач ловят жест раньше и оставляют себе свои «выполнить»
                // и «на завтра», поэтому листается только там, где строк нет.
                .pointerInput(Unit) {
                    val threshold = size.width * 0.18f
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onHorizontalDrag = { change, amount ->
                            total += amount
                            change.consume()
                        },
                        onDragEnd = {
                            when {
                                total > threshold -> goToDay(selectedDay.minusDays(1))
                                total < -threshold -> goToDay(selectedDay.plusDays(1))
                            }
                        }
                    )
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = 16.dp
                )
            ) {
                item {
                    ScreenHeader(
                        title = tr("Задачи"),
                        subtitle = if (todayCount > 0) trf("На сегодня %1\$s", tasksLabel(todayCount))
                        else tr("На сегодня всё чисто"),
                        trailing = {
                            SoftIconButton(Icons.Rounded.Search, tr("Поиск"), onOpenSearch)
                            SoftIconButton(Icons.Rounded.Bookmarks, tr("Шаблоны"), onOpenTemplates)
                            SoftIconButton(Icons.Rounded.FolderOpen, tr("Проекты"), onOpenProjects)
                            SoftIconButton(Icons.Rounded.Settings, tr("Настройки"), onOpenSettings)
                        }
                    )
                }

                item {
                    WeekStrip(
                        week = week,
                        selected = selectedDay,
                        counts = weekCounts,
                        onPick = { goToDay(it) }
                    )
                }

                if (activeProjects.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterPill(
                                    tr("Все проекты"),
                                    projectFilter == null,
                                    { projectFilter = null },
                                    tonal = true
                                )
                            }
                            items(activeProjects, key = { it.id }) { p ->
                                val count = data.tasks.count { it.projectId == p.id && !it.done }
                                FilterPill(
                                    tr(p.name),
                                    projectFilter == p.id,
                                    { projectFilter = if (projectFilter == p.id) null else p.id },
                                    count = count,
                                    accent = Accents.color(p.colorIndex),
                                    tonal = true
                                )
                            }
                        }
                    }
                }

                // Просроченное копится молча — предлагаем перенести всё разом.
                if (showOverdueBanner) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OverdueBanner(
                                count = overdueCount,
                                onRoll = { vm.rollOverdueToToday() }
                            )
                        }
                    }
                }

                // День листается как страница: содержимое уезжает в сторону свайпа.
                item(key = "day") {
                    AnimatedContent(
                        targetState = selectedDay,
                        transitionSpec = {
                            val shift = if (forward) 1 else -1
                            (slideInHorizontally(Motion.enter()) { it / 6 * shift } +
                                fadeIn(Motion.enter()))
                                .togetherWith(
                                    slideOutHorizontally(Motion.exit()) { -it / 6 * shift } +
                                        fadeOut(Motion.exit(Motion.QUICK))
                                )
                                .using(SizeTransform(clip = false))
                        },
                        label = "day"
                    ) { day ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            if (activeDay.isEmpty() && activeNoDate.isEmpty() && done.isEmpty()) {
                                EmptyDay(day = day)
                            } else {
                                if (activeDay.isNotEmpty()) {
                                    DayHeader(
                                        label = day.dayHeaderLabel(),
                                        count = activeDay.size,
                                        overdue = day.isBefore(today())
                                    )
                                    TaskSections(vm, data, activeDay, projectFilter)
                                }
                                if (activeNoDate.isNotEmpty()) {
                                    DayHeader(
                                        label = tr("Без срока"),
                                        count = activeNoDate.size
                                    )
                                    TaskSections(vm, data, activeNoDate, projectFilter)
                                }
                            }
                        }
                    }
                }

                if (done.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle(tr("Выполнено"), Modifier.weight(1f))
                            TextButton(onClick = { vm.clearShownCompleted(done.map { it.id }) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(tr("Очистить"))
                            }
                        }
                    }
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            TaskGroup(
                                tasks = done,
                                onToggle = { vm.toggleTask(it.id) },
                                onClick = { editing = it },
                                onPostpone = { vm.postponeTask(it.id) },
                                onToggleSubtask = { task, step -> vm.toggleSubtask(task.id, step.id) }
                            )
                        }
                    }
                }
            }
        }

        QuickAddBar(
            onAdd = { text ->
                // Дата и время вынимаются прямо из текста, остальное — как раньше.
                val date = vm.addQuickTask(
                    raw = text,
                    projectId = projectFilter ?: GENERAL_PROJECT_ID,
                    defaultDate = selectedDay
                )
                // Задача уехала на распознанную дату — показываем именно её.
                if (date != null) goToDay(date)
            },
            onOpenDetails = { draftTitle = it; creating = true },
            placeholder = if (isToday) tr("Новая задача")
            else trf("Задача на %1\$s", selectedDay.dayHeaderLabel().lowercase()),
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = quickAddBottomPadding(contentPadding) + 8.dp
            )
        )
    }

    if (creating) {
        TaskEditorSheet(
            initial = null,
            projects = data.projects,
            defaultTitle = draftTitle,
            defaultProjectId = projectFilter,
            defaultDue = selectedDay,
            onDismiss = { creating = false; draftTitle = "" },
            onSave = { task ->
                vm.addTask(task)
                creating = false
                draftTitle = ""
            }
        )
    }

    editing?.let { task ->
        TaskEditorSheet(
            initial = task,
            projects = data.projects,
            templateName = data.templates.firstOrNull { it.id == task.templateId }?.title,
            onSaveAsTemplate = if (task.templateId != null) null else {
                { vm.saveTaskAsTemplate(task); editing = null }
            },
            onDismiss = { editing = null },
            onSave = { vm.updateTask(it); editing = null },
            onDelete = { vm.deleteTask(task.id); editing = null }
        )
    }
}

/** Задачи одного блока со всеми жестами, которые к ним привязаны. */
@Composable
private fun TaskSections(
    vm: AppViewModel,
    data: AppData,
    tasks: List<Task>,
    projectFilter: String?
) {
    var editing by remember { mutableStateOf<Task?>(null) }
    ProjectSections(
        tasks = tasks,
        projects = data.projects,
        onToggle = { vm.toggleTask(it.id) },
        onClick = { editing = it },
        showProjectHeaders = projectFilter == null,
        showDue = false,
        onPostpone = { vm.postponeTask(it.id) },
        onReorder = { ids -> vm.reorderTasks(ids) },
        onToggleSubtask = { task, step -> vm.toggleSubtask(task.id, step.id) }
    )
    editing?.let { task ->
        TaskEditorSheet(
            initial = task,
            projects = data.projects,
            templateName = data.templates.firstOrNull { it.id == task.templateId }?.title,
            onSaveAsTemplate = if (task.templateId != null) null else {
                { vm.saveTaskAsTemplate(task); editing = null }
            },
            onDismiss = { editing = null },
            onSave = { vm.updateTask(it); editing = null },
            onDelete = { vm.deleteTask(task.id); editing = null }
        )
    }
}

/** Пустой день: подсказка вместо списка, но заголовок дня остаётся на месте. */
@Composable
private fun EmptyDay(day: LocalDate) {
    Column {
        DayHeader(label = day.dayHeaderLabel(), count = 0)
        EmptyState(
            icon = Icons.Rounded.ChecklistRtl,
            title = tr("День свободен"),
            subtitle = tr("Свайп вбок листает дни, а новая задача встанет на этот день.")
        )
    }
}

/**
 * Неделя над списком. У каждого дня число незакрытых задач висит значком
 * над кружком — так видно, где день забит, ещё до того как туда пролистал.
 */
@Composable
private fun WeekStrip(
    week: List<LocalDate>,
    selected: LocalDate,
    counts: Map<LocalDate, Int>,
    onPick: (LocalDate) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        week.forEach { date ->
            WeekDayCell(
                date = date,
                selected = date == selected,
                count = counts[date] ?: 0,
                onClick = { onPick(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeekDayCell(
    date: LocalDate,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val isToday = date == today()
    val circleColor = if (selected) accent else Color.Transparent
    val numberColor = when {
        selected -> contrastOn(accent)
        isToday -> accent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            date.weekdayShort(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(circleColor)
                    .then(
                        if (isToday && !selected)
                            Modifier.border(1.5.dp, accent, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Normal,
                    color = numberColor
                )
            }
            // Значок-счётчик над кружком, как уведомление на иконке приложения.
            if (count > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(if (count > 9) 18.dp else 15.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) contrastOn(accent) else MaterialTheme.colorScheme.error
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (count > 99) "99+" else count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) accent else MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

/** Плашка «просрочено» с переносом всех задач на сегодня. */
@Composable
private fun OverdueBanner(count: Int, onRoll: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(error.copy(alpha = 0.10f))
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.EventBusy,
            contentDescription = null,
            tint = error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            trf("Просрочено: %1\$d", count),
            style = MaterialTheme.typography.bodyMedium,
            color = error,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRoll) { Text(tr("На сегодня")) }
    }
}

/** Мягкая круглая кнопка-иконка для заголовков экранов. */
@Composable
fun SoftIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(if (selected) 28.dp else 22.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

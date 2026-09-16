package com.simply.app.ui.screens

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.Task
import com.simply.app.data.dayMonthLabel
import com.simply.app.data.iso
import com.simply.app.data.relativeLabel
import com.simply.app.data.monthYearLabel
import com.simply.app.data.activeTasks
import com.simply.app.data.inMatrixOrder
import com.simply.app.data.tasksLabel
import com.simply.app.data.startOfWeek
import com.simply.app.data.dayHeaderLabel
import com.simply.app.ui.components.FilterPill
import com.simply.app.data.today
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.ScreenHeader
import com.simply.app.ui.components.QuickAddBar
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.quickAddBottomPadding
import com.simply.app.ui.components.SimplyCard
import com.simply.app.ui.components.contrastOn
import com.simply.app.ui.theme.Accents
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import com.simply.app.ui.theme.Motion

private enum class CalendarMode(val label: String) {
    MONTH(tr("Месяц")), WEEK(tr("Неделя")), AGENDA(tr("Повестка"))
}

@Composable
fun CalendarScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenProjects: () -> Unit
) {
    var mode by remember { mutableStateOf(CalendarMode.MONTH) }
    var month by remember { mutableStateOf(YearMonth.from(today())) }
    var selected by remember { mutableStateOf(today()) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }

    val tasksByDate = remember(data.tasks, data.projects) {
        data.activeTasks().filter { it.due != null }.groupBy { it.due!! }
    }
    val habitsByDate = remember(data.habits) {
        val map = mutableMapOf<String, Int>()
        data.habits.forEach { h ->
            // День засчитан, только когда набрана дневная цель привычки.
            h.progress.forEach { (day, count) ->
                if (count >= h.perDay) map[day] = (map[day] ?: 0) + 1
            }
        }
        map.toMap()
    }

    // Точки под числом красим в цвета проектов: по календарю сразу видно,
    // чей это день, а не просто «что-то есть». Палитру снимаем один раз —
    // Accents.color() читает тему и в remember-блок не годится.
    val palette = List(Accents.size) { Accents.color(it) }
    val accentFallback = MaterialTheme.colorScheme.primary
    val projectColorById = remember(data.projects, palette) {
        data.projects.associate { p ->
            p.id to palette[((p.colorIndex % palette.size) + palette.size) % palette.size]
        }
    }

    // Границы ячеек в координатах окна: по ним ловим, куда бросили задачу.
    val cellBounds = remember { mutableStateMapOf<String, Rect>() }
    // Выполненное опускается вниз, всё остальное встаёт по матрице.
    val dayTasks = tasksByDate[selected.iso()].orEmpty()
        .inMatrixOrder()
        .sortedBy { it.done }

    Column(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ScreenHeader(
                title = tr("Календарь"),
                subtitle = tr("Точки — задачи, полоска — привычки"),
                trailing = {
                    SoftIconButton(Icons.Rounded.FolderOpen, tr("Проекты"), onOpenProjects)
                    SoftIconButton(Icons.Rounded.Settings, tr("Настройки"), onOpenSettings)
                }
            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CalendarMode.entries.toList(), key = { it.name }) { m ->
                    FilterPill(m.label, mode == m, { mode = m })
                }
            }
        }

        if (mode != CalendarMode.AGENDA) item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (mode == CalendarMode.WEEK) selected = selected.minusWeeks(1)
                    else month = month.minusMonths(1)
                }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = tr("Предыдущий месяц"))
                }
                Text(
                    if (mode == CalendarMode.WEEK)
                        "${selected.startOfWeek().dayMonthLabel()} — " +
                            selected.startOfWeek().plusDays(6).dayMonthLabel()
                    else month.atDay(1).monthYearLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(onClick = {
                    if (mode == CalendarMode.WEEK) selected = selected.plusWeeks(1)
                    else month = month.plusMonths(1)
                }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = tr("Следующий месяц"))
                }
            }
        }

        if (mode != CalendarMode.AGENDA) item {
            SimplyCard(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                contentPadding = PaddingValues(10.dp)
            ) {
              Column {
                Row(Modifier.fillMaxWidth()) {
                    listOf(tr("пн"), tr("вт"), tr("ср"), tr("чт"), tr("пт"), tr("сб"), tr("вс")).forEachIndexed { i, d ->
                        Text(
                            d,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (i >= 5) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Сетка уезжает в сторону перелистывания: раньше месяц
                // подменялся одним кадром и было непонятно, куда ушёл.
                val anchor = if (mode == CalendarMode.WEEK) selected.startOfWeek()
                else month.atDay(1)
                AnimatedContent(
                    targetState = anchor,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(Motion.enter()) { it / 6 * dir } + fadeIn(Motion.enter()))
                            .togetherWith(
                                slideOutHorizontally(Motion.exit()) { -it / 6 * dir } +
                                    fadeOut(Motion.exit(Motion.QUICK))
                            )
                    },
                    label = "grid"
                ) { shown ->
                  Column {
                    val rows = if (mode == CalendarMode.WEEK)
                        listOf((0..6).map { shown.plusDays(it.toLong()) })
                    else monthCells(YearMonth.from(shown)).chunked(7)
                    rows.forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            DayCell(
                                date = date,
                                inMonth = mode == CalendarMode.WEEK ||
                                    (date != null && YearMonth.from(date) == month),
                                selected = date == selected,
                                isToday = date == today(),
                                dots = date?.let { d ->
                                    tasksByDate[d.iso()].orEmpty()
                                        .filterNot { t -> t.done }
                                        .take(3)
                                        .map { t -> projectColorById[t.projectId] ?: accentFallback }
                                }.orEmpty(),
                                habitCount = date?.let { habitsByDate[it.iso()] } ?: 0,
                                modifier = Modifier.weight(1f),
                                onBounds = { rect ->
                                    if (date != null) cellBounds[date.iso()] = rect
                                }
                            ) { date?.let { selected = it } }
                        }
                    }
                    }
                  }
                }
              }
            }
        }

        if (mode == CalendarMode.AGENDA) {
            val agenda = (0..13).map { today().plusDays(it.toLong()) }
                .mapNotNull { date ->
                    val items = tasksByDate[date.iso()].orEmpty().filterNot { it.done }
                    if (items.isEmpty()) null else date to items
                }
            if (agenda.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.EventAvailable,
                        title = tr("Ближайшие две недели свободны"),
                        subtitle = tr("Свободно. Можно добавить задачу — или оставить как есть.")
                    )
                }
            }
            items(agenda, key = { it.first.toString() }) { (date, items) ->
                Column(Modifier.padding(horizontal = 16.dp)) {
                    DayHeader(
                        label = date.dayHeaderLabel(),
                        count = items.size,
                        overdue = false
                    )
                    ProjectSections(
                        tasks = items,
                        projects = data.projects,
                        onToggle = { vm.toggleTask(it.id) },
                        onClick = { editing = it },
                        showDue = false,
                        onPostpone = { vm.postponeTask(it.id) },
                        onToggleSubtask = { task, step -> vm.toggleSubtask(task.id, step.id) }
                    )
                }
            }
        }

        if (mode != CalendarMode.AGENDA) item {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(selected.dayMonthLabel(), Modifier.weight(1f))
                TextButton(onClick = { creating = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(tr("Задача"))
                }
            }
        }

        val habitsDone = habitsByDate[selected.iso()] ?: 0
        if (habitsDone > 0 && mode != CalendarMode.AGENDA) {
            item {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SimplyCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(
                            trf("Привычек отмечено: %1\$d из %2\$d", habitsDone, data.habits.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (dayTasks.isEmpty() && mode != CalendarMode.AGENDA) {
            item {
                EmptyState(
                    icon = Icons.Rounded.EventAvailable,
                    title = tr("На этот день ничего нет"),
                    subtitle = tr("Свободно. Можно добавить задачу — или оставить как есть.")
                )
            }
        }

        if (dayTasks.isNotEmpty() && mode != CalendarMode.AGENDA) {
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    ProjectSections(
                        tasks = dayTasks,
                        projects = data.projects,
                        onToggle = { vm.toggleTask(it.id) },
                        onClick = { editing = it },
                        showDue = false,
                        onPostpone = { vm.postponeTask(it.id) },
                        onToggleSubtask = { task, step -> vm.toggleSubtask(task.id, step.id) },
                        onDropAt = { task, point ->
                            // Долгое нажатие и перенос на любой день сетки.
                            cellBounds.entries
                                .firstOrNull { it.value.contains(point) }
                                ?.let { (key, _) ->
                                    runCatching { LocalDate.parse(key) }.getOrNull()
                                        ?.let { date -> vm.moveTaskToDate(task.id, date) }
                                }
                        }
                    )
                }
            }
        }
    }

    QuickAddBar(
        onAdd = { text -> vm.addQuickTask(raw = text, defaultDate = selected) },
        onOpenDetails = { draftTitle = it; creating = true },
        placeholder = trf("Задача на %1\$s", selected.relativeLabel().lowercase()),
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
            defaultDue = selected,
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
            onDismiss = { editing = null },
            onSave = { vm.updateTask(it); editing = null },
            onDelete = { vm.deleteTask(task.id); editing = null }
        )
    }
}

/** Сетка месяца: 6 строк по 7 дней, начиная с понедельника. */
private fun monthCells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val shift = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val start = first.minusDays(shift.toLong())
    return (0 until 42).map { start.plusDays(it.toLong()) }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    inMonth: Boolean,
    selected: Boolean,
    isToday: Boolean,
    dots: List<Color>,
    habitCount: Int,
    modifier: Modifier = Modifier,
    onBounds: (Rect) -> Unit = {},
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(
        if (selected) accent else Color.Transparent,
        label = "dayBg"
    )
    val textColor = when {
        selected -> contrastOn(accent)
        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        isToday -> accent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(0.92f)
            .onGloballyPositioned { onBounds(it.boundsInWindow()) }
            .padding(2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(
                if (isToday && !selected) Modifier.border(1.5.dp, accent, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    dots.forEach { dot ->
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                // На выбранном дне цвета проектов утонули бы в заливке.
                                .background(if (selected) contrastOn(accent) else dot)
                        )
                    }
                }
                if (habitCount > 0) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier
                            .height(2.dp)
                            .size(width = 12.dp, height = 2.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) contrastOn(accent).copy(alpha = 0.7f)
                                else Accents.color(1)
                            )
                    )
                }
            }
        }
    }
}

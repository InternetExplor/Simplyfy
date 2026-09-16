package com.simply.app.ui.screens

import com.simply.app.ui.components.dialogEnter
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.Habit
import com.simply.app.data.inMatrixOrder
import com.simply.app.data.Project
import com.simply.app.data.Task
import com.simply.app.data.dayHeaderLabel
import com.simply.app.data.daysLabel
import com.simply.app.data.formatMinutes
import com.simply.app.data.tasksLabel
import com.simply.app.data.today
import com.simply.app.data.streak
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.ProjectIcons
import com.simply.app.ui.components.QuickAddBar
import com.simply.app.ui.components.RenameDialog
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.SimplyCard
import com.simply.app.ui.components.StatTile
import com.simply.app.ui.components.quickAddBottomPadding
import com.simply.app.ui.theme.Accents
import java.time.LocalDate

private const val NO_SECTION = "__none__"

@Composable
fun ProjectScreen(
    vm: AppViewModel,
    data: AppData,
    projectId: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val project = data.projects.firstOrNull { it.id == projectId }
    LaunchedEffect(project) { if (project == null) onBack() }
    if (project == null) return

    var sectionFilter by remember(projectId) { mutableStateOf<String?>(null) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editingProject by remember { mutableStateOf(false) }
    var newSection by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var creatingHabit by remember { mutableStateOf(false) }

    val accent = Accents.color(project.colorIndex)
    val all = data.tasks.filter { it.projectId == projectId }
    val visible = remember(all, sectionFilter) {
        when (sectionFilter) {
            null -> all
            NO_SECTION -> all.filter { it.sectionId == null }
            else -> all.filter { it.sectionId == sectionFilter }
        }
    }
    val active = visible.filterNot { it.done }.inMatrixOrder()
    val done = if (data.settings.showCompleted) visible.filter { it.done } else emptyList()
    val focusMinutes = remember(data.sessions, projectId) {
        data.sessions.filter { it.projectId == projectId }.sumOf { it.minutes }
    }
    val progress = if (all.isEmpty()) 0f else all.count { it.done }.toFloat() / all.size
    val habits = data.habits.filter { it.projectId == projectId }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = contentPadding.calculateTopPadding())
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = tr("Назад"))
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ProjectIcons.vector(project.icon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                tr(project.name),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            SoftIconButton(Icons.Rounded.Edit, tr("Изменить проект")) { editingProject = true }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Завершённый проект открывается как обычно, но сразу говорит, что он
            // закрыт: иначе непонятно, почему его задач нет в дне и в матрице.
            if (project.completed) {
                item {
                    FinishedBanner(
                        project = project,
                        onReopen = { vm.setProjectCompleted(projectId, false) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (project.note.isNotBlank()) {
                item {
                    SimplyCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Text(
                            project.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        value = all.count { !it.done }.toString(),
                        label = tr("активных"),
                        modifier = Modifier.weight(1f),
                        accent = accent
                    )
                    StatTile(
                        value = all.count { it.done }.toString(),
                        label = tr("выполнено"),
                        modifier = Modifier.weight(1f),
                        accent = Accents.color(1)
                    )
                    StatTile(
                        value = formatMinutes(focusMinutes),
                        label = tr("в фокусе"),
                        modifier = Modifier.weight(1f),
                        accent = Accents.color(4)
                    )
                }
            }

            if (all.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    ProgressLine(progress, accent)
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterPill(
                            tr("Все"),
                            sectionFilter == null,
                            { sectionFilter = null },
                            accent = accent,
                            tonal = true
                        )
                    }
                    items(project.sections.sortedBy { it.order }, key = { it.id }) { section ->
                        FilterPill(
                            section.name,
                            sectionFilter == section.id,
                            { sectionFilter = if (sectionFilter == section.id) null else section.id },
                            count = all.count { it.sectionId == section.id && !it.done },
                            accent = accent,
                            tonal = true
                        )
                    }
                    if (project.sections.isNotEmpty()) {
                        item {
                            FilterPill(
                                tr("Без раздела"),
                                sectionFilter == NO_SECTION,
                                {
                                    sectionFilter =
                                        if (sectionFilter == NO_SECTION) null else NO_SECTION
                                },
                                accent = accent,
                                tonal = true
                            )
                        }
                    }
                    item {
                        FilterPill(tr("＋ Раздел"), false, { newSection = true })
                    }
                }
            }

            if (active.isEmpty() && done.isEmpty()) {
                item {
                    EmptyState(
                        icon = ProjectIcons.vector(project.icon),
                        title = tr("Здесь пока пусто"),
                        subtitle = tr("Добавь задачу внизу — она попадёт в этот проект.")
                    )
                }
            }

            // Без фильтра и при наличии разделов группируем по разделам, иначе — по дням.
            if (sectionFilter == null && project.sections.isNotEmpty()) {
                val bySection = active.groupBy { it.sectionId }
                project.sections.sortedBy { it.order }.forEach { section ->
                    val items = bySection[section.id].orEmpty()
                    if (items.isNotEmpty()) {
                        item(key = "h-" + section.id) {
                            SectionHeaderRow(
                                title = section.name,
                                count = items.size,
                                onDelete = { vm.deleteSection(projectId, section.id) },
                                onRename = { name ->
                                    vm.renameSection(projectId, section.id, name)
                                }
                            )
                        }
                        item(key = "g-" + section.id) {
                            TaskGroup(
                                tasks = items,
                                onToggle = { vm.toggleTask(it.id) },
                                onClick = { editingTask = it },
                                tint = accent,
                                onPostpone = { vm.postponeTask(it.id) },
                                onReorder = { ids -> vm.reorderTasks(ids) },
                                onToggleSubtask = { task, step ->
                                    vm.toggleSubtask(task.id, step.id)
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
                val loose = bySection[null].orEmpty()
                if (loose.isNotEmpty()) {
                    item { SectionHeaderRow(tr("Без раздела"), loose.size, null) }
                    item {
                        TaskGroup(
                            tasks = loose,
                            onToggle = { vm.toggleTask(it.id) },
                            onClick = { editingTask = it },
                            tint = accent,
                            onPostpone = { vm.postponeTask(it.id) },
                            onReorder = { ids -> vm.reorderTasks(ids) },
                            onToggleSubtask = { task, step ->
                                vm.toggleSubtask(task.id, step.id)
                            }
                        )
                    }
                }
            } else {
                val buckets = active.groupBy { it.dueDate }.toSortedMap(nullsLast())
                buckets.forEach { (date, items) ->
                    item(key = "d-" + (date?.toString() ?: "none")) {
                        DayHeader(
                            label = date?.dayHeaderLabel() ?: tr("Без срока"),
                            count = items.size,
                            overdue = date != null && date.isBefore(today())
                        )
                    }
                    item(key = "dg-" + (date?.toString() ?: "none")) {
                        TaskGroup(
                            tasks = items,
                            onToggle = { vm.toggleTask(it.id) },
                            onClick = { editingTask = it },
                            showDue = false,
                            tint = accent,
                            onPostpone = { vm.postponeTask(it.id) },
                            onReorder = { ids -> vm.reorderTasks(ids) },
                            onToggleSubtask = { task, step ->
                                vm.toggleSubtask(task.id, step.id)
                            }
                        )
                    }
                }
            }

            item {
                SectionHeaderRow(
                    title = tr("Привычки"),
                    count = habits.size,
                    onDelete = null
                )
            }
            item {
                ProjectHabits(
                    habits = habits,
                    accent = accent,
                    onToggle = { vm.toggleHabit(it.id, today()) },
                    onEdit = { editingHabit = it },
                    onAdd = { creatingHabit = true }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (done.isNotEmpty()) {
                item { SectionTitle(tr("Выполнено"), trailing = tasksLabel(done.size)) }
                item {
                    TaskGroup(
                        tasks = done,
                        onToggle = { vm.toggleTask(it.id) },
                        onClick = { editingTask = it }
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }

        QuickAddBar(
            onAdd = { text ->
                vm.addQuickTask(
                    raw = text,
                    projectId = projectId,
                    sectionId = sectionFilter?.takeIf { it != NO_SECTION }
                )
            },
            onOpenDetails = { title ->
                editingTask = Task(
                    title = title,
                    projectId = projectId,
                    sectionId = sectionFilter?.takeIf { it != NO_SECTION }
                )
            },
            placeholder = trf("Задача в «%1\$s»", tr(project.name)),
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = quickAddBottomPadding(contentPadding) + 8.dp
            )
        )
    }

    editingTask?.let { task ->
        val isNew = data.tasks.none { it.id == task.id }
        TaskEditorSheet(
            initial = if (isNew) null else task,
            // Проект самого экрана показываем всегда: внутри завершённого
            // проекта его задачу иначе некуда было бы отнести.
            projects = data.projects.filter { it.isActive || it.id == projectId },
            defaultTitle = if (isNew) task.title else "",
            defaultProjectId = projectId,
            templateName = data.templates.firstOrNull { it.id == task.templateId }?.title,
            onDismiss = { editingTask = null },
            onSave = { saved ->
                if (isNew) vm.addTask(saved.copy(sectionId = task.sectionId))
                else vm.updateTask(saved)
                editingTask = null
            },
            onDelete = if (isNew) null else {
                { vm.deleteTask(task.id); editingTask = null }
            }
        )
    }

    if (creatingHabit) {
        HabitEditorSheet(
            initial = null,
            data = data,
            onDismiss = { creatingHabit = false },
            onSave = { habit ->
                vm.addHabit(
                    name = habit.name,
                    icon = habit.icon,
                    colorIndex = habit.colorIndex,
                    targetPerWeek = habit.targetPerWeek,
                    dailyTarget = habit.dailyTarget,
                    reminderTime = habit.reminderTime,
                    projectId = habit.projectId,
                    groupId = habit.groupId
                )
                creatingHabit = false
            },
            onDelete = null,
            onCreateGroup = { vm.addHabitGroup(it) },
            defaultProjectId = projectId
        )
    }

    editingHabit?.let { habit ->
        HabitEditorSheet(
            initial = habit,
            data = data,
            onDismiss = { editingHabit = null },
            onSave = { vm.updateHabit(it); editingHabit = null },
            onDelete = { vm.deleteHabit(habit.id); editingHabit = null },
            onCreateGroup = { vm.addHabitGroup(it) }
        )
    }

    if (newSection) {
        TextPromptDialog(
            title = tr("Новый раздел"),
            placeholder = tr("Название"),
            onDismiss = { newSection = false },
            onConfirm = { vm.addSection(projectId, it); newSection = false }
        )
    }

    if (editingProject) {
        ProjectEditorSheet(
            project = project,
            isNew = false,
            taskCount = all.size,
            onDismiss = { editingProject = false },
            onSave = { vm.updateProject(it); editingProject = false },
            onComplete = if (project.isGeneral) null else {
                {
                    vm.setProjectCompleted(projectId, !project.completed)
                    editingProject = false
                    // Завершили — уходим со экрана: проекта больше нет в работе.
                    // Вернули в работу — остаёмся, человек пришёл сюда работать.
                    if (!project.completed) onBack()
                }
            },
            onDelete = if (project.isGeneral) null else {
                { editingProject = false; confirmDelete = true }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            modifier = Modifier.dialogEnter(),
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("Удалить проект?")) },
            text = { Text(tr("Задачи проекта переедут в «Общий», разделы будут удалены.")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteProject(projectId)
                    onBack()
                }) { Text(tr("Удалить"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(tr("Отмена")) }
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }
}

/** Проект закрыт: что это значит и как вернуть его в работу. */
@Composable
private fun FinishedBanner(project: Project, onReopen: () -> Unit) {
    val accent = Accents.color(project.colorIndex)
    SimplyCard(
        Modifier.fillMaxWidth(),
        color = accent.copy(alpha = 0.10f),
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Проект завершён"), style = MaterialTheme.typography.titleSmall)
                Text(
                    tr("Его задачи не показываются в дне, матрице и календаре."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onReopen) { Text(tr("Вернуть")) }
        }
    }
}

/**
 * Привычки проекта одной карточкой: кружок сегодняшнего дня и название.
 * Полная неделя и статистика живут на экране «Привычки» — здесь важно
 * только то, сделано ли это сегодня.
 */
@Composable
private fun ProjectHabits(
    habits: List<Habit>,
    accent: androidx.compose.ui.graphics.Color,
    onToggle: (Habit) -> Unit,
    onEdit: (Habit) -> Unit,
    onAdd: () -> Unit
) {
    com.simply.app.ui.components.GroupedCard(Modifier.fillMaxWidth()) {
        habits.forEach { habit ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(habit) }
                    .padding(start = 6.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.simply.app.ui.components.CheckCircle(
                    checked = habit.isDoneOn(today()),
                    onClick = { onToggle(habit) },
                    accent = Accents.color(habit.colorIndex),
                    label = tr(habit.name)
                )
                Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                    Text(
                        habit.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val streakDays = habit.streak()
                    Text(
                        if (streakDays > 0) trf("серия %1\$s", daysLabel(streakDays))
                        else trf("цель %1\$d в неделю", habit.targetPerWeek),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    com.simply.app.ui.components.HabitIcons.vector(habit.icon),
                    contentDescription = null,
                    tint = Accents.color(habit.colorIndex),
                    modifier = Modifier.size(18.dp)
                )
            }
            com.simply.app.ui.components.RowSeparator(startInset = 58.dp)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdd)
                .padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                if (habits.isEmpty()) tr("Добавить привычку проекта") else tr("Добавить привычку"),
                style = MaterialTheme.typography.bodyLarge,
                color = accent
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    count: Int,
    onDelete: (() -> Unit)?,
    onRename: ((String) -> Unit)? = null
) {
    var renaming by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(
            title,
            Modifier
                .weight(1f)
                .then(
                    if (onRename == null) Modifier
                    else Modifier.clickable { renaming = true }
                ),
            trailing = count.toString()
        )
        if (onRename != null) {
            IconButton(onClick = { renaming = true }) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = tr("Переименовать раздел"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = tr("Удалить раздел"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (renaming && onRename != null) {
        RenameDialog(
            title = tr("Название раздела"),
            initial = title,
            onDismiss = { renaming = false },
            onConfirm = { name -> onRename(name); renaming = false }
        )
    }
}

@Composable
fun TextPromptDialog(
    title: String,
    placeholder: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        modifier = Modifier.dialogEnter(),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(tr("Готово"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена")) } },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditorSheet(
    project: Project,
    isNew: Boolean,
    taskCount: Int,
    onDismiss: () -> Unit,
    onSave: (Project) -> Unit,
    onComplete: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(project.name) }
    var note by remember { mutableStateOf(project.note) }
    var color by remember { mutableStateOf(project.colorIndex) }
    var icon by remember { mutableStateOf(project.icon) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        // Содержимое длиннее экрана (особенно с открытой клавиатурой) —
        // без прокрутки нижние кнопки становятся недоступны.
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                if (isNew) tr("Новый проект") else tr("Проект"),
                style = MaterialTheme.typography.titleLarge
            )
            if (!isNew) {
                Spacer(Modifier.height(4.dp))
                Text(
                    tasksLabel(taskCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tr("Название")) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(tr("Описание (необязательно)")) },
                shape = MaterialTheme.shapes.small,
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle(tr("Цвет"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(Accents.size) { i ->
                    ColorDot(Accents.color(i), color == i) { color = i }
                }
            }

            SectionTitle(tr("Значок"))
            com.simply.app.ui.components.IconPickerGrid(
                keys = ProjectIcons.keys,
                common = ProjectIcons.common,
                selected = icon,
                accent = Accents.color(color),
                vectorFor = { ProjectIcons.vector(it) },
                onPick = { icon = it }
            )

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    onSave(
                        project.copy(
                            name = name.trim().ifEmpty { project.name },
                            note = note.trim(),
                            colorIndex = color,
                            icon = icon
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (isNew) tr("Создать") else tr("Сохранить"))
            }

            if (onComplete != null) {
                TextButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        if (project.completed) Icons.Rounded.Replay else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (project.completed) tr("Вернуть в работу")
                        else tr("Завершить проект")
                    )
                }
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Удалить проект"), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

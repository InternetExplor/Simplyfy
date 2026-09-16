package com.simply.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.activeTasks
import com.simply.app.data.inMatrixOrder
import com.simply.app.data.AppData
import com.simply.app.data.Habit
import com.simply.app.data.Project
import com.simply.app.data.Task
import com.simply.app.data.dayHeaderLabel
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.GroupedCard
import com.simply.app.ui.components.HabitIcons
import com.simply.app.ui.components.ProjectIcons
import com.simply.app.ui.components.RowSeparator
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.softSurface
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.theme.Accents

/**
 * Поиск отдельной страницей: поле живёт в шапке, клавиатура открывается сразу,
 * результаты сгруппированы по типу. Раньше поиск был строкой внутри списка
 * задач и отъедал у него место, ничего не находя в привычках и проектах.
 */
@Composable
fun SearchScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    onOpenProject: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Task?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val text = query.trim()
    val tasks = remember(data.tasks, data.projects, text) {
        if (text.isEmpty()) emptyList()
        else data.activeTasks().filter {
            it.title.contains(text, true) || it.note.contains(text, true) ||
                it.subtasks.any { step -> step.title.contains(text, true) }
        }.inMatrixOrder().sortedBy { it.done }
    }
    val habits = remember(data.habits, text) {
        if (text.isEmpty()) emptyList() else data.habits.filter { it.name.contains(text, true) }
    }
    val projects = remember(data.projects, text) {
        if (text.isEmpty()) emptyList()
        // Имя встроенного проекта хранится по-русски, а на экране показывается
        // переводом — ищем по обоим, иначе «Work» не находит «Работу».
        else data.projects.filter {
            it.name.contains(text, true) || tr(it.name).contains(text, true) ||
                it.note.contains(text, true)
        }
    }
    val nothing = text.isNotEmpty() && tasks.isEmpty() && habits.isEmpty() && projects.isEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        SearchBar(
            query = query,
            onQueryChange = { query = it },
            onClear = { query = "" },
            onBack = onBack,
            focusRequester = focusRequester
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            )
        ) {
            if (text.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = tr("Что ищем?"),
                        subtitle = tr("Поиск идёт по задачам, их шагам, привычкам и проектам.")
                    )
                }
            }
            if (nothing) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = tr("Ничего не нашлось"),
                        subtitle = tr("Попробуй другое слово.")
                    )
                }
            }

            if (tasks.isNotEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle(tr("Задачи"), trailing = tasks.size.toString())
                    }
                }
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        TaskGroup(
                            tasks = tasks,
                            onToggle = { vm.toggleTask(it.id) },
                            onClick = { editing = it },
                            onPostpone = { vm.postponeTask(it.id) },
                            onToggleSubtask = { task, step -> vm.toggleSubtask(task.id, step.id) }
                        )
                    }
                }
            }

            if (habits.isNotEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle(tr("Привычки"), trailing = habits.size.toString())
                    }
                }
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        GroupedCard(Modifier.fillMaxWidth()) {
                            habits.forEachIndexed { index, habit ->
                                HabitHit(habit)
                                if (index != habits.lastIndex) RowSeparator(startInset = 58.dp)
                            }
                        }
                    }
                }
            }

            if (projects.isNotEmpty()) {
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle(tr("Проекты"), trailing = projects.size.toString())
                    }
                }
                item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        GroupedCard(Modifier.fillMaxWidth()) {
                            projects.forEachIndexed { index, project ->
                                ProjectHit(
                                    project = project,
                                    count = data.tasks.count {
                                        it.projectId == project.id && !it.done
                                    },
                                    onClick = { onOpenProject(project.id) }
                                )
                                if (index != projects.lastIndex) RowSeparator(startInset = 58.dp)
                            }
                        }
                    }
                }
            }
        }
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

/** Шапка страницы: стрелка назад и поле — как в системных настройках. */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = tr("Назад"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(4.dp))
        Row(
            Modifier
                .weight(1f)
                .softSurface(
                    CircleShape,
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    elevation = 0.dp
                )
                .height(46.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        tr("Поиск по задачам"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = tr("Очистить"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitHit(habit: Habit) {
    val accent = Accents.color(habit.colorIndex)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                HabitIcons.vector(habit.icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            habit.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProjectHit(project: Project, count: Int, onClick: () -> Unit) {
    val accent = Accents.color(project.colorIndex)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ProjectIcons.vector(project.icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            tr(project.name),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Подпись срока для строки результата — чтобы задача не висела без контекста. */
internal fun Task.searchHint(): String? = dueDate?.dayHeaderLabel()

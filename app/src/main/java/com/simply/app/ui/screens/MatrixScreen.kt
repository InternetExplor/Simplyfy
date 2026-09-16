package com.simply.app.ui.screens

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inbox
import com.simply.app.ui.components.SimplyCard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.core.animate
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.GENERAL_PROJECT_ID
import com.simply.app.data.Quadrant
import com.simply.app.data.Task
import com.simply.app.data.activeTasks
import com.simply.app.data.inMatrixOrder
import com.simply.app.data.tasksLabel
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.ScreenHeader
import com.simply.app.ui.components.QuickAddBar
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.quickAddBottomPadding
import com.simply.app.ui.components.softSurface
import com.simply.app.ui.components.contrastOn
import com.simply.app.ui.theme.Accents

@Composable
fun MatrixScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenProjects: () -> Unit
) {
    // null — выбраны «Входящие»: задачи, которым квадрант ещё не назначен.
    var selected by remember { mutableStateOf<Quadrant?>(null) }
    var projectFilter by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var creating by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }

    val activeProjects = remember(data.projects) { data.projects.filter { it.isActive } }

    // Проект завершили, пока он стоял в фильтре — возвращаемся ко всем задачам,
    // иначе экран остался бы пустым без всякого объяснения.
    LaunchedEffect(activeProjects, projectFilter) {
        if (projectFilter != null && activeProjects.none { it.id == projectFilter }) {
            projectFilter = null
        }
    }

    val open = remember(data.tasks, data.projects, projectFilter) {
        data.activeTasks()
            .filterNot { it.done }
            .filter { projectFilter == null || it.projectId == projectFilter }
            .inMatrixOrder()
    }
    val byQuadrant = remember(open) { open.groupBy { it.quadrant } }
    val inbox = byQuadrant[null].orEmpty()
    val inSelected = remember(byQuadrant, selected) { byQuadrant[selected].orEmpty() }

    // Разобрали последнюю задачу — уходим из пустых «Входящих» в первый квадрант.
    LaunchedEffect(inbox.isEmpty()) {
        if (selected == null && inbox.isEmpty()) selected = Quadrant.DO
    }

    Column(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                title = tr("Матрица"),
                subtitle = tr("Разложи задачи по важности и срочности"),
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

        item {
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (inbox.isNotEmpty()) {
                    InboxTile(
                        count = inbox.size,
                        selected = selected == null,
                        onClick = { selected = null }
                    )
                }
                AxisLabels()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuadrantTile(Quadrant.DO, byQuadrant, selected, Modifier.weight(1f)) { selected = it }
                    QuadrantTile(Quadrant.PLAN, byQuadrant, selected, Modifier.weight(1f)) { selected = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuadrantTile(Quadrant.DELEGATE, byQuadrant, selected, Modifier.weight(1f)) { selected = it }
                    QuadrantTile(Quadrant.DROP, byQuadrant, selected, Modifier.weight(1f)) { selected = it }
                }
            }
        }

        item {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(
                    selected?.let { tr(it.label) } ?: tr("Входящие"),
                    Modifier.weight(1f)
                )
                TextButton(onClick = { creating = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(tr("Добавить"))
                }
            }
        }

        if (inSelected.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.GridView,
                    title = if (selected == null) tr("Всё разобрано") else tr("Квадрант пуст"),
                    subtitle = selected?.let { quadrantHint(it) }
                        ?: tr("Новые задачи ждут здесь, пока не разложишь их по квадрантам.")
                )
            }
        }

        // «Входящие» — режим разбора: под каждой задачей четыре кнопки-квадранта.
        if (selected == null) {
            items(inSelected, key = { it.id }) { task ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                    InboxSortCard(
                        task = task,
                        project = data.projects.firstOrNull { it.id == task.projectId },
                        onToggle = { vm.toggleTask(task.id) },
                        onClick = { editing = task },
                        onAssign = { quadrant -> vm.updateTask(task.copy(quadrant = quadrant)) }
                    )
                }
            }
        } else if (inSelected.isNotEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    ProjectSections(
                        tasks = inSelected,
                        projects = data.projects,
                        onToggle = { vm.toggleTask(it.id) },
                        onClick = { editing = it },
                        showProjectHeaders = projectFilter == null,
                        onPostpone = { vm.postponeTask(it.id) },
                        onToggleSubtask = { task, step -> vm.toggleSubtask(task.id, step.id) }
                    )
                }
            }
        }
    }

    QuickAddBar(
        onAdd = { text ->
            vm.addQuickTask(
                raw = text,
                projectId = projectFilter ?: GENERAL_PROJECT_ID,
                quadrant = selected
            )
        },
        onOpenDetails = { draftTitle = it; creating = true },
        placeholder = selected?.let { trf("Новая задача · %1\$s", tr(it.shortLabel).lowercase()) }
            ?: tr("Новая задача"),
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
            defaultQuadrant = selected,
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

private fun quadrantHint(q: Quadrant): String = when (q) {
    Quadrant.DO -> tr("Сюда попадают задачи «важно + срочно». Хорошо, что их нет.")
    Quadrant.PLAN -> tr("Самый полезный квадрант: важное без пожара. Запланируй что-нибудь.")
    Quadrant.DELEGATE -> tr("Срочное, но не важное — делегируй или закрой за пару минут.")
    Quadrant.DROP -> tr("Ни важное, ни срочное. Такое можно и не делать.")
}

@Composable
private fun AxisLabels() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AxisChip(tr("Срочно"), Modifier.weight(1f))
        AxisChip(tr("Не срочно"), Modifier.weight(1f))
    }
}

@Composable
private fun AxisChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp)
    )
}

@Composable
private fun QuadrantTile(
    quadrant: Quadrant,
    byQuadrant: Map<Quadrant?, List<Task>>,
    selected: Quadrant?,
    modifier: Modifier = Modifier,
    onSelect: (Quadrant) -> Unit
) {
    val accent = Accents.color(quadrant.accentIndex)
    val isSelected = quadrant == selected
    val tasks = byQuadrant[quadrant].orEmpty()

    val card = MaterialTheme.colorScheme.surfaceContainerLow
    val bg by animateColorAsState(
        if (isSelected) accent.copy(alpha = 0.14f).compositeOver(card) else card,
        label = "quadrantBg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) accent.copy(alpha = 0.55f) else Color.Transparent,
        label = "quadrantBorderColor"
    )
    val shape = RoundedCornerShape(22.dp)

    val spoken = trf("%1\$s, задач: %2\$d", tr(quadrant.shortLabel), tasks.size)
    Column(
        modifier = modifier
            .height(152.dp)
            .softSurface(shape, bg)
            .border(1.5.dp, borderColor, shape)
            .semantics { contentDescription = spoken }
            .clickable { onSelect(quadrant) }
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                tasks.size.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tr(quadrant.shortLabel),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tr(quadrant.label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        if (tasks.isNotEmpty()) {
            Text(
                tasks.first().title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Пятая корзина над сеткой: задачи, которым квадрант ещё не назначен. */
@Composable
private fun InboxTile(count: Int, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val card = MaterialTheme.colorScheme.surfaceContainerLow
    val bg by animateColorAsState(
        if (selected) accent.copy(alpha = 0.14f).compositeOver(card) else card,
        label = "inboxBg"
    )
    val borderColor by animateColorAsState(
        if (selected) accent.copy(alpha = 0.55f) else Color.Transparent,
        label = "inboxBorder"
    )
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softSurface(shape, bg)
            .border(1.5.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Inbox,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("Входящие"), style = MaterialTheme.typography.titleSmall)
            Text(
                tr("Разложи по квадрантам"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = accent
        )
    }
}

/**
 * Задача из «Входящих» вместе с кнопками разбора: одно касание —
 * и она уезжает в нужный квадрант.
 */
@Composable
private fun InboxSortCard(
    task: Task,
    project: com.simply.app.data.Project?,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onAssign: (Quadrant) -> Unit
) {
    // Свайпы — два самых частых решения; остальные два остаются кнопками.
    // Вертикальные жесты не берём: они спорили бы с прокруткой списка.
    val scope = rememberCoroutineScope()
    var dragged by remember(task.id) { mutableFloatStateOf(0f) }

    Box {
        if (dragged != 0f) {
            InboxSwipeBackground(if (dragged > 0) Quadrant.PLAN else Quadrant.DROP)
        }
        Box(
            Modifier
                .offset { IntOffset(dragged.roundToInt(), 0) }
                .pointerInput(task.id) {
                    val threshold = size.width * 0.3f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragged += amount
                        },
                        onDragEnd = {
                            val moved = dragged
                            when {
                                moved > threshold -> onAssign(Quadrant.PLAN)
                                moved < -threshold -> onAssign(Quadrant.DROP)
                            }
                            scope.launch { animate(moved, 0f) { value, _ -> dragged = value } }
                        },
                        onDragCancel = {
                            val moved = dragged
                            scope.launch { animate(moved, 0f) { value, _ -> dragged = value } }
                        }
                    )
                }
        ) {
            InboxSortCardContent(task, project, onToggle, onClick, onAssign)
        }
    }
}

@Composable
private fun BoxScope.InboxSwipeBackground(quadrant: Quadrant) {
    val color = Accents.color(quadrant.accentIndex)
    val toStart = quadrant == Quadrant.PLAN
    Row(
        Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(22.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (toStart) Arrangement.Start else Arrangement.End
    ) {
        Text(tr(quadrant.label), style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun InboxSortCardContent(
    task: Task,
    project: com.simply.app.data.Project?,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onAssign: (Quadrant) -> Unit
) {
    val accent = Accents.color(project?.colorIndex ?: 0)
    SimplyCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        Column {
            TaskRow(
                task = task,
                accent = accent,
                onToggle = onToggle,
                onClick = onClick
            )
            Column(
                Modifier.padding(start = 14.dp, end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuadrantPill(Quadrant.DO, onAssign)
                    QuadrantPill(Quadrant.PLAN, onAssign)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuadrantPill(Quadrant.DELEGATE, onAssign)
                    QuadrantPill(Quadrant.DROP, onAssign)
                }
            }
        }
    }
}

@Composable
private fun QuadrantPill(quadrant: Quadrant, onAssign: (Quadrant) -> Unit) {
    // Это кнопки действия, а не состояние фильтра, поэтому они всегда
    // покрашены в цвет своего квадранта — серые читались бы как «выключено».
    FilterPill(
        tr(quadrant.shortLabel),
        selected = true,
        onClick = { onAssign(quadrant) },
        accent = Accents.color(quadrant.accentIndex),
        tonal = true
    )
}

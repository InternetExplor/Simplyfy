package com.simply.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.runtime.key
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.Project
import com.simply.app.data.RU
import com.simply.app.data.relativeLabel
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import com.simply.app.data.Subtask
import com.simply.app.data.Task
import com.simply.app.data.tasksLabel
import com.simply.app.ui.components.CheckCircle
import com.simply.app.ui.components.GroupedCard
import com.simply.app.ui.components.LabeledRow
import com.simply.app.ui.components.RowSeparator
import com.simply.app.ui.theme.Accents
import com.simply.app.ui.theme.Motion
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

/**
 * Задачи одного дня, разложенные по проектам: у каждого проекта свой блок,
 * подкрашенный его цветом, с отдельным заголовком.
 */
@Composable
fun ProjectSections(
    tasks: List<Task>,
    projects: List<Project>,
    onToggle: (Task) -> Unit,
    onClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
    showProjectHeaders: Boolean = true,
    showDue: Boolean = true,
    onPostpone: ((Task) -> Unit)? = null,
    onReorder: ((List<String>) -> Unit)? = null,
    onDropAt: ((Task, Offset) -> Unit)? = null,
    onToggleSubtask: ((Task, Subtask) -> Unit)? = null
) {
    if (tasks.isEmpty()) return

    if (!showProjectHeaders) {
        TaskGroup(
            tasks, onToggle, onClick, modifier,
            showDue = showDue,
            onPostpone = onPostpone,
            onReorder = onReorder,
            onDropAt = onDropAt,
            onToggleSubtask = onToggleSubtask
        )
        return
    }

    val byProject = tasks.groupBy { it.projectId }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        projects.forEach { project ->
            val items = byProject[project.id].orEmpty()
            if (items.isEmpty()) return@forEach
            val accent = Accents.color(project.colorIndex)
            Column {
                ProjectHeader(project = project, count = items.size)
                TaskGroup(
                    tasks = items,
                    onToggle = onToggle,
                    onClick = onClick,
                    showDue = showDue,
                    tint = accent,
                    onPostpone = onPostpone,
                    onReorder = onReorder,
                    onDropAt = onDropAt,
                    onToggleSubtask = onToggleSubtask
                )
            }
        }
    }
}

@Composable
private fun ProjectHeader(project: Project, count: Int) {
    val accent = Accents.color(project.colorIndex)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            tr(project.name),
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Заголовок дня: «Сегодня», «Завтра», «28 августа, пятница» или «Просрочено». */
@Composable
fun DayHeader(label: String, count: Int, overdue: Boolean = false) {
    val color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.weight(1f)
        )
        Text(
            tasksLabel(count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Список задач как единый блок — строки разделены волосяной линией. */
@Composable
fun TaskGroup(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
    showDue: Boolean = true,
    tint: androidx.compose.ui.graphics.Color? = null,
    onPostpone: ((Task) -> Unit)? = null,
    /** Долгое нажатие + перетаскивание меняет порядок задач внутри группы. */
    onReorder: ((List<String>) -> Unit)? = null,
    /** Долгое нажатие + перетаскивание отпускает задачу в точке экрана (календарь). */
    onDropAt: ((Task, Offset) -> Unit)? = null,
    /** Отметить шаг задачи в раскрытом виде. */
    onToggleSubtask: ((Task, Subtask) -> Unit)? = null
) {
    if (tasks.isEmpty()) return

    var dragIndex by remember(tasks.size) { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var startPoint by remember { mutableStateOf(Offset.Zero) }
    val rowHeights = remember(tasks.size) { mutableStateMapOf<Int, Int>() }
    val rowTops = remember(tasks.size) { mutableStateMapOf<Int, Offset>() }
    val draggable = onReorder != null || onDropAt != null

    // Куда встанет задача, если отпустить прямо сейчас.
    fun targetIndex(): Int {
        val from = dragIndex ?: return 0
        val height = (rowHeights[from] ?: 1).coerceAtLeast(1)
        val shift = (dragOffset.y / height).roundToInt()
        return (from + shift).coerceIn(0, tasks.lastIndex)
    }

    // Карточка остаётся нейтральной: цвет проекта живёт в заголовке и кружке задачи,
    // иначе на светлой теме списки превращаются в цветную кашу.
    // Строка ушла в «Выполнено» или вернулась — блок меняет высоту плавно,
    // иначе список дёргается на каждой галочке.
    GroupedCard(modifier = modifier.fillMaxWidth().animateContentSize(Motion.size)) {
        tasks.forEachIndexed { index, task ->
            key(task.id) {
                val dragging = dragIndex == index
                val from = dragIndex
                val to = if (from != null) targetIndex() else null
                // Соседи расступаются, показывая, куда встанет строка.
                val neighbourShift = when {
                    from == null || to == null || onDropAt != null -> 0f
                    from < index && index <= to -> -(rowHeights[from] ?: 0).toFloat()
                    to <= index && index < from -> (rowHeights[from] ?: 0).toFloat()
                    else -> 0f
                }
                Box(
                    Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            if (dragging) {
                                translationY = dragOffset.y
                                translationX = if (onDropAt != null) dragOffset.x else 0f
                                scaleX = 1.02f
                                scaleY = 1.02f
                                alpha = 0.95f
                            } else {
                                translationY = neighbourShift
                            }
                        }
                        .onGloballyPositioned {
                            rowHeights[index] = it.size.height
                            rowTops[index] = it.boundsInWindow().topLeft
                        }
                        .then(
                            when {
                                draggable -> Modifier.pointerInput(task.id, tasks.size) {
                                    // Долгое нажатие без движения — переименование,
                                    // с движением — прежнее перетаскивание.
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { touch ->
                                            dragIndex = index
                                            dragOffset = Offset.Zero
                                            startPoint = (rowTops[index] ?: Offset.Zero) + touch
                                        },
                                        onDrag = { change, amount ->
                                            dragOffset += amount
                                            change.consume()
                                        },
                                        onDragEnd = {
                                            val dropped = startPoint + dragOffset
                                            val newIndex = targetIndex()
                                            dragIndex = null
                                            dragOffset = Offset.Zero
                                            when {
                                                onDropAt != null -> onDropAt(task, dropped)
                                                newIndex != index -> {
                                                    val ids = tasks.map { it.id }.toMutableList()
                                                    val movedId = ids.removeAt(index)
                                                    ids.add(newIndex, movedId)
                                                    onReorder?.invoke(ids)
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            dragIndex = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                }

                                else -> Modifier
                            }
                        )
                ) {
                    SwipeableTaskRow(
                        task = task,
                        accent = tint,
                        onToggle = { onToggle(task) },
                        onClick = { onClick(task) },
                        showDue = showDue,
                        onPostpone = onPostpone,
                        onToggleSubtask = onToggleSubtask?.let { toggle ->
                            { step: Subtask -> toggle(task, step) }
                        }
                    )
                }
            }
            if (index != tasks.lastIndex) RowSeparator(startInset = 58.dp)
        }
    }

}

/**
 * Строка задачи со свайпами: вправо — выполнить, влево — перенести на день вперёд.
 * Жест собран вручную на горизонтальном перетаскивании: строка не улетает из списка,
 * а возвращается на место, выполнив действие.
 */
@Composable
private fun SwipeableTaskRow(
    task: Task,
    accent: androidx.compose.ui.graphics.Color?,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    showDue: Boolean,
    onPostpone: ((Task) -> Unit)?,
    onToggleSubtask: ((Subtask) -> Unit)? = null
) {
    if (onPostpone == null) {
        TaskRow(task, accent, onToggle, onClick, showDue, onToggleSubtask)
        return
    }
    val canPostpone = !task.done
    val scope = rememberCoroutineScope()
    // Смещение считаем синхронно: решение о действии не должно зависеть
    // от того, успела ли отработать анимация.
    var dragged by remember(task.id) { mutableFloatStateOf(0f) }

    // Насколько жест «созрел»: по нему же проявляется подложка, поэтому
    // до порога она бледная — видно, что действие ещё не сработает.
    var reach by remember(task.id) { mutableFloatStateOf(0f) }

    Box {
        if (dragged != 0f) {
            SwipeBackground(toStart = dragged > 0, done = task.done, reach = reach)
        }
        Box(
            Modifier
                .offset { IntOffset(dragged.roundToInt(), 0) }
                // Непрозрачная подложка обязательна: без неё подпись «Выполнено»
                // просвечивала сквозь строку и накладывалась на название задачи.
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .pointerInput(task.id, canPostpone) {
                    val threshold = size.width * 0.3f
                    fun release() {
                        val moved = dragged
                        scope.launch {
                            animate(
                                initialValue = moved,
                                targetValue = 0f,
                                animationSpec = Motion.settle()
                            ) { value, _ ->
                                dragged = value
                                reach = (kotlin.math.abs(value) / threshold).coerceIn(0f, 1f)
                            }
                        }
                    }
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            val next = dragged + amount
                            dragged = if (canPostpone) next else next.coerceAtLeast(0f)
                            reach = (kotlin.math.abs(dragged) / threshold).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val moved = dragged
                            when {
                                moved > threshold -> onToggle()
                                moved < -threshold && canPostpone -> onPostpone(task)
                            }
                            release()
                        },
                        onDragCancel = { release() }
                    )
                }
        ) {
            TaskRow(task, accent, onToggle, onClick, showDue, onToggleSubtask)
        }
    }
}

/** Подложка под строкой во время свайпа: слева — «выполнить», справа — «завтра». */
@Composable
private fun BoxScope.SwipeBackground(toStart: Boolean, done: Boolean, reach: Float) {
    val color = if (toStart) Accents.color(1) else Accents.color(4)
    val icon = when {
        toStart && done -> Icons.Rounded.Replay
        toStart -> Icons.Rounded.Check
        else -> Icons.Rounded.Schedule
    }
    val label = when {
        toStart && done -> tr("Вернуть")
        toStart -> tr("Выполнено")
        else -> tr("Завтра")
    }
    Row(
        Modifier
            .matchParentSize()
            .background(color.copy(alpha = 0.05f + 0.16f * reach))
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (toStart) Arrangement.Start else Arrangement.End
    ) {
        // У порога значок дорастает до полного размера — это и есть подсказка,
        // что палец можно отпускать.
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    val grow = 0.7f + 0.3f * reach
                    scaleX = grow
                    scaleY = grow
                    alpha = 0.4f + 0.6f * reach
                }
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = color.copy(alpha = 0.4f + 0.6f * reach)
        )
    }
}

@Composable
fun TaskRow(
    task: Task,
    accent: androidx.compose.ui.graphics.Color?,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    showDue: Boolean = true,
    onToggleSubtask: ((Subtask) -> Unit)? = null
) {
    val tint = accent ?: MaterialTheme.colorScheme.primary
    val titleColor by animateColorAsState(
        if (task.done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = Motion.enter(Motion.CHECK),
        label = "taskTitle"
    )
    // Задача с шагами разворачивается прямо в списке: чек-лист под названием,
    // без похода в редактор ради одной галочки.
    val canExpand = task.subtasks.isNotEmpty() && onToggleSubtask != null
    var expanded by remember(task.id) { mutableStateOf(false) }
    val chevronTurn by animateFloatAsState(
        if (expanded) 180f else 0f,
        animationSpec = Motion.settle(),
        label = "taskChevron"
    )

    // Отметка задачи раньше происходила в один кадр: галочка не успевала
    // налиться, а строка уже исчезала из списка. Теперь у нажатия три такта:
    // кружок наливается, строка держится (HOLD) — это и есть момент, ради
    // которого галочку ставят, — и только потом уезжает вбок, чуть уменьшаясь.
    // В данные отметка уходит последней, так что список закрывает уже просвет.
    var leaving by remember(task.id) { mutableStateOf(false) }
    val leave = remember(task.id) { Animatable(0f) }
    LaunchedEffect(leaving) {
        if (!leaving) {
            // Если строка осталась в этом же списке (экран проекта, «все задачи»),
            // она не должна выщёлкивать обратно — возвращается тем же движением.
            if (leave.value != 0f) leave.animateTo(0f, Motion.enter(Motion.CALM))
            return@LaunchedEffect
        }
        delay(Motion.HOLD.toLong())
        leave.animateTo(1f, Motion.exit(Motion.CALM))
        onToggle()
        leaving = false
    }

    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val gone = leave.value
                alpha = 1f - gone
                translationX = gone * 28.dp.toPx()
                val shrink = 1f - gone * 0.06f
                scaleX = shrink
                scaleY = shrink
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .animateContentSize(Motion.size)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 60.dp)
                .padding(start = 4.dp, end = if (canExpand) 4.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckCircle(
                // Пока проигрывается отметка, кружок показывает то, что человек
                // выбрал, а не то, что ещё лежит в данных.
                checked = if (leaving) !task.done else task.done,
                onClick = { if (!leaving) leaving = true },
                accent = tint,
                label = if (task.done) trf("Выполнено: %1\$s", task.title)
                else trf("Отметить выполненной: %1\$s", task.title)
            )
            Spacer(Modifier.width(4.dp))

            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 11.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null
                )

                val due = if (showDue) dueLabel(task) else dueLabel(task, withDate = false)
                // Задачу могли закрыть не в свой день — она остаётся в дне срока,
                // а когда её выполнили, говорит подписью.
                val doneLate = task.done && task.dueDate != null &&
                    task.completedDate != task.dueDate
                if (due != null || doneLate || task.note.isNotBlank() || task.subtasks.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (due != null) {
                            val (text, overdue) = due
                            LabeledRow(
                                icon = if (overdue) Icons.Rounded.EventBusy else Icons.Rounded.Schedule,
                                text = text,
                                tint = if (overdue) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (doneLate) {
                            LabeledRow(
                                icon = Icons.Rounded.DoneAll,
                                text = trf(
                                    "выполнено %1\$s",
                                    task.completedDate.relativeLabel().lowercase(RU)
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (task.subtasks.isNotEmpty()) {
                            LabeledRow(
                                icon = Icons.Rounded.Checklist,
                                text = "${task.doneSubtasks}/${task.subtasks.size}",
                                tint = if (task.doneSubtasks == task.subtasks.size) tint
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = if (!canExpand) Modifier
                                else Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { expanded = !expanded }
                            )
                        }
                        if (task.note.isNotBlank()) {
                            Text(
                                task.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (canExpand) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { expanded = !expanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) tr("Свернуть шаги") else tr("Показать шаги"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronTurn)
                    )
                }
            }

            if (task.isOverdue()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }

        if (canExpand && expanded) {
            Column(
                Modifier.padding(start = 46.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                task.subtasks.forEach { step ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleSubtask?.invoke(step) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckCircle(
                            checked = step.done,
                            onClick = { onToggleSubtask?.invoke(step) },
                            accent = tint,
                            size = 18.dp,
                            label = trf("Шаг: %1\$s", step.title)
                        )
                        Text(
                            step.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (step.done) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (step.done) TextDecoration.LineThrough else null,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** Плашка проекта для мест, где список не разбит на секции (матрица, календарь). */
@Composable
fun ProjectChip(project: Project?) {
    if (project == null) return
    val accent = Accents.color(project.colorIndex)
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            tr(project.name),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

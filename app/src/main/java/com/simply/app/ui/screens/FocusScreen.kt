package com.simply.app.ui.screens

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import com.simply.app.ui.components.HabitIcons
import com.simply.app.ui.components.SimplyCard
import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.simply.app.data.activeTasks
import com.simply.app.data.inMatrixOrder
import com.simply.app.data.relativeLabel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.FocusStats
import com.simply.app.data.PomodoroSettings
import com.simply.app.data.formatMinutes
import com.simply.app.data.sessionsLabel
import com.simply.app.data.iso
import com.simply.app.data.today
import com.simply.app.ui.AppViewModel
import com.simply.app.pomodoro.PomodoroPhase
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.ScreenHeader
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.StatTile
import com.simply.app.ui.theme.Accents
import com.simply.app.ui.theme.Motion

@Composable
fun FocusScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenProjects: () -> Unit
) {
    val state by vm.pomodoro.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }

    val accentTarget = when (state.phase) {
        PomodoroPhase.WORK -> MaterialTheme.colorScheme.primary
        else -> Accents.color(1)
    }
    // Смена фазы перекрашивала весь экран одним кадром — переливаем цвет.
    val accent by animateColorAsState(accentTarget, Motion.enter(Motion.CALM), label = "focusAccent")

    // Экран не гаснет, пока идёт отсчёт — иначе таймер бесполезен.
    val view = LocalView.current
    val keepOn = data.pomodoro.keepScreenOn && state.running
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    val linkedTask = if (!state.linkedIsHabit) data.tasks.firstOrNull { it.id == state.linkedId } else null
    val linkedHabit = if (state.linkedIsHabit) data.habits.firstOrNull { it.id == state.linkedId } else null
    val stats = remember(data.sessions) { FocusStats(data.sessions) }
    val todayMinutes = stats.minutesOn(today())
    val weekMinutes = remember(stats) { stats.minutesInRange(today().minusDays(6), today()) }

    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ScreenHeader(
                title = tr("Фокус"),
                subtitle = trf(
                    "Помодоро: %1\$d мин работы, %2\$d мин отдыха",
                    data.pomodoro.workMinutes,
                    data.pomodoro.shortBreakMinutes
                ),
                trailing = {
                    SoftIconButton(Icons.Rounded.FolderOpen, tr("Проекты"), onOpenProjects)
                    SoftIconButton(Icons.Rounded.BarChart, tr("Статистика"), onOpenStats)
                    SoftIconButton(Icons.Rounded.Tune, tr("Настройки таймера"), onOpenSettings)
                }
            )
        }

        item {
            Box(
                Modifier
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                TimerRing(progress = state.progress, accent = accent, running = state.running)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(
                        targetState = state.phase,
                        transitionSpec = {
                            (fadeIn(Motion.enter()) + slideInVertically(Motion.enter()) { it / 3 })
                                .togetherWith(
                                    fadeOut(Motion.exit(Motion.QUICK)) +
                                        slideOutVertically(Motion.exit()) { -it / 3 }
                                )
                        },
                        label = "phase"
                    ) { phase ->
                        Text(
                            tr(phase.title).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.timeLabel,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(data.pomodoro.cyclesBeforeLongBreak) { i ->
                            Box(
                                Modifier
                                    .size(if (i < state.completedInCycle) 9.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i < state.completedInCycle) accent
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { vm.stopTimer() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = tr("Закончить"))
                }
                Button(
                    onClick = { vm.toggleTimer() },
                    modifier = Modifier
                        .height(60.dp)
                        .widthIn(min = 168.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(
                        if (state.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (state.running) tr("Пауза") else tr("Старт"),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                FilledTonalIconButton(
                    onClick = { vm.skipPhase() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = tr("Пропустить"))
                }
            }
        }

        item {
            SimplyCard(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                onClick = onOpenStats,
                contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SummaryValue(
                        value = formatMinutes(todayMinutes),
                        label = trf("сегодня · %1\$s", sessionsLabel(stats.sessionsOn(today()))),
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryValue(
                        value = formatMinutes(weekMinutes),
                        label = tr("за 7 дней"),
                        accent = Accents.color(1),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = tr("Открыть статистику"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(tr("Над чем работаем"), Modifier.weight(1f))
                    TextButton(onClick = { showManual = true }) {
                        Text(tr("Записать вручную"), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        item {
            Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                when {
                    linkedTask != null -> LinkedTargetCard(
                        title = linkedTask.title,
                        subtitle = data.projects.firstOrNull { it.id == linkedTask.projectId }
                            ?.name?.let { tr(it) } ?: tr("Задача"),
                        badge = null,
                        accent = Accents.color(
                            data.projects.firstOrNull { it.id == linkedTask.projectId }?.colorIndex ?: 0
                        ),
                        onClear = { vm.linkTarget(null) },
                        onClick = { showPicker = true }
                    )

                    linkedHabit != null -> LinkedTargetCard(
                        title = linkedHabit.name,
                        subtitle = tr("Привычка"),
                        badge = HabitIcons.vector(linkedHabit.icon),
                        accent = Accents.color(linkedHabit.colorIndex),
                        onClear = { vm.linkTarget(null) },
                        onClick = { showPicker = true }
                    )

                    else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterPill(tr("Без задачи"), true, { })
                        FilterPill(
                            tr("Выбрать…"),
                            false,
                            { showPicker = true }
                        )
                    }
                }
            }
        }
    }


    if (showManual) {
        SessionEditorSheet(
            data = data,
            initial = null,
            defaultTargetId = state.linkedId,
            defaultTargetIsHabit = state.linkedIsHabit,
            onDismiss = { showManual = false },
            onSave = { minutes, date, targetId, isHabit ->
                vm.saveSession(null, minutes, date, targetId, isHabit)
                showManual = false
            },
            onDelete = null
        )
    }

    if (showPicker) {
        FocusTargetSheet(
            data = data,
            selectedId = state.linkedId,
            onPick = { id, isHabit -> vm.linkTarget(id, isHabit); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun SummaryValue(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TimerRing(progress: Float, accent: Color, running: Boolean) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (running) 400 else 250),
        label = "ringProgress"
    )
    val dial = MaterialTheme.colorScheme.surfaceContainerLow
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)

    Canvas(Modifier.fillMaxSize()) {
        val stroke = size.minDimension * 0.055f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)

        // «циферблат» — мягкая светлая подложка под кольцом
        drawCircle(color = dial, radius = size.minDimension / 2f - stroke * 1.05f)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        if (animated > 0.001f) {
            drawArc(
                brush = Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.75f), accent)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

/** Текущая цель сессии: задача или привычка, с кнопкой сброса. */
@Composable
private fun LinkedTargetCard(
    title: String,
    subtitle: String,
    badge: ImageVector?,
    accent: Color,
    onClear: () -> Unit,
    onClick: () -> Unit
) {
    SimplyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    badge ?: Icons.Rounded.CheckCircleOutline,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = tr("Убрать"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Поиск по невыполненным задачам и привычкам. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusTargetSheet(
    data: AppData,
    selectedId: String?,
    onPick: (String?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val q = query.trim().lowercase()
    val tasks = remember(data.tasks, data.projects, q) {
        data.activeTasks()
            .filterNot { it.done }
            .filter { q.isEmpty() || it.title.lowercase().contains(q) }
            .inMatrixOrder()
            .take(30)
    }
    val habits = remember(data.habits, q) {
        data.habits.filter { q.isEmpty() || it.name.lowercase().contains(q) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(tr("Над чем работаем"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(tr("Поиск задачи или привычки")) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = tr("Очистить"))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    PickerRow(
                        title = tr("Без задачи"),
                        subtitle = tr("Просто отсчёт времени"),
                        badge = Icons.Rounded.HourglassEmpty,
                        accent = MaterialTheme.colorScheme.onSurfaceVariant,
                        selected = selectedId == null,
                        onClick = { onPick(null, false) }
                    )
                }

                if (tasks.isNotEmpty()) {
                    item { SectionTitle(tr("Задачи")) }
                    items(tasks, key = { "t" + it.id }) { task ->
                        val project = data.projects.firstOrNull { it.id == task.projectId }
                        PickerRow(
                            title = task.title,
                            subtitle = listOfNotNull(
                                project?.name?.let { tr(it) },
                                dueLabel(task)?.first
                            ).joinToString(" · "),
                            badge = null,
                            accent = Accents.color(project?.colorIndex ?: 0),
                            selected = selectedId == task.id,
                            onClick = { onPick(task.id, false) }
                        )
                    }
                }

                if (habits.isNotEmpty()) {
                    item { SectionTitle(tr("Привычки")) }
                    items(habits, key = { "h" + it.id }) { habit ->
                        PickerRow(
                            title = habit.name,
                            subtitle = trf("Цель: %1\$d раз в неделю", habit.targetPerWeek),
                            badge = HabitIcons.vector(habit.icon),
                            accent = Accents.color(habit.colorIndex),
                            selected = selectedId == habit.id,
                            onClick = { onPick(habit.id, true) }
                        )
                    }
                }

                if (tasks.isEmpty() && habits.isEmpty()) {
                    item {
                        Text(
                            tr("Ничего не нашлось"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    badge: ImageVector?,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            if (badge != null) {
                Icon(
                    badge,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

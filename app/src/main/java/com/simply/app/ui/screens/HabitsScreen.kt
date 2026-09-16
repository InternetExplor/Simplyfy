package com.simply.app.ui.screens

import com.simply.app.ui.components.dialogEnter
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Button
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import com.simply.app.ui.components.IconPickerGrid
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.Habit
import com.simply.app.data.HabitGroup
import com.simply.app.data.Project
import com.simply.app.data.MAX_DAILY_TARGET
import com.simply.app.data.daysLabel
import com.simply.app.data.dayMonthLabel
import com.simply.app.data.iso
import com.simply.app.data.startOfWeek
import com.simply.app.data.streak
import com.simply.app.data.bestStreak
import com.simply.app.data.today
import com.simply.app.data.weekdayShort
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.ScreenHeader
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.HabitIcons
import com.simply.app.ui.components.ProjectIcons
import com.simply.app.ui.components.ProjectPicker
import com.simply.app.ui.components.RenameDialog
import com.simply.app.ui.components.SimplyCard
import com.simply.app.ui.components.contrastOn
import com.simply.app.ui.theme.Accents
import java.time.LocalDate
import kotlin.math.roundToInt
import com.simply.app.ui.theme.Motion

@Composable
fun HabitsScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    addRequested: Boolean,
    onAddHandled: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProjects: () -> Unit
) {
    var weekOffset by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var creating by remember { mutableStateOf(false) }
    var renamingGroup by remember { mutableStateOf<HabitGroup?>(null) }

    LaunchedEffect(addRequested) {
        if (addRequested) {
            creating = true
            onAddHandled()
        }
    }

    val weekStart = remember(weekOffset) { today().startOfWeek().plusWeeks(weekOffset.toLong()) }
    val week = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }
    val doneToday = data.habits.count { it.isDoneOn(today()) }

    // Группа, у которой не осталось привычек, с экрана исчезает сама —
    // пустой заголовок ничего не сообщает.
    val grouped = remember(data.habits) { data.habits.filter { it.groupId != null }
        .groupBy { it.groupId!! } }
    val ungrouped = remember(data.habits) { data.habits.filter { it.groupId == null } }
    val groups = remember(data.habitGroups) { data.habitGroups.sortedBy { it.order } }
    fun projectOf(habit: Habit) = data.projects.firstOrNull { it.id == habit.projectId }

    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ScreenHeader(
                title = tr("Привычки"),
                subtitle = if (data.habits.isEmpty()) tr("Добавь первую привычку")
                else trf("Сегодня %1\$d из %2\$d", doneToday, data.habits.size),
                trailing = {
                    SoftIconButton(Icons.Rounded.FolderOpen, tr("Проекты"), onOpenProjects)
                    SoftIconButton(Icons.Rounded.Settings, tr("Настройки"), onOpenSettings)
                }
            )
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { weekOffset-- }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = tr("Предыдущая неделя"))
                }
                Text(
                    text = "${week.first().dayMonthLabel()} — ${week.last().dayMonthLabel()}",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { if (weekOffset < 0) weekOffset++ },
                    enabled = weekOffset < 0
                ) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = tr("Следующая неделя"))
                }
            }
        }

        if (data.habits.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Spa,
                    title = tr("Пока нет привычек"),
                    subtitle = tr("Начни с одной-двух: вода, чтение, зарядка. Отмечай кружок каждый день.")
                )
            }
        }

        // Привычки вне групп идут первыми: это то, что человек ведёт поштучно.
        // Всё сложенное в группы — ниже, и каждая группа сворачивается в одну
        // строку, иначе на двух десятках привычек экран превращается в ленту.
        items(ungrouped, key = { it.id }) { habit ->
            Box(Modifier.padding(horizontal = 16.dp)) {
                HabitCard(
                    habit = habit,
                    project = projectOf(habit),
                    week = week,
                    onToggle = { date -> vm.toggleHabit(habit.id, date) },
                    onSetCount = { date, value -> vm.setHabitCount(habit.id, date, value) },
                    onSkip = { date -> vm.toggleSkip(habit.id, date) },
                    onEdit = { editing = habit }
                )
            }
        }

        groups.forEach { group ->
            val inGroup = grouped[group.id].orEmpty()
            if (inGroup.isEmpty()) return@forEach
            item(key = "gh-" + group.id) {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    HabitGroupHeader(
                        group = group,
                        habits = inGroup,
                        onToggle = { vm.toggleHabitGroup(group.id) },
                        onRename = { renamingGroup = group }
                    )
                }
            }
            if (!group.collapsed) {
                items(inGroup, key = { it.id }) { habit ->
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        HabitCard(
                            habit = habit,
                            project = projectOf(habit),
                            week = week,
                            onToggle = { date -> vm.toggleHabit(habit.id, date) },
                            onSetCount = { date, value -> vm.setHabitCount(habit.id, date, value) },
                            onSkip = { date -> vm.toggleSkip(habit.id, date) },
                            onEdit = { editing = habit }
                        )
                    }
                }
            }
        }

        if (data.habits.isNotEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SectionTitle(tr("Отмечено за неделю"),
                        trailing = data.habits.sumOf { h -> week.count { h.isDoneOn(it) } }.toString()
                    )
                }
            }
        }
    }

    if (creating) {
        HabitEditorSheet(
            initial = null,
            data = data,
            onDismiss = { creating = false },
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
                creating = false
            },
            onDelete = null,
            onCreateGroup = { vm.addHabitGroup(it) }
        )
    }

    editing?.let { habit ->
        HabitEditorSheet(
            initial = habit,
            data = data,
            onDismiss = { editing = null },
            onSave = { vm.updateHabit(it); editing = null },
            onDelete = { vm.deleteHabit(habit.id); editing = null },
            onCreateGroup = { vm.addHabitGroup(it) }
        )
    }

    renamingGroup?.let { group ->
        RenameDialog(
            title = tr("Группа"),
            initial = group.name,
            onDismiss = { renamingGroup = null },
            onConfirm = { vm.renameHabitGroup(group.id, it); renamingGroup = null },
            onDelete = { vm.deleteHabitGroup(group.id); renamingGroup = null }
        )
    }
}

/**
 * Шапка группы привычек: название, сколько отмечено сегодня и шеврон.
 * Свёрнутая группа — одна строка вместо пяти карточек; это единственное,
 * ради чего группы и заведены.
 */
@Composable
private fun HabitGroupHeader(
    group: HabitGroup,
    habits: List<Habit>,
    onToggle: () -> Unit,
    onRename: () -> Unit
) {
    val accent = Accents.color(group.colorIndex)
    val done = habits.count { it.isDoneOn(today()) }
    val turn by animateFloatAsState(
        if (group.collapsed) 0f else 180f,
        animationSpec = Motion.settle(),
        label = "groupChevron"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            group.name,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            trf("%1\$d из %2\$d", done, habits.size),
            style = MaterialTheme.typography.labelMedium,
            color = if (done == habits.size) accent
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRename) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = tr("Изменить группу"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = if (group.collapsed) tr("Развернуть") else tr("Свернуть"),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .rotate(turn)
        )
    }
}

/** Плашка проекта на карточке привычки — та же, что у задач. */
@Composable
private fun HabitProjectChip(project: Project) {
    val accent = Accents.color(project.colorIndex)
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            ProjectIcons.vector(project.icon),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(11.dp)
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

@Composable
private fun HabitCard(
    habit: Habit,
    project: Project?,
    week: List<LocalDate>,
    onToggle: (LocalDate) -> Unit,
    onSetCount: (LocalDate, Int) -> Unit,
    onSkip: (LocalDate) -> Unit,
    onEdit: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    // Одно деление колеса — столько же пути пальца, сколько занимает сам кружок.
    val stepPx = with(LocalDensity.current) { 28.dp.toPx() }
    var expanded by remember { mutableStateOf(false) }
    // День, который сейчас крутят колесом, и значение на момент начала жеста:
    // протяжку считаем от него, а не от текущего, иначе число убегало бы само.
    var counting by remember(habit.id) { mutableStateOf<LocalDate?>(null) }
    var countAtStart by remember(habit.id) { mutableIntStateOf(0) }
    val accent = Accents.color(habit.colorIndex)
    val doneThisWeek = week.count { habit.isDoneOn(it) }
    // Пролистали неделю — закрываем колесо: оно показывало бы день,
    // кружка которого на экране больше нет.
    LaunchedEffect(week) { if (counting !in week) counting = null }
    val streakDays = habit.streak()
    val todayCount = habit.countOn(today())

    SimplyCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(Motion.size),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.18f))
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        HabitIcons.vector(habit.icon),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).clickable { expanded = !expanded }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (project != null) HabitProjectChip(project)
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (streakDays > 0) {
                            Icon(
                                Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                daysLabel(streakDays),
                                style = MaterialTheme.typography.bodySmall,
                                color = accent
                            )
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            trf("%1\$d / %2\$d за неделю", doneThisWeek, habit.targetPerWeek),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // У привычки со счётчиком показываем ещё и сегодняшний прогресс.
                        if (habit.perDay > 1) {
                            Text(
                                "\u2022",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                trf("%1\$d из %2\$d сегодня", todayCount, habit.perDay),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (todayCount >= habit.perDay) accent
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { date ->
                    DayDot(
                        date = date,
                        count = habit.countOn(date),
                        target = habit.perDay,
                        skipped = habit.isSkippedOn(date),
                        accent = accent,
                        enabled = !date.isAfter(today()),
                        counting = counting == date,
                        onClick = { onToggle(date) },
                        onCountStart = {
                            counting = date
                            countAtStart = habit.countOn(date)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onCountDrag = { shift ->
                            val steps = (shift / stepPx).roundToInt()
                            val next = (countAtStart + steps).coerceIn(0, MAX_DAILY_TARGET)
                            if (next != habit.countOn(date)) {
                                onSetCount(date, next)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onCountEnd = {}
                    )
                }
            }

            // Полоса счёта живёт под строкой дней, а не всплывашкой над кружком:
            // палец, который держит день, иначе закрывал бы собой число.
            counting?.let { date ->
                Spacer(Modifier.height(12.dp))
                HabitCountWheel(
                    date = date,
                    count = habit.countOn(date),
                    target = habit.perDay,
                    skipped = habit.isSkippedOn(date),
                    accent = accent,
                    onSet = { onSetCount(date, it) },
                    onSkip = { onSkip(date); counting = null },
                    onClose = { counting = null }
                )
            }

            // Раскрытая карточка: месяц целиком и итоги по привычке.
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                MonthStrip(habit = habit, accent = accent)
                Spacer(Modifier.height(12.dp))
                HabitTotals(habit = habit, accent = accent)
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Изменить привычку"))
                }
            }
        }
    }
}

/**
 * Колесо счёта одного дня: лента чисел едет под неподвижной меткой.
 *
 * Появляется по удержанию кружка дня и остаётся после того, как палец убрали, —
 * так промах не заставляет начинать жест заново. Крутить можно и здесь: полоса
 * ловит горизонтальную протяжку тем же шагом, что и сам кружок.
 */
@Composable
private fun HabitCountWheel(
    date: LocalDate,
    count: Int,
    target: Int,
    skipped: Boolean,
    accent: Color,
    onSet: (Int) -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val stepPx = with(LocalDensity.current) { 28.dp.toPx() }
    // Дробный остаток протяжки: без него медленное движение не набирало бы шаг.
    var carry by remember(date) { mutableFloatStateOf(0f) }

    fun shift(by: Int) {
        val next = (count + by).coerceIn(0, MAX_DAILY_TARGET)
        if (next == count) return
        onSet(next)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                date.dayMonthLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                trf("цель %1\$d", target),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = tr("Закрыть"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            WheelButton(Icons.Rounded.Remove, tr("Меньше"), accent) { shift(-1) }

            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .pointerInput(date) {
                        detectHorizontalDragGestures { change, amount ->
                            change.consume()
                            carry += amount
                            val steps = (carry / stepPx).toInt()
                            if (steps != 0) {
                                carry -= steps * stepPx
                                shift(steps)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                WheelStrip(count = count, target = target, accent = accent)
            }

            WheelButton(Icons.Rounded.Add, tr("Больше"), accent) { shift(1) }
        }

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (skipped) tr("Вернуть день") else tr("Пропустить день"))
        }
    }
}

/** Лента чисел вокруг текущего: соседи гаснут и уменьшаются к краям. */
@Composable
private fun WheelStrip(count: Int, target: Int, accent: Color) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (-3..3).forEach { offset ->
                val value = count + offset
                val here = offset == 0
                val fade = 1f - kotlin.math.abs(offset) * 0.28f
                Box(
                    Modifier.width(if (here) 46.dp else 34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (value in 0..MAX_DAILY_TARGET) {
                        Text(
                            value.toString(),
                            style = if (here) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (here) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                here -> accent
                                // Цель отмечена цветом: видно, где проходит норма.
                                value == target -> accent.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = fade.coerceAtLeast(0.15f))
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
        // Неподвижная метка под текущим числом — по ней и читают значение.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .width(30.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(accent)
        )
    }
}

@Composable
private fun WheelButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
    }
}

/** Последние четыре недели: видно, где привычка проседает. */
@Composable
private fun MonthStrip(habit: Habit, accent: Color) {
    val today = today()
    val start = today.minusDays(27)
    val days = (0..27).map { start.plusDays(it.toLong()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            tr("Последние 4 недели"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        days.chunked(7).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { date ->
                    val done = habit.isDoneOn(date)
                    val part = habit.countOn(date) > 0 && !done
                    val skipped = habit.isSkippedOn(date)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                when {
                                    done -> accent
                                    part -> accent.copy(alpha = 0.35f)
                                    skipped -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Прочерк только у пустого дня: набранное число важнее
                        // пометки о пропуске и не должно ею закрываться.
                        if (skipped && habit.countOn(date) == 0) {
                            Text(
                                "\u2014",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Итоги привычки: месяц, серии, процент. */
@Composable
private fun HabitTotals(habit: Habit, accent: Color) {
    val today = today()
    val monthStart = today.withDayOfMonth(1)
    val daysInMonth = (0 until (today.dayOfMonth)).map { monthStart.plusDays(it.toLong()) }
    val doneThisMonth = daysInMonth.count { habit.isDoneOn(it) }
    val skippedThisMonth = daysInMonth.count { habit.isSkippedOn(it) }
    val counted = (daysInMonth.size - skippedThisMonth).coerceAtLeast(1)
    val percent = doneThisMonth * 100 / counted

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HabitTotal(tr("в этом месяце"), "$doneThisMonth", accent, Modifier.weight(1f))
        HabitTotal(tr("лучшая серия"), habit.bestStreak().toString(), accent, Modifier.weight(1f))
        HabitTotal(tr("выполнение"), "$percent%", accent, Modifier.weight(1f))
    }
}

@Composable
private fun HabitTotal(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Кружок дня. У привычки с целью 1 — обычная галочка, у привычки со счётчиком
 * кольцо показывает, сколько из дневной цели уже набрано.
 */
@Composable
private fun DayDot(
    date: LocalDate,
    count: Int,
    target: Int,
    skipped: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    /** Удержание: открыть счёт этого дня. */
    onCountStart: () -> Unit,
    /** Протяжка после удержания, в пикселях от точки нажатия. */
    onCountDrag: (Float) -> Unit,
    onCountEnd: () -> Unit,
    /** День, который сейчас крутят колесом, — подсвечиваем его кружок. */
    counting: Boolean = false
) {
    val checked = count >= target
    // Цель перешагнули — показываем набранное число, а не галочку:
    // иначе «12 из 8» выглядело бы ровно так же, как «8 из 8».
    val over = target > 1 && count > target
    val scale by animateFloatAsState(
        if (checked) 1f else 0f,
        animationSpec = Motion.settle(),
        label = "dayDot"
    )
    val fraction by animateFloatAsState(
        (count.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f),
        label = "dayDotFraction"
    )
    val isToday = date == today()
    val ringColor = accent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(40.dp)
            .alpha(if (enabled) 1f else 0.35f)
    ) {
        Text(
            date.weekdayShort(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        val spoken = when {
            skipped -> trf("%1\$s, пропущен", date.dayMonthLabel())
            target > 1 -> trf("%1\$s, %2\$d из %3\$d", date.dayMonthLabel(), count, target)
            checked -> trf("%1\$s, выполнено", date.dayMonthLabel())
            else -> trf("%1\$s, не отмечено", date.dayMonthLabel())
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .semantics { contentDescription = spoken }
                .then(
                    if (!enabled) Modifier
                    else Modifier
                        // Порядок важен: детектор удержания стоит первым, иначе
                        // обычное касание успевало бы съесть жест. Ровно так же
                        // собраны строки задач с перетаскиванием.
                        .pointerInput(date) {
                            var moved = 0f
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    moved = 0f
                                    onCountStart()
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    moved += amount.x
                                    onCountDrag(moved)
                                },
                                onDragEnd = { onCountEnd() },
                                onDragCancel = { onCountEnd() }
                            )
                        }
                        .clickable(onClick = onClick)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Незавершённый день со счётчиком: кольцо по кругу вместо заливки.
            if (!checked && count > 0) {
                Canvas(Modifier.size(38.dp)) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) accent
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .then(
                        when {
                            counting -> Modifier.border(2.dp, accent, CircleShape)
                            isToday && !checked -> Modifier.border(2.dp, accent, CircleShape)
                            else -> Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // Прочерк — только у по-настоящему пустого пропущенного дня:
                    // раньше он стоял первым и закрывал набранное число.
                    skipped && count == 0 -> Text(
                        "\u2014",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    over -> Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contrastOn(accent)
                    )

                    checked -> Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = contrastOn(accent),
                        modifier = Modifier.size(18.dp).scale(scale)
                    )

                    count > 0 -> Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )

                    else -> Text(
                        date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HabitEditorSheet(
    initial: Habit?,
    data: AppData,
    onDismiss: () -> Unit,
    /** Отдаём целую привычку: полей стало слишком много для позиционного списка. */
    onSave: (Habit) -> Unit,
    onDelete: (() -> Unit)?,
    /** Заводит группу на месте и возвращает её id. */
    onCreateGroup: (String) -> String? = { null },
    /** Проект, из которого открыли редактор — подставляется новой привычке. */
    defaultProjectId: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: "spark") }
    var color by remember { mutableIntStateOf(initial?.colorIndex ?: 0) }
    var target by remember { mutableIntStateOf(initial?.targetPerWeek ?: 7) }
    var perDay by remember { mutableIntStateOf(initial?.perDay ?: 1) }
    var showPerDayInput by remember { mutableStateOf(false) }
    var reminder by remember { mutableStateOf(initial?.reminderTime) }
    var showTimeInput by remember { mutableStateOf(false) }
    var projectId by remember { mutableStateOf(initial?.projectId ?: defaultProjectId) }
    var groupId by remember { mutableStateOf(initial?.groupId) }
    var newGroup by remember { mutableStateOf(false) }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        // Содержимое длиннее экрана — без прокрутки кнопка «Сохранить» уезжает за край.
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                if (initial == null) tr("Новая привычка") else tr("Привычка"),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tr("Название")) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle(tr("Значок"))
            IconPickerGrid(
                keys = HabitIcons.keys,
                common = HabitIcons.common,
                selected = icon,
                accent = Accents.color(color),
                vectorFor = { HabitIcons.vector(it) },
                onPick = { icon = it }
            )

            SectionTitle(tr("Цвет"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(Accents.size) { i ->
                    ColorDot(Accents.color(i), color == i) { color = i }
                }
            }

            SectionTitle(
                tr("Цель на день"),
                trailing = if (perDay == 1) tr("Просто отметка") else trf("%1\$d раз", perDay)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(perDayPresets) { n ->
                    FilterPill(
                        if (n == 1) tr("Просто отметка") else "$n",
                        perDay == n,
                        { perDay = n },
                        accent = Accents.color(color)
                    )
                }
                // Заготовок хватает не всем: сюда можно ввести любое число.
                item {
                    val custom = perDay !in perDayPresets
                    FilterPill(
                        if (custom) perDay.toString() else tr("Своё…"),
                        custom,
                        { showPerDayInput = true },
                        accent = Accents.color(color)
                    )
                }
            }

            SectionTitle(
                tr("Напоминание"),
                trailing = reminder ?: tr("Нет")
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterPill(
                        tr("Нет"),
                        reminder == null,
                        { reminder = null },
                        accent = Accents.color(color)
                    )
                }
                items(reminderPresets) { time ->
                    FilterPill(
                        time,
                        reminder == time,
                        { reminder = time },
                        accent = Accents.color(color)
                    )
                }
                item {
                    val custom = reminder != null && reminder !in reminderPresets
                    FilterPill(
                        if (custom) reminder!! else tr("Время…"),
                        custom,
                        { showTimeInput = true },
                        accent = Accents.color(color)
                    )
                }
            }

            // Привычка может принадлежать проекту — тогда она видна на его экране
            // и помечена его цветом. Группа же ничего не значит, кроме порядка
            // на этом экране: ею просто складывают длинный список в стопки.
            SectionTitle(tr("Проект"))
            ProjectPicker(
                projects = data.projects.filter { it.isActive },
                selectedId = projectId,
                onSelect = { projectId = it },
                noneLabel = tr("Без проекта")
            )

            SectionTitle(tr("Группа"))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterPill(tr("Без группы"), groupId == null, { groupId = null })
                }
                items(data.habitGroups.sortedBy { it.order }) { group ->
                    FilterPill(
                        group.name,
                        groupId == group.id,
                        { groupId = group.id },
                        accent = Accents.color(group.colorIndex)
                    )
                }
                item { FilterPill(tr("＋ Группа"), false, { newGroup = true }) }
            }

            SectionTitle(tr("Цель"), trailing = trf("%1\$d в неделю", target))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items((1..7).toList()) { n ->
                    FilterPill(
                        n.toString(),
                        target == n,
                        { target = n },
                        accent = Accents.color(color)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    onSave(
                        (initial ?: Habit(name = name)).copy(
                            name = name.trim(),
                            icon = icon.ifBlank { "spark" },
                            colorIndex = color,
                            targetPerWeek = target,
                            dailyTarget = perDay,
                            reminderTime = reminder,
                            projectId = projectId,
                            groupId = groupId
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (initial == null) tr("Добавить") else tr("Сохранить"))
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Удалить привычку"), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (newGroup) {
        TextPromptDialog(
            title = tr("Новая группа"),
            placeholder = tr("Название"),
            onDismiss = { newGroup = false },
            onConfirm = { title ->
                onCreateGroup(title)?.let { groupId = it }
                newGroup = false
            }
        )
    }

    if (showTimeInput) {
        val state = rememberTimePickerState(
            initialHour = reminder?.substringBefore(':')?.toIntOrNull() ?: 9,
            initialMinute = reminder?.substringAfter(':')?.toIntOrNull() ?: 0,
            is24Hour = true
        )
        AlertDialog(
            modifier = Modifier.dialogEnter(),
            onDismissRequest = { showTimeInput = false },
            confirmButton = {
                TextButton(onClick = {
                    reminder = "%02d:%02d".format(state.hour, state.minute)
                    showTimeInput = false
                }) { Text(tr("Готово")) }
            },
            dismissButton = {
                TextButton(onClick = { showTimeInput = false }) { Text(tr("Отмена")) }
            },
            title = { Text(tr("Напоминание")) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }

    if (showPerDayInput) {
        NumberPromptDialog(
            title = tr("Цель на день"),
            hint = tr("Сколько раз в день"),
            initial = perDay,
            onDismiss = { showPerDayInput = false },
            onConfirm = { perDay = it; showPerDayInput = false }
        )
    }
}

/** Ввод произвольного числа: для целей, которых нет среди заготовок. */
@Composable
private fun NumberPromptDialog(
    title: String,
    hint: String,
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initial.toString()) }
    val value = text.trim().toIntOrNull()
    AlertDialog(
        modifier = Modifier.dialogEnter(),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> text = input.filter { it.isDigit() }.take(3) },
                placeholder = { Text(hint) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let { onConfirm(it.coerceIn(1, MAX_DAILY_TARGET)) } },
                enabled = value != null && value >= 1
            ) { Text(tr("Готово")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена")) } },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

// Заготовок нарочно мало: длинный ряд подсказок читался как список настроек,
// а любое другое число вводится в «Своё…».
private val perDayPresets = listOf(1, 2, 3, 5, 10)
private val reminderPresets = listOf("08:00", "12:00", "21:00")

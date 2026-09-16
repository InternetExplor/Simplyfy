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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.FocusSession
import com.simply.app.data.dayHeaderLabel
import com.simply.app.data.formatMinutes
import com.simply.app.data.iso
import com.simply.app.data.today
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.GroupedCard
import com.simply.app.ui.components.RowSeparator
import com.simply.app.ui.components.ScreenHeader
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.dialogEnter
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import com.simply.app.ui.theme.Accents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val hhmm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * История фокуса: все записанные отрезки по дням. Любой можно поправить или
 * удалить — таймер иногда забывают выключить, а иногда не включают вовсе.
 */
@Composable
fun FocusHistoryScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    // null — редактора нет, Optional.empty-подобная обёртка не нужна:
    // «создаём новую» отличаем отдельным флагом.
    var editing by remember { mutableStateOf<FocusSession?>(null) }
    var creating by remember { mutableStateOf(false) }

    val byDay = remember(data.sessions) {
        data.sessions
            .sortedByDescending { it.startedAt }
            .groupBy { it.date }
            .toList()
            .sortedByDescending { it.first }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            )
        ) {
            item {
                ScreenHeader(
                    title = tr("История фокуса"),
                    subtitle = trf(
                        "Всего %1\$s",
                        formatMinutes(data.sessions.sumOf { it.minutes })
                    ),
                    trailing = {
                        SoftIconButton(Icons.Rounded.Add, tr("Добавить запись")) {
                            creating = true
                        }
                        SoftIconButton(Icons.Rounded.ArrowBack, tr("Назад"), onBack)
                    }
                )
            }

            if (byDay.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.HistoryToggleOff,
                        title = tr("Записей пока нет"),
                        subtitle = tr("Отрезки появятся здесь сами, а забытое можно добавить вручную.")
                    )
                }
            }

            byDay.forEach { (day, sessions) ->
                val date = runCatching { LocalDate.parse(day) }.getOrNull()
                item(key = "h-$day") {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SectionTitle(
                            date?.dayHeaderLabel() ?: day,
                            trailing = formatMinutes(sessions.sumOf { it.minutes })
                        )
                    }
                }
                item(key = "g-$day") {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        GroupedCard(Modifier.fillMaxWidth()) {
                            sessions.forEachIndexed { index, session ->
                                SessionRow(session) { editing = session }
                                if (index != sessions.lastIndex) {
                                    RowSeparator(startInset = 58.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        SessionEditorSheet(
            data = data,
            initial = null,
            onDismiss = { creating = false },
            onSave = { minutes, date, targetId, isHabit ->
                vm.saveSession(null, minutes, date, targetId, isHabit)
                creating = false
            },
            onDelete = null
        )
    }

    editing?.let { session ->
        SessionEditorSheet(
            data = data,
            initial = session,
            onDismiss = { editing = null },
            onSave = { minutes, date, targetId, isHabit ->
                vm.saveSession(session.id, minutes, date, targetId, isHabit)
                editing = null
            },
            onDelete = { vm.deleteSession(session.id); editing = null }
        )
    }
}

@Composable
private fun SessionRow(session: FocusSession, onClick: () -> Unit) {
    val accent = if (session.targetIsHabit) Accents.color(1) else MaterialTheme.colorScheme.primary
    val startedAt = remember(session.startedAt) {
        Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault()).format(hhmm)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Timer,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session.targetTitle.ifBlank { tr("Без задачи") },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (session.completed) startedAt else trf("%1\$s · прервано", startedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            formatMinutes(session.minutes),
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
    }
}

/**
 * Один и тот же редактор для новой записи и для правки существующей:
 * минуты, день и цель. Дата выбирается календарём — «задним числом»
 * не ограничено последними днями.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditorSheet(
    data: AppData,
    initial: FocusSession?,
    onDismiss: () -> Unit,
    onSave: (Int, LocalDate, String?, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    defaultTargetId: String? = null,
    defaultTargetIsHabit: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var minutes by remember { mutableIntStateOf(initial?.minutes ?: 25) }
    var date by remember {
        mutableStateOf(
            initial?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today()
        )
    }
    var targetId by remember { mutableStateOf(initial?.targetId ?: defaultTargetId) }
    var targetIsHabit by remember {
        mutableStateOf(initial?.targetIsHabit ?: defaultTargetIsHabit)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMinutesInput by remember { mutableStateOf(false) }
    val presets = listOf(10, 15, 25, 30, 45, 60, 90)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                if (initial == null) tr("Записать вручную") else tr("Запись фокуса"),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (initial == null) tr("Время, которое таймер не застал")
                else tr("Поправь минуты, день или задачу"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionTitle(tr("Сколько минут"), trailing = formatMinutes(minutes))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { n ->
                    FilterPill("$n", minutes == n, { minutes = n })
                }
                item {
                    val custom = minutes !in presets
                    FilterPill(
                        if (custom) minutes.toString() else tr("Своё…"),
                        custom,
                        { showMinutesInput = true }
                    )
                }
            }

            SectionTitle(tr("Когда"), trailing = date.dayHeaderLabel())
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(0, 1, 2, 3)) { offset ->
                    val day = today().minusDays(offset.toLong())
                    FilterPill(day.dayHeaderLabel(), date == day, { date = day })
                }
                item {
                    FilterPill(
                        tr("Дата…"),
                        date < today().minusDays(3),
                        { showDatePicker = true }
                    )
                }
            }

            SectionTitle(tr("Над чем работаем"))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterPill(tr("Без задачи"), targetId == null, {
                        targetId = null
                        targetIsHabit = false
                    })
                }
                items(data.tasks.filterNot { it.done }.take(20), key = { "t" + it.id }) { task ->
                    FilterPill(task.title, targetId == task.id && !targetIsHabit, {
                        targetId = task.id
                        targetIsHabit = false
                    })
                }
                items(data.habits, key = { "h" + it.id }) { habit ->
                    FilterPill(habit.name, targetId == habit.id && targetIsHabit, {
                        targetId = habit.id
                        targetIsHabit = true
                    }, accent = Accents.color(habit.colorIndex))
                }
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { onSave(minutes, date, targetId, targetIsHabit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text(if (initial == null) tr("Добавить") else tr("Сохранить")) }

            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Удалить запись"), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(tr("Готово")) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(tr("Отмена")) }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showMinutesInput) {
        MinutesPromptDialog(
            initial = minutes,
            onDismiss = { showMinutesInput = false },
            onConfirm = { minutes = it; showMinutesInput = false }
        )
    }
}

/** Ввод произвольного числа минут: заготовок на всё не хватает. */
@Composable
private fun MinutesPromptDialog(
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initial.toString()) }
    val value = text.trim().toIntOrNull()
    androidx.compose.material3.AlertDialog(
        modifier = Modifier.dialogEnter(),
        onDismissRequest = onDismiss,
        title = { Text(tr("Сколько минут")) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { input -> text = input.filter { it.isDigit() }.take(4) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let { onConfirm(it.coerceIn(1, 24 * 60)) } },
                enabled = value != null && value >= 1
            ) { Text(tr("Готово")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена")) } },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

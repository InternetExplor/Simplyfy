package com.simply.app.ui.screens

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.GENERAL_PROJECT_ID
import com.simply.app.data.AppData
import com.simply.app.data.RepeatMode
import com.simply.app.data.TaskTemplate
import com.simply.app.data.iso
import com.simply.app.data.today
import com.simply.app.data.weekDayShortName
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.GroupedCard
import com.simply.app.ui.components.QuickAddBar
import com.simply.app.ui.components.RowSeparator
import com.simply.app.ui.components.ProjectPicker
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.quickAddBottomPadding
import com.simply.app.ui.theme.Accents
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun TemplatesScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    onOpenRepeat: (String?) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<TaskTemplate?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = contentPadding.calculateTopPadding())
                .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = tr("Назад"))
            }
            Column {
                Text(tr("Шаблоны"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    tr("Заготовки задач: ставь когда нужно или включи повтор"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            if (data.templates.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Bookmarks,
                        title = tr("Шаблонов пока нет"),
                        subtitle = tr("Создай заготовку для того, что повторяется: уборка, отчёт, тренировка. Ставить её потом — одно касание.")
                    )
                }
            }

            val repeating = data.templates.filter { it.repeat != RepeatMode.NONE }
            val manual = data.templates.filter { it.repeat == RepeatMode.NONE }

            item {
                SectionTitle(tr("Повторяются сами"), trailing = repeating.size.toString())
            }
            item {
                GroupedCard(Modifier.fillMaxWidth()) {
                    NewRepeatRow(onClick = { onOpenRepeat(null) })
                    if (repeating.isNotEmpty()) {
                        RowSeparator(startInset = 56.dp)
                        repeating.forEachIndexed { index, t ->
                            TemplateRow(
                                template = t,
                                projectName = data.projects
                                    .firstOrNull { it.id == t.projectId }?.name.orEmpty(),
                                colorIndex = data.projects
                                    .firstOrNull { it.id == t.projectId }?.colorIndex ?: 0,
                                createdCount = data.tasks.count { it.templateId == t.id },
                                onOpen = { onOpenRepeat(t.id) },
                                onQuickPlace = { vm.applyTemplate(t.id, today()) }
                            )
                            if (index != repeating.lastIndex) RowSeparator(startInset = 56.dp)
                        }
                    }
                }
            }
            if (manual.isNotEmpty()) {
                item { SectionTitle(tr("Ставятся вручную"), trailing = manual.size.toString()) }
                item {
                    TemplateGroup(manual, data, vm) { editing = it }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        QuickAddBar(
            onAdd = { title -> vm.addTemplate(TaskTemplate(title = title)) },
            onOpenDetails = { title ->
                editing = TaskTemplate(title = title, anchorDate = today().iso())
            },
            placeholder = tr("Новый шаблон"),
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = quickAddBottomPadding(contentPadding) + 8.dp
            )
        )
    }

    editing?.let { template ->
        val isNew = data.templates.none { it.id == template.id }
        TemplateSheet(
            template = template,
            data = data,
            isNew = isNew,
            onDismiss = { editing = null },
            onSave = {
                if (isNew) vm.addTemplate(it) else vm.updateTemplate(it)
                editing = null
            },
            onApply = { date ->
                if (isNew) vm.addTemplate(template)
                vm.applyTemplate(template.id, date)
                editing = null
            },
            onDelete = if (isNew) null else {
                { vm.deleteTemplate(template.id); editing = null }
            }
        )
    }
}

@Composable
private fun TemplateGroup(
    templates: List<TaskTemplate>,
    data: AppData,
    vm: AppViewModel,
    onOpen: (TaskTemplate) -> Unit
) {
    GroupedCard(Modifier.fillMaxWidth()) {
        templates.forEachIndexed { index, t ->
            TemplateRow(
                template = t,
                projectName = data.projects.firstOrNull { it.id == t.projectId }?.name.orEmpty(),
                colorIndex = data.projects.firstOrNull { it.id == t.projectId }?.colorIndex ?: 0,
                createdCount = data.tasks.count { it.templateId == t.id },
                onOpen = { onOpen(t) },
                onQuickPlace = { vm.applyTemplate(t.id, today()) }
            )
            if (index != templates.lastIndex) RowSeparator(startInset = 56.dp)
        }
    }
}

@Composable
private fun NewRepeatRow(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
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
                Icons.Rounded.Repeat,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("Новый повтор"), style = MaterialTheme.typography.bodyLarge, color = accent)
            Text(
                tr("Тонкая настройка расписания"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TemplateRow(
    template: TaskTemplate,
    projectName: String,
    colorIndex: Int,
    createdCount: Int,
    onOpen: () -> Unit,
    onQuickPlace: () -> Unit
) {
    val accent = Accents.color(colorIndex)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
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
                if (template.repeat == RepeatMode.NONE) Icons.Rounded.Bookmarks
                else Icons.Rounded.Repeat,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                template.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(template.scheduleLabel)
                    if (projectName.isNotEmpty()) append(" · ").append(tr(projectName))
                    template.dueTime?.let { append(trf(" · до %1\$s", it)) }
                    if (createdCount > 0) append(trf(" · поставлено %1\$d", createdCount))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onQuickPlace) {
            Icon(
                Icons.Rounded.PlaylistAdd,
                contentDescription = tr("Поставить на сегодня"),
                tint = accent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateSheet(
    template: TaskTemplate,
    data: AppData,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (TaskTemplate) -> Unit,
    onApply: (LocalDate?) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(template.title) }
    var note by remember { mutableStateOf(template.note) }
    var projectId by remember { mutableStateOf(template.projectId) }
    var quadrant by remember { mutableStateOf(template.quadrant) }
    var dueTime by remember {
        mutableStateOf(template.dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() })
    }
    var repeat by remember { mutableStateOf(template.repeat) }
    var weekDays by remember { mutableStateOf(template.weekDays) }
    var interval by remember { mutableStateOf(template.intervalDays) }
    var monthDay by remember { mutableStateOf(template.monthDay) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun collect() = template.copy(
        title = title.trim(),
        note = note.trim(),
        projectId = projectId,
        quadrant = quadrant,
        dueTime = dueTime?.format(timeFmt),
        repeat = repeat,
        weekDays = weekDays,
        intervalDays = interval,
        monthDay = monthDay,
        anchorDate = template.anchorDate ?: today().iso()
    )

    val created = data.tasks.filter { it.templateId == template.id }
    val doneCount = created.count { it.done }

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
                if (isNew) tr("Новый шаблон") else tr("Шаблон"),
                style = MaterialTheme.typography.titleLarge
            )
            if (!isNew && created.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    trf("Поставлено %1\$d, выполнено %2\$d", created.size, doneCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(tr("Название задачи")) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(tr("Заметка (необязательно)")) },
                shape = MaterialTheme.shapes.small,
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle(tr("Поставить задачу"))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(tr("Сегодня"), false, { onApply(today()) })
                FilterPill(tr("Завтра"), false, { onApply(today().plusDays(1)) })
                FilterPill(tr("Дата…"), false, { showDatePicker = true })
                FilterPill(tr("Без срока"), false, { onApply(null) })
            }

            SectionTitle(tr("Повтор"), trailing = collect().scheduleLabel)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepeatMode.entries.forEach { mode ->
                    FilterPill(tr(mode.title), repeat == mode, { repeat = mode })
                }
            }

            when (repeat) {
                RepeatMode.WEEKLY -> {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { day ->
                            FilterPill(
                                weekDayShortName(day),
                                day in weekDays,
                                {
                                    weekDays =
                                        if (day in weekDays) weekDays - day else weekDays + day
                                }
                            )
                        }
                    }
                }

                RepeatMode.EVERY_N_DAYS -> {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(2, 3, 4, 5, 7, 10, 14, 30)) { n ->
                            FilterPill("$n", interval == n, { interval = n })
                        }
                    }
                }

                RepeatMode.MONTHLY -> {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((1..28).toList()) { n ->
                            FilterPill("$n", monthDay == n, { monthDay = n })
                        }
                    }
                }

                else -> Unit
            }

            SectionTitle(tr("Выполнить до"), trailing = dueTime?.format(timeFmt) ?: tr("весь день"))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(tr("Весь день"), dueTime == null, { dueTime = null })
                listOf(9, 12, 15, 18, 21).forEach { h ->
                    val t = LocalTime.of(h, 0)
                    FilterPill(t.format(timeFmt), dueTime == t, { dueTime = t })
                }
            }

            SectionTitle(tr("Проект"))
            ProjectPicker(
                projects = data.projects.filter { it.isActive },
                selectedId = projectId,
                onSelect = { projectId = it ?: GENERAL_PROJECT_ID }
            )

            SectionTitle(
                tr("Матрица"),
                trailing = quadrant?.label ?: tr("Входящие")
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(tr("Входящие"), quadrant == null, { quadrant = null })
                FilterPill(
                    tr("Важно"),
                    quadrant?.isImportant == true,
                    {
                        quadrant = com.simply.app.data.Quadrant.of(
                            quadrant?.isImportant != true,
                            quadrant?.isUrgent == true
                        )
                    },
                    accent = Accents.color(1)
                )
                FilterPill(
                    tr("Срочно"),
                    quadrant?.isUrgent == true,
                    {
                        quadrant = com.simply.app.data.Quadrant.of(
                            quadrant?.isImportant == true,
                            quadrant?.isUrgent != true
                        )
                    },
                    accent = Accents.color(2)
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { onSave(collect()) },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (isNew) tr("Создать шаблон") else tr("Сохранить"))
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Удалить шаблон"), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = today()
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                    onApply(picked)
                }) { Text(tr("Поставить")) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(tr("Отмена")) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

package com.simply.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.GENERAL_PROJECT_ID
import com.simply.app.data.AppData
import com.simply.app.data.Quadrant
import com.simply.app.data.RepeatMode
import com.simply.app.data.TaskTemplate
import com.simply.app.data.dayMonthLabel
import com.simply.app.data.daysLabel
import com.simply.app.data.iso
import com.simply.app.data.today
import com.simply.app.data.weekDayShortName
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.GroupedCard
import com.simply.app.ui.components.RowSeparator
import com.simply.app.ui.components.ProjectPicker
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.SettingsSwitchRow
import com.simply.app.ui.components.SimplyCard
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import com.simply.app.ui.theme.Accents
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Отдельный экран для повторяющейся задачи: расписание, период действия,
 * время, задел вперёд и предпросмотр ближайших постановок.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatEditorScreen(
    vm: AppViewModel,
    data: AppData,
    templateId: String?,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val existing = templateId?.let { id -> data.templates.firstOrNull { it.id == id } }
    val isNew = existing == null

    var draft by remember(templateId) {
        mutableStateOf(
            existing ?: TaskTemplate(
                title = "",
                repeat = RepeatMode.DAILY,
                startDate = today().iso()
            )
        )
    }
    var picking by remember { mutableStateOf<String?>(null) }

    val accent = Accents.color(
        data.projects.firstOrNull { it.id == draft.projectId }?.colorIndex ?: 0
    )
    val created = data.tasks.filter { it.templateId == draft.id }
    val preview = remember(draft) { draft.nextDates(today(), 5) }

    fun save(andClose: Boolean = true) {
        if (draft.title.isBlank()) return
        if (isNew) vm.addTemplate(draft) else vm.updateTemplate(draft)
        if (andClose) onBack()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = contentPadding.calculateTopPadding())
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = tr("Назад"))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (isNew) tr("Новый повтор") else tr("Повторяющаяся задача"),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    tr("Тонкая настройка расписания"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .imePadding(),
            // Нижний отступ обязательно с учётом таб-бара: без него
            // кнопка «Сохранить» оказывается под панелью вкладок.
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            )
        ) {
            item {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    label = { Text(tr("Название задачи")) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { draft = draft.copy(note = it) },
                    label = { Text(tr("Заметка (необязательно)")) },
                    shape = MaterialTheme.shapes.small,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { SectionTitle(tr("Расписание"), trailing = draft.scheduleLabel) }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatMode.entries.filter { it != RepeatMode.NONE }.forEach { mode ->
                        FilterPill(
                            tr(mode.title),
                            draft.repeat == mode,
                            { draft = draft.copy(repeat = mode) },
                            accent = accent
                        )
                    }
                }
            }

            when (draft.repeat) {
                RepeatMode.WEEKLY -> item {
                    SectionTitle(tr("Дни недели"))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { day ->
                            FilterPill(
                                weekDayShortName(day),
                                day in draft.weekDays,
                                {
                                    val set = draft.weekDays
                                    draft = draft.copy(
                                        weekDays = if (day in set) set - day else set + day
                                    )
                                },
                                accent = accent
                            )
                        }
                    }
                }

                RepeatMode.EVERY_N_DAYS -> item {
                    SectionTitle(tr("Интервал"))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(2, 3, 4, 5, 6, 7, 10, 14, 21, 30)) { n ->
                            FilterPill(
                                "$n",
                                draft.intervalDays == n,
                                { draft = draft.copy(intervalDays = n) },
                                accent = accent
                            )
                        }
                    }
                }

                RepeatMode.MONTHLY -> item {
                    SectionTitle(tr("Число месяца"))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterPill(
                                tr("В последний день месяца"),
                                draft.monthLastDay,
                                { draft = draft.copy(monthLastDay = true) },
                                accent = accent
                            )
                        }
                        items((1..28).toList()) { n ->
                            FilterPill(
                                "$n",
                                !draft.monthLastDay && draft.monthDay == n,
                                { draft = draft.copy(monthDay = n, monthLastDay = false) },
                                accent = accent
                            )
                        }
                    }
                }

                else -> Unit
            }

            item { SectionTitle(tr("Период")) }
            item {
                GroupedCard(Modifier.fillMaxWidth()) {
                    com.simply.app.ui.components.SettingsRow(
                        title = tr("Начало"),
                        subtitle = draft.start?.dayMonthLabel() ?: tr("Сегодня"),
                        onClick = { picking = "start" }
                    )
                    RowSeparator()
                    com.simply.app.ui.components.SettingsRow(
                        title = tr("Окончание"),
                        subtitle = draft.end?.dayMonthLabel() ?: tr("Бессрочно"),
                        onClick = { picking = "end" },
                        trailing = {
                            if (draft.end != null) {
                                TextButton(onClick = { draft = draft.copy(endDate = null) }) {
                                    Text(tr("Очистить"))
                                }
                            }
                        }
                    )
                }
            }

            item {
                SectionTitle(
                    tr("Выполнить до"),
                    trailing = draft.dueTime ?: tr("весь день")
                )
            }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        tr("Весь день"),
                        draft.dueTime == null,
                        { draft = draft.copy(dueTime = null) },
                        accent = accent
                    )
                    listOf(7, 9, 12, 15, 18, 21).forEach { h ->
                        val label = LocalTime.of(h, 0).format(timeFmt)
                        FilterPill(
                            label,
                            draft.dueTime == label,
                            { draft = draft.copy(dueTime = label) },
                            accent = accent
                        )
                    }
                }
            }

            item { SectionTitle(tr("Ставить заранее")) }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        tr("Только в день"),
                        draft.leadDays == 0,
                        { draft = draft.copy(leadDays = 0) },
                        accent = accent
                    )
                    listOf(1, 3, 7, 14).forEach { n ->
                        FilterPill(
                            trf("за %1\$s", daysLabel(n)),
                            draft.leadDays == n,
                            { draft = draft.copy(leadDays = n) },
                            accent = accent
                        )
                    }
                }
            }

            item { SectionTitle(tr("Проект")) }
            item {
                ProjectPicker(
                    projects = data.projects.filter { it.isActive },
                    selectedId = draft.projectId,
                    onSelect = {
                        draft = draft.copy(
                            projectId = it ?: GENERAL_PROJECT_ID,
                            sectionId = null
                        )
                    }
                )
            }

            val sections = data.projects.firstOrNull { it.id == draft.projectId }?.sections.orEmpty()
            if (sections.isNotEmpty()) {
                item { SectionTitle(tr("Раздел")) }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterPill(
                            tr("Без раздела"),
                            draft.sectionId == null,
                            { draft = draft.copy(sectionId = null) },
                            accent = accent
                        )
                        sections.sortedBy { it.order }.forEach { section ->
                            FilterPill(
                                section.name,
                                draft.sectionId == section.id,
                                { draft = draft.copy(sectionId = section.id) },
                                accent = accent
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(
                    tr("Матрица"),
                    trailing = draft.quadrant?.label ?: tr("Входящие")
                )
            }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        tr("Входящие"),
                        draft.quadrant == null,
                        { draft = draft.copy(quadrant = null) },
                        accent = accent
                    )
                    FilterPill(
                        tr("Важно"),
                        draft.quadrant?.isImportant == true,
                        {
                            draft = draft.copy(
                                quadrant = Quadrant.of(
                                    draft.quadrant?.isImportant != true,
                                    draft.quadrant?.isUrgent == true
                                )
                            )
                        },
                        accent = Accents.color(1)
                    )
                    FilterPill(
                        tr("Срочно"),
                        draft.quadrant?.isUrgent == true,
                        {
                            draft = draft.copy(
                                quadrant = Quadrant.of(
                                    draft.quadrant?.isImportant == true,
                                    draft.quadrant?.isUrgent != true
                                )
                            )
                        },
                        accent = Accents.color(2)
                    )
                }
            }

            item { SectionTitle(tr("Поведение")) }
            item {
                GroupedCard(Modifier.fillMaxWidth()) {
                    SettingsSwitchRow(
                        title = tr("Повтор включён"),
                        subtitle = tr("Выключенный повтор задач не ставит"),
                        checked = draft.active,
                        onCheckedChange = { draft = draft.copy(active = it) }
                    )
                    RowSeparator()
                    SettingsSwitchRow(
                        title = tr("Не дублировать"),
                        subtitle = tr("Не ставить новую, пока предыдущая не выполнена"),
                        checked = draft.skipIfPending,
                        onCheckedChange = { draft = draft.copy(skipIfPending = it) }
                    )
                }
            }

            item { SectionTitle(tr("Ближайшие постановки")) }
            item {
                SimplyCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Column {
                        if (preview.isEmpty()) {
                            Text(
                                tr("Повтор не настроен"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            preview.forEachIndexed { index, date ->
                                Text(
                                    date.dayMonthLabel() +
                                        (draft.dueTime?.let { ", " + tr("до") + " $it" } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (index == 0) accent
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (index != preview.lastIndex) Spacer(Modifier.height(6.dp))
                            }
                        }
                        if (created.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                trf(
                                    "Поставлено %1\$d, выполнено %2\$d",
                                    created.size,
                                    created.count { it.done }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { save() },
                    enabled = draft.title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(tr("Сохранить"))
                }
                if (!isNew) {
                    TextButton(
                        onClick = { vm.applyTemplate(draft.id, today()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Rounded.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(tr("Поставить сейчас"))
                    }
                    TextButton(
                        onClick = { vm.deleteTemplate(draft.id); onBack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr("Удалить повтор"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    picking?.let { which ->
        val initial = if (which == "start") draft.start ?: today() else draft.end ?: today()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    if (picked != null) {
                        draft = if (which == "start") draft.copy(startDate = picked.iso())
                        else draft.copy(endDate = picked.iso())
                    }
                    picking = null
                }) { Text(tr("Готово")) }
            },
            dismissButton = {
                TextButton(onClick = { picking = null }) { Text(tr("Отмена")) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

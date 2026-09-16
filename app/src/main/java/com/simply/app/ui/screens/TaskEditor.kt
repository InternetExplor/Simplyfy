package com.simply.app.ui.screens

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.simply.app.data.Subtask
import com.simply.app.ui.components.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.simply.app.data.GENERAL_PROJECT_ID
import com.simply.app.data.Project
import com.simply.app.data.Quadrant
import com.simply.app.data.Task
import com.simply.app.data.dayMonthLabel
import com.simply.app.data.relativeLabel
import com.simply.app.data.today
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.ProjectPicker
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.theme.Accents
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.ZoneId

val Quadrant.label: String
    get() = when (this) {
        Quadrant.DO -> tr("Сделать сейчас")
        Quadrant.PLAN -> tr("Запланировать")
        Quadrant.DELEGATE -> tr("Быстро закрыть")
        Quadrant.DROP -> tr("Когда-нибудь")
    }

val Quadrant.shortLabel: String
    get() = when (this) {
        Quadrant.DO -> tr("Срочно и важно")
        Quadrant.PLAN -> tr("Важно")
        Quadrant.DELEGATE -> tr("Срочно")
        Quadrant.DROP -> tr("Не срочно")
    }

val Quadrant.accentIndex: Int
    get() = when (this) {
        Quadrant.DO -> 2
        Quadrant.PLAN -> 1
        Quadrant.DELEGATE -> 4
        Quadrant.DROP -> 3
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    initial: Task?,
    projects: List<Project>,
    defaultTitle: String = "",
    defaultProjectId: String? = null,
    defaultQuadrant: Quadrant? = null,
    defaultDue: LocalDate? = null,
    templateName: String? = null,
    onSaveAsTemplate: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initial?.title ?: defaultTitle) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var projectId by remember {
        mutableStateOf(initial?.projectId ?: defaultProjectId ?: GENERAL_PROJECT_ID)
    }
    var due by remember { mutableStateOf(initial?.dueDate ?: defaultDue) }
    var dueTime by remember { mutableStateOf(initial?.dueLocalTime) }
    // null — задача остаётся во «Входящих» матрицы.
    var quadrant by remember { mutableStateOf(initial?.quadrant ?: defaultQuadrant) }
    var sectionId by remember { mutableStateOf(initial?.sectionId) }
    var subtasks by remember { mutableStateOf(initial?.subtasks.orEmpty()) }
    var newStep by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (initial == null) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    fun save() {
        if (title.isBlank()) return
        val base = initial ?: Task(title = title)
        onSave(
            base.copy(
                title = title.trim(),
                note = note.trim(),
                subtasks = subtasks,
                projectId = projectId ?: GENERAL_PROJECT_ID,
                sectionId = sectionId,
                due = due?.toString(),
                dueTime = if (due == null) null else dueTime?.format(timeFmt),
                quadrant = quadrant
            )
        )
    }

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
                if (initial == null) tr("Новая задача") else tr("Задача"),
                style = MaterialTheme.typography.titleLarge
            )
            if (templateName != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Bookmarks,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        trf("Из шаблона «%1\$s»", templateName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(tr("Что нужно сделать")) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(tr("Заметка (необязательно)")) },
                shape = MaterialTheme.shapes.small,
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle(
                tr("Выполнить до"),
                trailing = due?.let { d ->
                    d.relativeLabel() + (dueTime?.let { tr(", до ") + it.format(timeFmt) } ?: "")
                } ?: tr("без срока")
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(tr("Без срока"), due == null, { due = null; dueTime = null })
                FilterPill(tr("Сегодня"), due == today(), { due = today() })
                FilterPill(tr("Завтра"), due == today().plusDays(1), { due = today().plusDays(1) })
                val custom = due != null && due != today() && due != today().plusDays(1)
                FilterPill(
                    if (custom) due!!.dayMonthLabel() else tr("Дата…"),
                    custom,
                    { showDatePicker = true }
                )
            }

            if (due != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(tr("Весь день"), dueTime == null, { dueTime = null })
                    quickTimes.forEach { t ->
                        FilterPill(t.format(timeFmt), dueTime == t, { dueTime = t })
                    }
                    val customTime = dueTime != null && dueTime !in quickTimes
                    FilterPill(
                        if (customTime) dueTime!!.format(timeFmt) else tr("Время…"),
                        customTime,
                        { showTimePicker = true }
                    )
                }
            }

            SectionTitle(
                tr("Шаги"),
                trailing = if (subtasks.isEmpty()) tr("необязательно")
                else "${subtasks.count { it.done }} / ${subtasks.size}"
            )
            // Поле ввода стоит первым и никуда не уезжает: добавленный шаг
            // встаёт под ним, и следующий пишут не сходя с места. Раньше список
            // рос сверху и утаскивал поле вниз — после каждого шага приходилось
            // догонять его пальцем.
            fun addStep() {
                val clean = newStep.trim()
                if (clean.isEmpty()) return
                subtasks = subtasks + Subtask(title = clean)
                newStep = ""
            }

            OutlinedTextField(
                value = newStep,
                onValueChange = { newStep = it },
                placeholder = { Text(tr("Добавить шаг")) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addStep() }),
                trailingIcon = {
                    if (newStep.isNotBlank()) {
                        IconButton(onClick = { addStep() }) {
                            Icon(Icons.Rounded.Add, contentDescription = tr("Добавить"))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (subtasks.isNotEmpty()) Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                subtasks.forEach { step ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckCircle(
                            checked = step.done,
                            onClick = {
                                subtasks = subtasks.map {
                                    if (it.id == step.id) it.copy(done = !it.done) else it
                                }
                            },
                            size = 20.dp
                        )
                        // Название шага правится на месте: раньше уже добавленный
                        // шаг оставалось только удалить и завести заново.
                        BasicTextField(
                            value = step.title,
                            onValueChange = { text ->
                                subtasks = subtasks.map {
                                    if (it.id == step.id) it.copy(title = text) else it
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = if (step.done) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (step.done) TextDecoration.LineThrough else null
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            subtasks = subtasks.filterNot { it.id == step.id }
                        }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = tr("Убрать"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            SectionTitle(tr("Проект"))
            ProjectPicker(
                projects = projects,
                selectedId = projectId,
                onSelect = { projectId = it ?: GENERAL_PROJECT_ID; sectionId = null }
            )

            val sections = projects.firstOrNull { it.id == projectId }?.sections.orEmpty()
            if (sections.isNotEmpty()) {
                SectionTitle(tr("Раздел"))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(tr("Без раздела"), sectionId == null, { sectionId = null })
                    sections.sortedBy { it.order }.forEach { section ->
                        FilterPill(
                            section.name,
                            sectionId == section.id,
                            { sectionId = section.id }
                        )
                    }
                }
            }

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
                        quadrant = Quadrant.of(
                            quadrant?.isImportant != true,
                            quadrant?.isUrgent == true
                        )
                    },
                    accent = Accents.color(1)
                )
                // «Срочно» и «Не срочно» — пара переключателей, а не два флажка:
                // раньше квадрант «Не срочно» нельзя было выбрать вовсе, он
                // получался только тем, что ничего не нажали, и был неотличим
                // от «Входящих».
                FilterPill(
                    tr("Срочно"),
                    quadrant?.isUrgent == true,
                    { quadrant = Quadrant.of(quadrant?.isImportant == true, true) },
                    accent = Accents.color(2)
                )
                FilterPill(
                    tr("Не срочно"),
                    quadrant != null && !quadrant!!.isUrgent,
                    { quadrant = Quadrant.of(quadrant?.isImportant == true, false) },
                    accent = Accents.color(3)
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { save() },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text(if (initial == null) tr("Добавить") else tr("Сохранить"))
            }

            if (onSaveAsTemplate != null) {
                TextButton(
                    onClick = onSaveAsTemplate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Rounded.Bookmarks,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Сохранить как шаблон"))
                }
            }

            if (onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Удалить задачу"), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (due ?: today())
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        due = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
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

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = dueTime?.hour ?: 18,
            initialMinute = dueTime?.minute ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueTime = LocalTime.of(state.hour, state.minute)
                    showTimePicker = false
                }) { Text(tr("Готово")) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(tr("Отмена")) }
            },
            title = { Text(tr("Выполнить до")) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }
}

private val quickTimes = listOf(
    LocalTime.of(9, 0),
    LocalTime.of(12, 0),
    LocalTime.of(15, 0),
    LocalTime.of(18, 0),
    LocalTime.of(21, 0)
)

val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Подпись дедлайна: текст + признак «просрочено». */
fun dueLabel(task: Task, withDate: Boolean = true): Pair<String, Boolean>? {
    val date = task.dueDate ?: return null
    val time = task.dueLocalTime
    val text = when {
        withDate && time != null ->
            trf("%1\$s, до %2\$s", date.relativeLabel(), time.format(timeFmt))
        withDate -> date.relativeLabel()
        time != null -> trf("до %1\$s", time.format(timeFmt))
        else -> return null
    }
    return text to task.isOverdue()
}

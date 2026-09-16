package com.simply.app.ui.screens

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.PomodoroSettings
import com.simply.app.data.ReminderOffset
import com.simply.app.data.ThemeMode
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.FilterPill
import com.simply.app.ui.components.GroupedCard
import com.simply.app.ui.components.RowSeparator
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.SettingsRow
import com.simply.app.ui.components.SettingsSwitchRow
import com.simply.app.ui.components.contrastOn
import com.simply.app.ui.theme.AccentTheme
import com.simply.app.ui.theme.Accents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    onOpenTrash: () -> Unit,
    onStartGuide: () -> Unit,
    onBack: () -> Unit
) {
    val s = data.settings
    val p = data.pomodoro
    var confirmReset by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingNotificationToggle by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingNotificationToggle
        pendingNotificationToggle = null
        if (granted) action?.invoke()
        else message = tr("Без разрешения на уведомления Simply не сможет их показывать")
    }

    /** На Android 13+ уведомления требуют разрешения — спрашиваем при включении. */
    fun withNotificationPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            pendingNotificationToggle = action
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(vm.exportJson().toByteArray())
                } ?: error("no stream")
            }.isSuccess
            message = if (ok) tr("Копия сохранена") else tr("Не удалось сохранить файл")
        }
    }

    // Папка для автокопии: держим постоянное разрешение, иначе доступ отвалится.
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        vm.updateSettings(s.copy(autoBackup = true, backupFolder = uri.toString()))
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (raw == null) message = tr("Не удалось прочитать файл") else pendingImport = raw
        }
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
                .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = tr("Назад"))
            }
            Text(tr("Настройки"), style = MaterialTheme.typography.headlineSmall)
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp
            )
        ) {
            item { SectionTitle(tr("Оформление")) }
            item {
                GroupedCard {
                    SettingsRow(
                        title = tr("Тема"),
                        subtitle = tr(s.themeMode.label),
                        icon = if (s.themeMode == ThemeMode.LIGHT) Icons.Rounded.WbSunny
                        else Icons.Rounded.DarkMode
                    )
                    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterPill(
                                    label = tr(mode.label),
                                    selected = s.themeMode == mode,
                                    onClick = { vm.updateSettings(s.copy(themeMode = mode)) }
                                )
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        RowSeparator()
                        SettingsSwitchRow(
                            title = tr("Цвета системы"),
                            subtitle = tr("Палитра из обоев, как в Material You"),
                            icon = Icons.Rounded.AutoAwesome,
                            checked = s.dynamicColor,
                            onCheckedChange = { vm.updateSettings(s.copy(dynamicColor = it)) }
                        )
                    }
                    RowSeparator()
                    SettingsRow(
                        title = tr("Акцент"),
                        subtitle = if (s.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            tr("Отключи цвета системы, чтобы выбрать свой")
                        else tr(AccentTheme.at(s.accent).title),
                        icon = Icons.Rounded.Palette
                    )
                    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AccentTheme.entries.forEachIndexed { i, theme ->
                                AccentSwatch(
                                    color = if (com.simply.app.ui.theme.LocalIsDark.current) theme.darkPrimary
                                    else theme.lightPrimary,
                                    selected = s.accent == i,
                                    onClick = { vm.updateSettings(s.copy(accent = i)) }
                                )
                            }
                        }
                    }
                }
            }

            item { SectionTitle(tr("Задачи")) }
            item {
                GroupedCard {
                    SettingsSwitchRow(
                        title = tr("Показывать выполненные"),
                        subtitle = tr("Блок «Выполнено» под списком"),
                        icon = Icons.Rounded.Visibility,
                        checked = s.showCompleted,
                        onCheckedChange = { vm.updateSettings(s.copy(showCompleted = it)) }
                    )
                    RowSeparator()
                    SettingsSwitchRow(
                        title = tr("Просроченные в «Сегодня»"),
                        subtitle = tr("Не теряются, если срок уже прошёл"),
                        icon = Icons.Rounded.Today,
                        checked = s.rollOverdue,
                        onCheckedChange = { vm.updateSettings(s.copy(rollOverdue = it)) }
                    )
                }
            }

            item { SectionTitle(tr("Уведомления")) }
            item {
                GroupedCard {
                    SettingsSwitchRow(
                        title = tr("Таймер в шторке"),
                        subtitle = tr("Отсчёт продолжается, когда приложение свёрнуто"),
                        icon = Icons.Rounded.Timer,
                        checked = s.pomodoroNotification,
                        onCheckedChange = { on ->
                            if (on) withNotificationPermission {
                                vm.updateSettings(s.copy(pomodoroNotification = true))
                            } else vm.updateSettings(s.copy(pomodoroNotification = false))
                        }
                    )
                    RowSeparator()
                    SettingsSwitchRow(
                        title = tr("Напоминания о дедлайнах"),
                        subtitle = tr("Для задач со временем «выполнить до»"),
                        icon = Icons.Rounded.NotificationsActive,
                        iconTint = Accents.color(2),
                        checked = s.deadlineReminders,
                        onCheckedChange = { on ->
                            if (on) withNotificationPermission {
                                vm.updateSettings(s.copy(deadlineReminders = true))
                            } else vm.updateSettings(s.copy(deadlineReminders = false))
                        }
                    )
                    if (s.deadlineReminders) {
                        RowSeparator()
                        Column(Modifier.padding(top = 12.dp, bottom = 14.dp)) {
                            Text(
                                tr("Когда напоминать"),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(ReminderOffset.entries.toList()) { offset ->
                                    FilterPill(
                                        tr(offset.label),
                                        s.reminderOffsetMinutes == offset.minutes,
                                        { vm.updateSettings(s.copy(reminderOffsetMinutes = offset.minutes)) }
                                    )
                                }
                            }
                        }
                    }
                    RowSeparator()
                    SettingsRow(
                        title = tr("Системные настройки уведомлений"),
                        subtitle = tr("Звук, важность, показ на экране блокировки"),
                        icon = Icons.Rounded.OpenInNew,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    )
                }
            }

            item { SectionTitle(tr("Помодоро")) }
            item {
                GroupedCard {
                    MinutesRow(tr("Работа"), listOf(15, 20, 25, 30, 45, 50), p.workMinutes) {
                        vm.updatePomodoroSettings(p.copy(workMinutes = it))
                    }
                    RowSeparator()
                    MinutesRow(tr("Короткий перерыв"), listOf(3, 5, 7, 10), p.shortBreakMinutes) {
                        vm.updatePomodoroSettings(p.copy(shortBreakMinutes = it))
                    }
                    RowSeparator()
                    MinutesRow(tr("Длинный перерыв"), listOf(10, 15, 20, 30), p.longBreakMinutes) {
                        vm.updatePomodoroSettings(p.copy(longBreakMinutes = it))
                    }
                    RowSeparator()
                    MinutesRow(
                        tr("Сессий до длинного"),
                        listOf(2, 3, 4, 5, 6),
                        p.cyclesBeforeLongBreak,
                        suffix = ""
                    ) {
                        vm.updatePomodoroSettings(p.copy(cyclesBeforeLongBreak = it))
                    }
                    RowSeparator()
                    SettingsSwitchRow(
                        title = tr("Вибрация в конце"),
                        icon = Icons.Rounded.NotificationsActive,
                        checked = p.vibrate,
                        onCheckedChange = { vm.updatePomodoroSettings(p.copy(vibrate = it)) }
                    )
                    RowSeparator()
                    SettingsSwitchRow(
                        title = tr("Не гасить экран"),
                        subtitle = tr("Пока идёт таймер"),
                        icon = Icons.Rounded.Schedule,
                        checked = p.keepScreenOn,
                        onCheckedChange = { vm.updatePomodoroSettings(p.copy(keepScreenOn = it)) }
                    )
                }
            }

            item { SectionTitle(tr("Данные")) }
            item {
                GroupedCard {
                    SettingsRow(
                        title = tr("Сохранить резервную копию"),
                        subtitle = tr("Все задачи, привычки, шаблоны и статистика в один файл"),
                        icon = Icons.Rounded.Save,
                        iconTint = Accents.color(4),
                        onClick = { exportLauncher.launch(defaultBackupName()) }
                    )
                    RowSeparator()
                    SettingsRow(
                        title = tr("Восстановить из копии"),
                        subtitle = tr("Текущие данные будут заменены"),
                        icon = Icons.Rounded.Restore,
                        iconTint = Accents.color(1),
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    )
                    RowSeparator()
                    SettingsRow(
                        title = tr("Удалить выполненные задачи"),
                        subtitle = trf("%1\$d в списке", data.tasks.count { it.done }),
                        icon = Icons.Rounded.CleaningServices,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        onClick = { vm.clearCompleted() }
                    )
                    RowSeparator()
                    SettingsSwitchRow(
                        title = tr("Автокопия раз в неделю"),
                        subtitle = if (s.backupFolder == null) tr("Выбери папку для копий")
                        else tr("Копия сохраняется при запуске приложения"),
                        icon = Icons.Rounded.Backup,
                        iconTint = Accents.color(1),
                        checked = s.autoBackup && s.backupFolder != null,
                        onCheckedChange = { on ->
                            if (!on) vm.updateSettings(s.copy(autoBackup = false))
                            else if (s.backupFolder == null) folderLauncher.launch(null)
                            else vm.updateSettings(s.copy(autoBackup = true))
                        }
                    )
                    if (s.autoBackup && s.backupFolder != null) {
                        RowSeparator()
                        SettingsRow(
                            title = tr("Папка для копий"),
                            subtitle = folderLabel(s.backupFolder),
                            icon = Icons.Rounded.FolderOpen,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            onClick = { folderLauncher.launch(null) }
                        )
                    }
                    RowSeparator()
                    SettingsRow(
                        title = tr("Корзина"),
                        subtitle = trf("%1\$d в корзине", data.trash.size),
                        icon = Icons.Rounded.DeleteOutline,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        onClick = onOpenTrash
                    )
                    RowSeparator()
                    SettingsRow(
                        title = tr("Сбросить всё"),
                        subtitle = tr("Задачи, проекты, привычки и статистика"),
                        icon = Icons.Rounded.DeleteForever,
                        iconTint = MaterialTheme.colorScheme.error,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { confirmReset = true }
                    )
                }
            }

            item { SectionTitle(tr("О приложении")) }
            item {
                GroupedCard {
                    SettingsRow(
                        title = tr("Как пользоваться"),
                        subtitle = tr("Короткий тур по разделам приложения"),
                        icon = Icons.Rounded.School,
                        iconTint = Accents.color(3),
                        onClick = onStartGuide
                    )
                    RowSeparator()
                    SettingsRow(
                        title = "Simply",
                        subtitle = tr("Версия 1.0 · всё хранится только на устройстве"),
                        icon = Icons.Rounded.Info,
                        iconTint = Accents.color(0)
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    pendingImport?.let { raw ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(tr("Восстановить из копии?")) },
            text = { Text(tr("Все текущие задачи, привычки, шаблоны и статистика будут заменены содержимым файла.")) },
            confirmButton = {
                TextButton(onClick = {
                    message = if (vm.importJson(raw)) tr("Данные восстановлены")
                    else tr("Файл не похож на копию Simply")
                    pendingImport = null
                }) { Text(tr("Восстановить")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text(tr("Отмена")) }
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text(tr("Понятно")) } },
            text = { Text(text) },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr("Сбросить все данные?")) },
            text = { Text(tr("Задачи, проекты, привычки и статистика помодоро будут удалены. Отменить это нельзя.")) },
            confirmButton = {
                TextButton(onClick = { vm.resetEverything(); confirmReset = false }) {
                    Text(tr("Сбросить"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(tr("Отмена")) }
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }
}

@Composable
private fun MinutesRow(
    title: String,
    values: List<Int>,
    selected: Int,
    suffix: String = tr("мин"),
    onSelect: (Int) -> Unit
) {
    Column(Modifier.padding(top = 12.dp, bottom = 14.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(values) { v ->
                FilterPill(
                    if (suffix.isBlank()) "$v" else "$v $suffix",
                    v == selected,
                    { onSelect(v) }
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        CircleShape
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = contrastOn(color),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Из SAF-адреса вытаскиваем последнюю часть — она читается как имя папки. */
private fun folderLabel(uri: String?): String {
    if (uri == null) return ""
    val decoded = android.net.Uri.decode(uri)
    return decoded.substringAfterLast(':').ifBlank { decoded }.substringAfterLast('/')
}

private fun defaultBackupName(): String {
    val stamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return "simply-backup-$stamp.json"
}

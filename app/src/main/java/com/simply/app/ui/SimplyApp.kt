package com.simply.app.ui

import com.simply.app.ui.components.dialogEnter
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.simply.app.pomodoro.FocusPrompt
import com.simply.app.pomodoro.StopPrompt
import com.simply.app.data.formatMinutes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simply.app.MainActivity
import com.simply.app.ui.screens.CalendarScreen
import com.simply.app.ui.screens.FocusHistoryScreen
import com.simply.app.ui.screens.FocusScreen
import com.simply.app.ui.screens.HabitsScreen
import com.simply.app.ui.screens.MatrixScreen
import com.simply.app.ui.screens.SearchScreen
import com.simply.app.ui.screens.SettingsScreen
import com.simply.app.ui.screens.ProjectScreen
import com.simply.app.ui.screens.ProjectsScreen
import com.simply.app.ui.screens.RepeatEditorScreen
import com.simply.app.ui.screens.StatsScreen
import com.simply.app.ui.screens.TasksScreen
import com.simply.app.ui.screens.TrashScreen
import com.simply.app.ui.screens.TemplatesScreen
import com.simply.app.ui.components.CoachKeys
import com.simply.app.ui.components.CoachOverlay
import com.simply.app.ui.components.CoachTargets
import com.simply.app.ui.components.LocalCoachTargets
import com.simply.app.ui.components.coachTarget
import com.simply.app.ui.theme.SimplyTheme
import com.simply.app.ui.theme.Motion

private enum class Tab(
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val hasAdd: Boolean
) {
    TASKS(tr("Задачи"), Icons.Outlined.CheckCircle, Icons.Rounded.CheckCircle, false),
    HABITS(tr("Привычки"), Icons.Outlined.Whatshot, Icons.Rounded.Whatshot, true),
    MATRIX(tr("Матрица"), Icons.Outlined.GridView, Icons.Rounded.GridView, false),
    FOCUS(tr("Фокус"), Icons.Outlined.Timer, Icons.Rounded.Timer, false),
    CALENDAR(tr("Календарь"), Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth, false)
}

@Composable
fun SimplyApp(
    launchAction: String? = null,
    onLaunchActionHandled: () -> Unit = {}
) {
    val vm: AppViewModel = viewModel()
    val data by vm.data.collectAsState()

    // Сохраняем состояние на диск, когда приложение уходит в фон.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Плашка «Отменить» после удаления: снимок состояния уже лежит во ViewModel.
    val snackbarHostState = remember { SnackbarHostState() }
    val undoMessage by vm.undoMessage.collectAsState()
    val focusPrompt by vm.focusPrompt.collectAsState()
    val stopPrompt by vm.stopPrompt.collectAsState()
    LaunchedEffect(undoMessage) {
        val message = undoMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = tr("Отменить"),
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) vm.undo() else vm.dismissUndo()
    }

    SimplyTheme(
        mode = data.settings.themeMode,
        accentIndex = data.settings.accent,
        dynamicColor = data.settings.dynamicColor
    ) {
        var tab by remember { mutableStateOf(Tab.TASKS) }
        var addRequested by remember { mutableStateOf(false) }

        // Ярлык с иконки приложения и кнопка на виджете.
        LaunchedEffect(launchAction) {
            when (launchAction) {
                MainActivity.ACTION_NEW_TASK -> {
                    tab = Tab.TASKS
                    addRequested = true
                }

                MainActivity.ACTION_START_FOCUS -> {
                    tab = Tab.FOCUS
                    if (!vm.pomodoro.value.running) vm.toggleTimer()
                }
            }
            if (launchAction != null) onLaunchActionHandled()
        }
        val openTasks = data.tasks.count { !it.done }
        // Задачи без срока ни в одном дне не показываются значком над датой,
        // поэтому о них напоминает бейдж вкладки — как открытые задачи у матрицы.
        val undatedTasks = data.tasks.count { !it.done && it.dueDate == null }
        var showSettings by remember { mutableStateOf(false) }
        var showTemplates by remember { mutableStateOf(false) }
        var showStats by remember { mutableStateOf(false) }
        var showTrash by remember { mutableStateOf(false) }
        var showProjects by remember { mutableStateOf(false) }
        var showSearch by remember { mutableStateOf(false) }
        var showFocusHistory by remember { mutableStateOf(false) }
        var openProjectId by remember { mutableStateOf<String?>(null) }
        var repeatEditor by remember { mutableStateOf<Pair<Boolean, String?>?>(null) }
        val overlayOpen = showSettings || showTemplates || showStats || showSearch ||
            showProjects || openProjectId != null || repeatEditor != null || showTrash ||
            showFocusHistory

        BackHandler(enabled = overlayOpen) {
            when {
                showTrash -> showTrash = false
                showFocusHistory -> showFocusHistory = false
                repeatEditor != null -> repeatEditor = null
                // Проект лежит поверх поиска — закрываем его первым.
                openProjectId != null -> openProjectId = null
                showSearch -> showSearch = false
                else -> {
                    showSettings = false
                    showTemplates = false
                    showStats = false
                    showProjects = false
                }
            }
        }

        // Вводный гид: рассказ идёт по живому приложению — всё темнеет, кроме
        // одного места. Координаты мест собирает CoachTargets, а шаг знает,
        // какую вкладку для этого открыть.
        val guide = remember { guideStops() }
        val coachTargets = remember { CoachTargets() }
        var guideStep by remember { mutableStateOf(-1) }
        val guideStop = guide.getOrNull(guideStep)

        fun stopGuide() {
            guideStep = -1
            vm.markGuideSeen()
        }

        // Первый запуск показывает гид сам, дальше — только кнопкой в настройках.
        LaunchedEffect(Unit) { if (!data.settings.guideSeen) guideStep = 0 }

        // Шаг открывает свою вкладку и убирает всё, что лежит поверх неё.
        LaunchedEffect(guideStep) {
            val stop = guide.getOrNull(guideStep) ?: return@LaunchedEffect
            showSettings = false
            showTemplates = false
            showStats = false
            showProjects = false
            showSearch = false
            showFocusHistory = false
            showTrash = false
            openProjectId = null
            repeatEditor = null
            stop.tab?.let { tab = it }
        }

        // Гид лежит поверх всего, поэтому «назад» первым закрывает его.
        BackHandler(enabled = guideStep >= 0) { stopGuide() }

        CompositionLocalProvider(LocalCoachTargets provides coachTargets) {
            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { snackbarData ->
                            Snackbar(
                                snackbarData = snackbarData,
                                shape = MaterialTheme.shapes.small,
                                containerColor = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                actionColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = tab.hasAdd && !overlayOpen,
                            enter = scaleIn(Motion.enter(Motion.QUICK)) + fadeIn(Motion.enter(Motion.QUICK)),
                            exit = fadeOut(Motion.exit(Motion.QUICK))
                        ) {
                            FloatingActionButton(
                                onClick = { addRequested = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = MaterialTheme.shapes.large,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = tr("Добавить"))
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 0.dp
                        ) {
                            Tab.entries.forEach { t ->
                                val selected = t == tab && !overlayOpen
                                NavigationBarItem(
                                    modifier = Modifier.coachTarget(CoachKeys.tab(t.name)),
                                    selected = selected,
                                    onClick = {
                                        showSettings = false
                                        showTemplates = false
                                        showStats = false
                                        showProjects = false
                                        showSearch = false
                                        showFocusHistory = false
                                        showTrash = false
                                        openProjectId = null
                                        repeatEditor = null
                                        if (t != tab) addRequested = false
                                        tab = t
                                    },
                                    icon = {
                                        // Число висит над иконкой значком, как уведомление
                                        // на иконке приложения: у матрицы — все открытые
                                        // задачи, у задач — те, у которых нет срока.
                                        val badge = when (t) {
                                            Tab.MATRIX -> openTasks
                                            Tab.TASKS -> undatedTasks
                                            else -> 0
                                        }
                                        if (badge == 0) {
                                            Icon(
                                                if (selected) t.iconSelected else t.icon,
                                                contentDescription = t.label
                                            )
                                        } else {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                        contentColor = MaterialTheme.colorScheme.onError
                                                    ) {
                                                        Text(if (badge > 99) "99+" else badge.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    if (selected) t.iconSelected else t.icon,
                                                    contentDescription = if (t == Tab.TASKS) trf(
                                                        "%1\$s, без срока: %2\$d", t.label, badge
                                                    ) else trf(
                                                        "%1\$s, открытых задач: %2\$d", t.label, badge
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    label = {
                                        Text(t.label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                    },
                                    alwaysShowLabel = true,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val padding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    )

                    // Вкладки уезжают в ту сторону, в которую переключились: резкая
                    // подмена экрана читалась как сбой отрисовки.
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally(Motion.enter()) { it / 6 * dir } + fadeIn(Motion.enter()))
                                .togetherWith(
                                    slideOutHorizontally(Motion.exit()) { -it / 6 * dir } + fadeOut(Motion.exit(Motion.QUICK))
                                )
                        },
                        label = "tabs"
                    ) { current ->
                        when (current) {
                            Tab.TASKS -> TasksScreen(
                                vm, data, padding,
                                addRequested = addRequested && tab == Tab.TASKS,
                                onAddHandled = { addRequested = false },
                                onOpenSettings = { showSettings = true },
                                onOpenTemplates = { showTemplates = true },
                                onOpenProjects = { showProjects = true },
                                onOpenSearch = { showSearch = true }
                            )
                            Tab.HABITS -> HabitsScreen(
                                vm, data, padding,
                                addRequested = addRequested && tab == Tab.HABITS,
                                onAddHandled = { addRequested = false },
                                onOpenSettings = { showSettings = true },
                                onOpenProjects = { showProjects = true }
                            )
                            Tab.MATRIX -> MatrixScreen(
                                vm, data, padding,
                                onOpenSettings = { showSettings = true },
                                onOpenProjects = { showProjects = true }
                            )
                            Tab.FOCUS -> FocusScreen(
                                vm, data, padding,
                                onOpenSettings = { showSettings = true },
                                onOpenStats = { showStats = true },
                                onOpenProjects = { showProjects = true }
                            )
                            Tab.CALENDAR -> CalendarScreen(
                                vm, data, padding,
                                onOpenSettings = { showSettings = true },
                                onOpenProjects = { showProjects = true }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showProjects,
                        enter = slideInHorizontally(Motion.enter()) { it / 4 } + fadeIn(Motion.enter()),
                        exit = slideOutHorizontally(Motion.exit()) { it / 4 } + fadeOut(Motion.exit(Motion.QUICK))
                    ) {
                        ProjectsScreen(
                            vm = vm,
                            data = data,
                            contentPadding = padding,
                            onOpenProject = { openProjectId = it },
                            onBack = { showProjects = false }
                        )
                    }

                    AnimatedVisibility(
                        visible = showSearch,
                        enter = slideInHorizontally(Motion.enter()) { it / 4 } + fadeIn(Motion.enter()),
                        exit = slideOutHorizontally(Motion.exit()) { it / 4 } + fadeOut(Motion.exit(Motion.QUICK))
                    ) {
                        SearchScreen(
                            vm = vm,
                            data = data,
                            contentPadding = padding,
                            onOpenProject = { openProjectId = it },
                            onBack = { showSearch = false }
                        )
                    }

                    // Экран рисуем по последнему открытому id, а не по текущему:
                    // иначе на закрытие содержимое исчезает раньше анимации ухода.
                    var lastProjectId by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(openProjectId) {
                        if (openProjectId != null) lastProjectId = openProjectId
                    }
                    AnimatedVisibility(
                        visible = openProjectId != null,
                        enter = slideInHorizontally(Motion.enter()) { it / 4 } + fadeIn(Motion.enter()),
                        exit = slideOutHorizontally(Motion.exit()) { it / 4 } + fadeOut(Motion.exit(Motion.QUICK))
                    ) {
                        lastProjectId?.let { id ->
                            ProjectScreen(
                                vm = vm,
                                data = data,
                                projectId = id,
                                contentPadding = padding,
                                onBack = { openProjectId = null }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showStats,
                        enter = slideInHorizontally(Motion.enter()) { it / 4 } + fadeIn(Motion.enter()),
                        exit = slideOutHorizontally(Motion.exit()) { it / 4 } + fadeOut(Motion.exit(Motion.QUICK))
                    ) {
                        StatsScreen(
                            data = data,
                            contentPadding = padding,
                            onOpenHistory = { showFocusHistory = true },
                            onBack = { showStats = false }
                        )
                    }

                    // История открывается из статистики, поэтому объявлена после неё —
                    // иначе откроется невидимой под родителем.
                    AnimatedVisibility(
                        visible = showFocusHistory,
                        enter = slideInHorizontally(Motion.enter()) { it / 4 } + fadeIn(Motion.enter()),
                        exit = slideOutHorizontally(Motion.exit()) { it / 4 } + fadeOut(Motion.exit(Motion.QUICK))
                    ) {
                        FocusHistoryScreen(
                            vm = vm,
                            data = data,
                            contentPadding = padding,
                            onBack = { showFocusHistory = false }
                        )
                    }

                    AnimatedVisibility(
                        visible = showTemplates,
                        enter = slideInHorizontally(Motion.enter()) { it / 4 } + fadeIn(Motion.enter()),
                        exit = slideOutHorizontally(Motion.exit()) { it / 4 } + fadeOut(Motion.exit(Motion.QUICK))
                    ) {
                        TemplatesScreen(
                            vm = vm,
                            data = data,
                            contentPadding = padding,
                            onOpenRepeat = { id -> repeatEditor = true to id },
                            onBack = { showTemplates = false }
                        )
                    }

                    var lastRepeatId by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(repeatEditor) {
                        if (repeatEditor != null) lastRepeatId = repeatEditor?.second
                    }
                    AnimatedVisibility(
                        visible = repeatEditor != null,
                        enter = slideInHorizontally(Motion.enter()) { it / 4 } + fadeIn(Motion.enter()),
                        exit = slideOutHorizontally(Motion.exit()) { it / 4 } + fadeOut(Motion.exit(Motion.QUICK))
                    ) {
                        RepeatEditorScreen(
                            vm = vm,
                            data = data,
                            templateId = lastRepeatId,
                            contentPadding = padding,
                            onBack = { repeatEditor = null }
                        )
                    }

                    AnimatedVisibility(
                        visible = showSettings,
                        enter = slideInHorizontally(Motion.enter()) { it / 4 } + fadeIn(Motion.enter()),
                        exit = slideOutHorizontally(Motion.exit()) { it / 4 } + fadeOut(Motion.exit(Motion.QUICK))
                    ) {
                        SettingsScreen(
                            vm = vm,
                            data = data,
                            contentPadding = padding,
                            onOpenTrash = { showTrash = true },
                            onStartGuide = { showSettings = false; guideStep = 0 },
                            onBack = { showSettings = false }
                        )
                    }

                    AnimatedVisibility(
                        visible = showTrash,
                        enter = slideInHorizontally(Motion.enter()) { it / 4 } + fadeIn(Motion.enter()),
                        exit = slideOutHorizontally(Motion.exit()) { it / 4 } + fadeOut(Motion.exit(Motion.QUICK))
                    ) {
                        TrashScreen(
                            vm = vm,
                            data = data,
                            contentPadding = padding,
                            onBack = { showTrash = false }
                        )
                    }

                    // Отрезок фокуса закончился — спрашиваем, продолжать ли эту цель.
                    // Ответ выбирает, что дальше: ещё один отрезок или перерыв. Вопрос
                    // приходит из движка, поэтому доживает до возвращения в приложение
                    // и виден с любой вкладки.
                    focusPrompt?.let { prompt ->
                        FocusResultDialog(
                            prompt = prompt,
                            onContinue = { vm.continueFocusTarget() },
                            onBreak = { vm.completeFocusTarget(prompt) }
                        )
                    }

                    // Кнопка «Закончить» останавливает таймер и спрашивает, что делать
                    // с уже наработанным: засчитать или уйти, будто отрезка не было.
                    stopPrompt?.let { prompt ->
                        StopFocusDialog(
                            prompt = prompt,
                            onRecord = { vm.confirmStop(record = true) },
                            onDiscard = { vm.confirmStop(record = false) },
                            onDismiss = { vm.dismissStopPrompt() }
                        )
                    }
                }

                guideStop?.let { stop ->
                    CoachOverlay(
                        title = stop.title,
                        text = stop.text,
                        target = coachTargets[stop.target],
                        index = guideStep,
                        total = guide.size,
                        onNext = {
                            if (guideStep + 1 < guide.size) guideStep++ else stopGuide()
                        },
                        onSkip = { stopGuide() }
                    )
                }
            }
        }
    }
}

/**
 * Ранний конец отрезка. Таймер уже стоит, и человек решает сам: засчитать
 * прошедшее время как полноценный отрезок или закрыть сессию без следа.
 * Закрытие диалога мимо кнопок ничего не меняет — таймер остаётся на паузе,
 * и его можно продолжить.
 */
@Composable
private fun StopFocusDialog(
    prompt: StopPrompt,
    onRecord: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.dialogEnter(),
        onDismissRequest = onDismiss,
        title = { Text(tr("Закончить раньше?")) },
        text = {
            Column {
                Text(
                    trf("В фокусе %1\$s", formatMinutes(prompt.minutes)),
                    style = MaterialTheme.typography.bodyLarge
                )
                prompt.targetTitle?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    tr("Записать это время в статистику или остановить сессию без записи?"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRecord) { Text(tr("Записать время")) }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) { Text(tr("Без записи")) }
        },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

/**
 * Итог отрезка фокуса. «Продолжить работу» ничего не меняет: таймер уже стоит
 * в начале следующего отрезка по той же цели. «На перерыв» — цель закрыта
 * (задача выполнена, привычке отметка), и человек уходит отдыхать: перерыв
 * нужен между делами, а не после каждого отрезка. Отрезок в статистику
 * записан в любом случае, поэтому закрытие мимо кнопок равносильно
 * «продолжить» — ничего не теряется.
 */
@Composable
private fun FocusResultDialog(
    prompt: FocusPrompt,
    onContinue: () -> Unit,
    onBreak: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.dialogEnter(),
        onDismissRequest = onContinue,
        title = { Text(tr("Продолжить?")) },
        text = {
            Column {
                Text(prompt.title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    trf("В фокусе %1\$s", formatMinutes(prompt.minutes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (prompt.isHabit) tr("На перерыв — привычка получит отметку.")
                    else tr("На перерыв — задача закроется."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onContinue) { Text(tr("Продолжить работу")) } },
        dismissButton = { TextButton(onClick = onBreak) { Text(tr("На перерыв")) } },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

/** Одна остановка вводного гида: какую вкладку открыть, куда светить, что сказать. */
private data class GuideStop(
    val tab: Tab?,
    val target: String?,
    val title: String,
    val text: String
)

/**
 * Вводный гид — только основы и по одному предложению на раздел: длинный
 * рассказ на первом запуске никто не читает. Первая и последняя остановки
 * без подсветки, поэтому карточка встаёт по центру экрана.
 */
private fun guideStops(): List<GuideStop> = listOf(
    GuideStop(
        tab = Tab.TASKS,
        target = null,
        title = tr("Добро пожаловать"),
        text = tr("Simply — задачи, привычки, помодоро и календарь. Всё хранится на телефоне: ни аккаунтов, ни сети. Покажу основное за минуту.")
    ),
    GuideStop(
        tab = Tab.TASKS,
        target = CoachKeys.tab(Tab.TASKS.name),
        title = tr("Задачи"),
        text = tr("Главный экран — один день. Неделя сверху, свайп вбок листает даты. Просроченное поднимается в сегодня, задачи без срока лежат в подвале дня.")
    ),
    GuideStop(
        tab = Tab.TASKS,
        target = CoachKeys.QUICK_ADD,
        title = tr("Быстрый ввод"),
        text = tr("Наберите название — задача уже в списке. Срок можно писать прямо в тексте: «завтра в 18:00». Кнопка справа откроет полный редактор.")
    ),
    GuideStop(
        tab = Tab.TASKS,
        target = CoachKeys.HEADER_ACTIONS,
        title = tr("Кнопки в заголовке"),
        text = tr("Поиск, шаблоны с повторами, проекты и настройки. У каждой задачи всегда есть проект — если не выбрать, будет «Общий».")
    ),
    GuideStop(
        tab = Tab.HABITS,
        target = CoachKeys.tab(Tab.HABITS.name),
        title = tr("Привычки"),
        text = tr("Недельная сетка с сериями. Касание кружка — плюс один за день, касание по полному — сброс, долгое нажатие — минус один.")
    ),
    GuideStop(
        tab = Tab.MATRIX,
        target = CoachKeys.tab(Tab.MATRIX.name),
        title = tr("Матрица"),
        text = tr("Важно и срочно — четыре квадранта. Новые задачи ждут во «Входящих» матрицы, пока вы не разложите их.")
    ),
    GuideStop(
        tab = Tab.FOCUS,
        target = CoachKeys.tab(Tab.FOCUS.name),
        title = tr("Фокус"),
        text = tr("Помодоро: выберите цель из задач и привычек и запустите отрезок. В конце спросим, продолжать или уйти на перерыв. Кнопка графика — статистика.")
    ),
    GuideStop(
        tab = Tab.CALENDAR,
        target = CoachKeys.tab(Tab.CALENDAR.name),
        title = tr("Календарь"),
        text = tr("Месяц целиком: точки задач, полоска привычек и список дел на выбранный день.")
    ),
    GuideStop(
        tab = Tab.TASKS,
        target = null,
        title = tr("Это всё"),
        text = tr("Тема, длительности помодоро, напоминания и копия данных — в настройках. Там же кнопка «Как пользоваться», если захотите пройти гид ещё раз.")
    )
)

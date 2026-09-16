package com.simply.app.ui.screens

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.FocusStats
import com.simply.app.data.ProjectStat
import com.simply.app.data.TargetStat
import com.simply.app.data.formatMinutes
import com.simply.app.data.fullName
import com.simply.app.data.dayMonthLabel
import com.simply.app.data.sessionsLabel
import com.simply.app.data.today
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.GroupedCard
import com.simply.app.ui.components.HabitIcons
import com.simply.app.ui.components.RowSeparator
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.SettingsRow
import com.simply.app.ui.components.SimplyCard
import com.simply.app.ui.theme.Accents
import java.time.LocalDate

@Composable
fun StatsScreen(
    data: AppData,
    contentPadding: PaddingValues,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit
) {
    val stats = remember(data.sessions) { FocusStats(data.sessions) }
    val projectStats = remember(data.sessions, data.projects) { stats.byProject(data.projects) }
    val week = remember(stats) { stats.minutesInRange(today().minusDays(6), today()) }
    val days = remember(stats) { stats.lastDays(14) }

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
            Column(Modifier.weight(1f)) {
                Text(tr("Статистика"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    tr("Сколько времени уходит на что"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SoftIconButton(Icons.Rounded.HistoryToggleOff, tr("История фокуса"), onOpenHistory)
        }

        if (data.sessions.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.BarChart,
                title = tr("Пока нечего считать"),
                subtitle = tr("Запусти таймер на вкладке «Фокус» — здесь появится разбор по задачам, проектам и дням.")
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp
            )
        ) {
            item { SectionTitle(tr("Время в фокусе")) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigStat(
                        value = formatMinutes(stats.minutesOn(today())),
                        label = tr("сегодня"),
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    BigStat(
                        value = formatMinutes(week),
                        label = tr("за 7 дней"),
                        accent = Accents.color(1),
                        modifier = Modifier.weight(1f)
                    )
                    BigStat(
                        value = formatMinutes(stats.totalMinutes),
                        label = tr("всего"),
                        accent = Accents.color(4),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { SectionTitle(tr("Последние 14 дней"), trailing = formatMinutes(days.sumOf { it.second })) }
            item { DaysChart(days) }

            if (stats.tasks.isNotEmpty()) {
                item { SectionTitle(tr("Задачи"), trailing = tr("по времени")) }
                item {
                    TargetList(
                        targets = stats.tasks.take(8),
                        max = stats.tasks.first().minutes,
                        colorFor = { t ->
                            Accents.color(
                                data.projects.firstOrNull { it.id == t.projectId }?.colorIndex ?: 0
                            )
                        },
                        subtitleFor = { t ->
                            val project = data.projects.firstOrNull { it.id == t.projectId }?.name
                            listOfNotNull(
                                sessionsLabel(t.sessions),
                                trf("в среднем %1\$s", formatMinutes(t.averageMinutes)),
                                project
                            ).joinToString(" · ")
                        }
                    )
                }
            }

            if (stats.habits.isNotEmpty()) {
                item { SectionTitle(tr("Привычки")) }
                item {
                    TargetList(
                        targets = stats.habits.take(6),
                        max = stats.habits.first().minutes,
                        colorFor = { t ->
                            Accents.color(
                                data.habits.firstOrNull { it.id == t.id }?.colorIndex ?: 1
                            )
                        },
                        subtitleFor = { t ->
                            trf("%1\$s · в среднем %2\$s", sessionsLabel(t.sessions), formatMinutes(t.averageMinutes))
                        },
                        iconFor = { t ->
                            data.habits.firstOrNull { it.id == t.id }?.icon
                        }
                    )
                }
            }

            if (projectStats.isNotEmpty()) {
                item { SectionTitle(tr("Проекты")) }
                item {
                    GroupedCard(Modifier.fillMaxWidth()) {
                        val max = projectStats.first().minutes.coerceAtLeast(1)
                        projectStats.forEachIndexed { index, p ->
                            ProjectRow(p, max)
                            if (index != projectStats.lastIndex) RowSeparator(startInset = 16.dp)
                        }
                    }
                }
            }

            item { SectionTitle(tr("Показатели")) }
            item {
                GroupedCard(Modifier.fillMaxWidth()) {
                    MetricRow(tr("Всего сессий"), stats.totalSessions.toString())
                    RowSeparator(startInset = 16.dp)
                    MetricRow(
                        tr("Доведено до конца"),
                        trf("%1\$d из %2\$d", stats.completedSessions, stats.totalSessions)
                    )
                    RowSeparator(startInset = 16.dp)
                    MetricRow(tr("Прервано вручную"), stats.interruptedSessions.toString())
                    RowSeparator(startInset = 16.dp)
                    MetricRow(tr("Средняя сессия"), formatMinutes(stats.averageSessionMinutes))
                    RowSeparator(startInset = 16.dp)
                    MetricRow(tr("Самая длинная сессия"), formatMinutes(stats.longestSessionMinutes))
                    RowSeparator(startInset = 16.dp)
                    MetricRow(tr("Дней с фокусом"), stats.activeDays.toString())
                    RowSeparator(startInset = 16.dp)
                    MetricRow(tr("Серия сейчас"), trf("%1\$d подряд", stats.currentStreak))
                    RowSeparator(startInset = 16.dp)
                    MetricRow(tr("Лучшая серия"), trf("%1\$d подряд", stats.bestStreak))
                    stats.bestWeekday?.let {
                        RowSeparator(startInset = 16.dp)
                        MetricRow(tr("Самый рабочий день"), it.fullName())
                    }
                    stats.busiestDay?.let { (date, minutes) ->
                        RowSeparator(startInset = 16.dp)
                        MetricRow(
                            tr("Рекорд за день"),
                            "${formatMinutes(minutes)} · ${date.dayMonthLabel()}"
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    SimplyCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Column {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Столбики по дням: высота — минуты, сегодня выделено. */
@Composable
private fun DaysChart(days: List<Pair<LocalDate, Int>>) {
    val max = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val accent = MaterialTheme.colorScheme.primary

    SimplyCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { (date, minutes) ->
                    val isToday = date == today()
                    val fraction = (minutes.toFloat() / max).coerceIn(0f, 1f)
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((6 + 108 * fraction).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        minutes == 0 -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        isToday -> accent
                                        else -> accent.copy(alpha = 0.45f)
                                    }
                                )
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                days.forEach { (date, _) ->
                    Text(
                        date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (date == today()) accent
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                days.first().first.dayMonthLabel() + " — " + days.last().first.dayMonthLabel() +
                    " · " + trf("максимум %1\$s в день", formatMinutes(max)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TargetList(
    targets: List<TargetStat>,
    max: Int,
    colorFor: @Composable (TargetStat) -> Color,
    subtitleFor: (TargetStat) -> String,
    iconFor: (TargetStat) -> String? = { null }
) {
    GroupedCard(Modifier.fillMaxWidth()) {
        targets.forEachIndexed { index, target ->
            val accent = colorFor(target)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = iconFor(target)
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(
                            HabitIcons.vector(icon),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        target.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitleFor(target),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(
                        fraction = target.minutes.toFloat() / max.coerceAtLeast(1),
                        color = accent
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    formatMinutes(target.minutes),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    maxLines = 1
                )
            }
            if (index != targets.lastIndex) RowSeparator(startInset = 58.dp)
        }
    }
}

@Composable
private fun ProjectRow(stat: ProjectStat, max: Int) {
    val accent = Accents.color(stat.project?.colorIndex ?: 0)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stat.project?.name?.let { tr(it) } ?: tr("Без проекта"),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(stat.minutes.toFloat() / max, accent)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            formatMinutes(stat.minutes),
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun MetricRow(title: String, value: String) {
    SettingsRow(
        title = title,
        trailing = {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

package com.simply.app.ui.screens

import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.Project
import com.simply.app.data.relativeLabel
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.GroupedCard
import com.simply.app.ui.components.ProjectIcons
import com.simply.app.ui.components.QuickAddBar
import com.simply.app.ui.components.RowSeparator
import com.simply.app.ui.components.SectionTitle
import com.simply.app.ui.components.quickAddBottomPadding
import com.simply.app.ui.theme.Accents

@Composable
fun ProjectsScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    onOpenProject: (String) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<Project?>(null) }

    val active = data.projects.filter { it.isActive }
    // Завершённые — сверху самые свежие: к последнему закрытому возвращаются чаще.
    val finished = data.projects.filter { it.completed }
        .sortedByDescending { it.completedAt ?: it.createdAt }

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
                Text(tr("Проекты"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    tr("У каждого свои разделы, задачи и прогресс"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (active.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.FolderOpen,
                        title = tr("Нет проектов"),
                        subtitle = tr("Проект собирает задачи одной темы и делится на разделы.")
                    )
                }
            }

            items(active.size) { index ->
                val project = active[index]
                ProjectCard(
                    project = project,
                    data = data,
                    onClick = { onOpenProject(project.id) }
                )
            }

            if (finished.isNotEmpty()) {
                item { SectionTitle(tr("Завершённые"), trailing = finished.size.toString()) }
                item {
                    GroupedCard(Modifier.fillMaxWidth()) {
                        finished.forEachIndexed { index, project ->
                            FinishedRow(
                                project = project,
                                count = data.tasks.count { it.projectId == project.id },
                                onRestore = { vm.setProjectCompleted(project.id, false) },
                                onClick = { onOpenProject(project.id) }
                            )
                            if (index != finished.lastIndex) RowSeparator(startInset = 56.dp)
                        }
                    }
                }
            }
        }

        QuickAddBar(
            onAdd = { name -> vm.addProject(name, data.projects.size % Accents.size) },
            onOpenDetails = { name ->
                editing = Project(
                    name = name,
                    colorIndex = data.projects.size % Accents.size
                )
            },
            placeholder = tr("Новый проект"),
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = quickAddBottomPadding(contentPadding) + 8.dp
            )
        )
    }

    editing?.let { project ->
        val isNew = data.projects.none { it.id == project.id }
        ProjectEditorSheet(
            project = project,
            isNew = isNew,
            taskCount = data.tasks.count { it.projectId == project.id },
            onDismiss = { editing = null },
            onSave = {
                if (isNew) {
                    val id = vm.addProject(it.name, it.colorIndex, it.icon)
                    if (id != null && it.note.isNotBlank()) {
                        vm.updateProject(
                            it.copy(id = id, note = it.note)
                        )
                    }
                } else {
                    vm.updateProject(it)
                }
                editing = null
            },
            onComplete = null,
            onDelete = null
        )
    }
}

@Composable
private fun ProjectCard(project: Project, data: AppData, onClick: () -> Unit) {
    val accent = Accents.color(project.colorIndex)
    val tasks = data.tasks.filter { it.projectId == project.id }
    val done = tasks.count { it.done }
    val open = tasks.size - done
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size

    com.simply.app.ui.components.SimplyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        ProjectIcons.vector(project.icon),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        tr(project.name),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        buildString {
                            append(trf("Активных %1\$d", open))
                            if (done > 0) append(trf(" · выполнено %1\$d", done))
                            if (project.sections.isNotEmpty()) {
                                append(trf(" · разделов %1\$d", project.sections.size))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (project.note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    project.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (tasks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                ProgressLine(progress, accent)
            }
        }
    }
}

@Composable
fun ProgressLine(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Строка завершённого проекта: что в нём было и когда его закрыли. */
@Composable
private fun FinishedRow(
    project: Project,
    count: Int,
    onRestore: () -> Unit,
    onClick: () -> Unit
) {
    val accent = Accents.color(project.colorIndex)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ProjectIcons.vector(project.icon),
                contentDescription = null,
                tint = accent.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tr(project.name), style = MaterialTheme.typography.bodyLarge)
            Text(
                buildString {
                    append(com.simply.app.data.tasksLabel(count))
                    project.completedAt?.let {
                        append(" · ")
                        append(trf("завершён %1\$s", finishedLabel(it)))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        androidx.compose.material3.TextButton(onClick = onRestore) { Text(tr("Вернуть")) }
    }
}

/** «завершён вчера», «завершён 12 августа» — та же подпись, что у задач. */
private fun finishedLabel(at: Long): String =
    java.time.Instant.ofEpochMilli(at)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .relativeLabel()
        .lowercase(com.simply.app.data.RU)

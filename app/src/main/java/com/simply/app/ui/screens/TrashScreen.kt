package com.simply.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simply.app.data.AppData
import com.simply.app.data.TRASH_DAYS
import com.simply.app.data.dayMonthLabel
import com.simply.app.data.tasksLabel
import com.simply.app.ui.AppViewModel
import com.simply.app.ui.components.EmptyState
import com.simply.app.ui.components.GroupedCard
import com.simply.app.ui.components.RowSeparator
import com.simply.app.ui.i18n.tr
import com.simply.app.ui.i18n.trf
import java.time.Instant
import java.time.ZoneId

/**
 * Корзина: удалённые задачи лежат здесь [TRASH_DAYS] дней,
 * потом исчезают сами. Отсюда их можно вернуть или добить.
 */
@Composable
fun TrashScreen(
    vm: AppViewModel,
    data: AppData,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val items = data.trash.sortedByDescending { it.deletedAt ?: 0L }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = contentPadding.calculateTopPadding())
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = tr("Назад"))
            }
            Column(Modifier.weight(1f)) {
                Text(tr("Корзина"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    trf("Удалённое хранится %1\$d дней", TRASH_DAYS.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (items.isNotEmpty()) {
                TextButton(onClick = { vm.clearTrash() }) {
                    Text(tr("Очистить"), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.DeleteOutline,
                title = tr("Корзина пуста"),
                subtitle = tr("Удалённые задачи попадают сюда и ждут 30 дней.")
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            )
        ) {
            item {
                Text(
                    tasksLabel(items.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            item {
                GroupedCard(Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, task ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    deletedLabel(task.deletedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { vm.restoreFromTrash(task.id) }) {
                                Icon(
                                    Icons.Rounded.Restore,
                                    contentDescription = tr("Вернуть"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { vm.purgeFromTrash(task.id) }) {
                                Icon(
                                    Icons.Rounded.DeleteForever,
                                    contentDescription = tr("Удалить навсегда"),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (index != items.lastIndex) RowSeparator(startInset = 16.dp)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun deletedLabel(deletedAt: Long?): String {
    if (deletedAt == null) return tr("Удалена")
    val date = Instant.ofEpochMilli(deletedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    return trf("Удалена %1\$s", date.dayMonthLabel())
}

package com.simply.app.ui.components

import com.simply.app.ui.i18n.tr
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * Строка быстрого добавления: ввёл название — задача уже в списке.
 * Срок, проект и квадрант можно проставить позже, открыв задачу,
 * либо сразу через кнопку «детали».
 */
@Composable
fun QuickAddBar(
    onAdd: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = tr("Новая задача")
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val canSubmit = text.isNotBlank()
    val shape = RoundedCornerShape(26.dp)

    fun submit() {
        if (!canSubmit) return
        onAdd(text.trim())
        text = ""
    }

    // Черновик уезжает в расширенный редактор, поэтому строку освобождаем сразу:
    // иначе после сохранения (или отмены) текст остался бы висеть в быстром вводе.
    fun openDetails() {
        val draft = text.trim()
        text = ""
        onOpenDetails(draft)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .coachTarget(CoachKeys.QUICK_ADD)
            .softSurface(shape, MaterialTheme.colorScheme.surfaceContainerLow, elevation = 10.dp)
            .height(56.dp)
            .padding(start = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .clickable { focusRequester.requestFocus() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }

        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (text.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { openDetails() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription = tr("Детали задачи"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(
            visible = canSubmit,
            enter = scaleIn(initialScale = 0.7f) + fadeIn(),
            exit = scaleOut(targetScale = 0.7f) + fadeOut()
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { submit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = tr("Добавить"),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (!canSubmit) Spacer(Modifier.width(2.dp))
    }
}

/** Отступ снизу для строки быстрого ввода: над таб-баром, а при вводе — над клавиатурой. */
@Composable
fun quickAddBottomPadding(contentPadding: androidx.compose.foundation.layout.PaddingValues): androidx.compose.ui.unit.Dp {
    val ime = androidx.compose.foundation.layout.WindowInsets.ime
        .asPaddingValues()
        .calculateBottomPadding()
    return maxOf(ime, contentPadding.calculateBottomPadding())
}

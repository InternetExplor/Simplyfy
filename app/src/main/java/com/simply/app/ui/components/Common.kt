package com.simply.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simply.app.ui.i18n.tr
import com.simply.app.data.Project
import com.simply.app.ui.theme.LocalIsDark
import com.simply.app.ui.theme.Accents
import com.simply.app.ui.theme.Motion

/** Мягкая тень вместо жёсткой рамки — на светлой теме, тонкая рамка — на тёмной. */
@Composable
fun Modifier.softSurface(
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    elevation: Dp = 6.dp
): Modifier {
    val dark = LocalIsDark.current
    return this
        .then(
            if (dark) Modifier
            else Modifier.shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = Color(0x0F000000),
                spotColor = Color(0x14000000)
            )
        )
        .clip(shape)
        .background(color)
        .then(
            if (dark) Modifier.border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                shape
            ) else Modifier
        )
}

/** Базовая карточка приложения. */
@Composable
fun SimplyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape: Shape = RoundedCornerShape(22.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .softSurface(shape, color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding)
    ) { content() }
}

/**
 * Сгруппированный список в стиле iOS: один блок со скруглением,
 * строки внутри разделены волосяной линией.
 */
@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(modifier = modifier.softSurface(shape, color), content = content)
}

/** Волосяной разделитель с отступом слева — как в системных списках iOS. */
@Composable
fun RowSeparator(startInset: Dp = 20.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier.coachTarget(CoachKeys.HEADER_ACTIONS),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) { trailing() }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Мягкий чип-фильтр. */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    count: Int? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    tonal: Boolean = false
) {
    val targetBg = when {
        selected && tonal -> accent.copy(alpha = 0.16f)
        selected -> accent
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val targetFg = when {
        selected && tonal -> accent
        selected -> contrastOn(accent)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bg by animateColorAsState(targetBg, label = "pillBg")
    val fg by animateColorAsState(targetFg, label = "pillFg")

    Row(
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
            maxLines = 1
        )
        if (count != null && count > 0) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = fg.copy(alpha = 0.65f)
            )
        }
    }
}

/** Круглый чекбокс с пружинной анимацией. */
@Composable
fun CheckCircle(
    checked: Boolean,
    onClick: () -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 25.dp,
    /** Что читает голосовой доступ: обычно название задачи. */
    label: String? = null
) {
    // Отметка разложена на три такта, иначе всё случалось в один кадр и
    // читалось как мигание: круг наливается, галочка догоняет с перелётом,
    // вокруг расходится кольцо — короткий отзвук нажатия.
    val fill by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (checked) Motion.enter(Motion.CHECK) else Motion.exit(Motion.QUICK),
        label = "checkFill"
    )
    val mark by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (checked) Motion.pop(Motion.CHECK - CHECK_MARK_DELAY)
        else Motion.exit(Motion.QUICK / 2),
        label = "checkMark"
    )
    val borderColor by animateColorAsState(
        if (checked) accent else MaterialTheme.colorScheme.outline,
        animationSpec = Motion.enter(Motion.CHECK),
        label = "checkBorder"
    )

    // Кольцо живёт только на переходе в «выполнено»: на первой отрисовке уже
    // отмеченной задачи (прокрутка списка) вспыхивать оно не должно.
    val halo = remember { Animatable(1f) }
    var wasChecked by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        if (checked == wasChecked) return@LaunchedEffect
        wasChecked = checked
        if (!checked) {
            halo.snapTo(1f)
            return@LaunchedEffect
        }
        halo.snapTo(0f)
        halo.animateTo(1f, tween(Motion.CHECK + Motion.QUICK, easing = Motion.EaseEnter))
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp),
                role = Role.Checkbox,
                onClick = onClick
            )
            .then(
                if (label == null) Modifier
                else Modifier.semantics { contentDescription = label }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    val ring = halo.value
                    if (ring >= 1f) return@drawBehind
                    val radius = this.size.minDimension / 2f
                    drawCircle(
                        color = accent,
                        radius = radius * (1f + ring * 0.75f),
                        alpha = (1f - ring) * 0.28f
                    )
                }
                .clip(CircleShape)
                .border(1.8.dp * (1f - fill), borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Заливка не просто проявляется, а вырастает из центра —
            // так кружок «закрывается», а не меняет цвет.
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val grow = 0.55f + 0.45f * fill
                        scaleX = grow
                        scaleY = grow
                        alpha = fill
                    }
                    .clip(CircleShape)
                    .background(accent)
            )
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = contrastOn(accent),
                modifier = Modifier
                    .size(size * 0.66f)
                    .scale(mark)
                    .alpha(mark.coerceIn(0f, 1f))
            )
        }
    }
}

/** Пауза между заливкой кружка и появлением галочки. */
private const val CHECK_MARK_DELAY = 110

/**
 * Выбор проекта: строка поиска, под ней — сами проекты.
 *
 * Раньше проекты лежали в ленте с горизонтальной прокруткой: пока их три,
 * это удобно, а на двадцати нужный уезжает за край и его нельзя найти иначе,
 * чем листая. Поиск отвечает на вопрос «где мой проект» за одно слово,
 * а сетка с переносом показывает всё разом, не пряча ничего вбок.
 *
 * Выбранный проект показывается всегда, даже если не подходит под запрос —
 * иначе непонятно, что вообще выбрано.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectPicker(
    projects: List<Project>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    /** Подпись варианта «без проекта». null — выбор проекта обязателен. */
    noneLabel: String? = null,
    /** Порог, ниже которого поиск только мешает. */
    searchFrom: Int = 5
) {
    var query by remember { mutableStateOf("") }
    val clean = query.trim()
    val shown = remember(projects, clean, selectedId) {
        if (clean.isEmpty()) projects
        else projects.filter {
            it.id == selectedId || tr(it.name).contains(clean, ignoreCase = true)
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (projects.size >= searchFrom) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(tr("Поиск проекта")) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (clean.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = tr("Очистить"),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (shown.isEmpty() && noneLabel == null) {
            Text(
                tr("Ничего не найдено"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (noneLabel != null) {
                    FilterPill(noneLabel, selectedId == null, { onSelect(null) })
                }
                shown.forEach { p ->
                    FilterPill(
                        tr(p.name),
                        selectedId == p.id,
                        { onSelect(p.id) },
                        accent = Accents.color(p.colorIndex)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    SimplyCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Column {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Строка настроек: заголовок, необязательное описание и контрол справа. */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = iconTint,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = Color.White,
                    checkedBorderColor = Color.Transparent
                )
            )
        }
    )
}

/**
 * Выбор значка. По умолчанию свёрнут: пять самых ходовых значков и кнопка,
 * которая раскрывает полную сетку — иначе список значков занимал бы пол-экрана.
 * Выбранный значок виден всегда, даже если его нет среди общих.
 */
@Composable
fun IconPickerGrid(
    keys: List<String>,
    common: List<String>,
    selected: String,
    accent: Color,
    vectorFor: (String) -> ImageVector,
    onPick: (String) -> Unit,
    columns: Int = 6
) {
    var expanded by remember { mutableStateOf(false) }
    val shortList = remember(common, selected) {
        if (selected in common) common.take(5)
        else listOf(selected) + common.filter { it != selected }.take(4)
    }
    // null в списке — ячейка с кнопкой «развернуть/свернуть».
    val cells: List<String?> = (if (expanded) keys else shortList) + listOf(null)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(columns).forEach { rowCells ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCells.forEach { key ->
                    if (key == null) {
                        IconCell(
                            modifier = Modifier.weight(1f),
                            icon = if (expanded) Icons.Rounded.ExpandLess
                            else Icons.Rounded.MoreHoriz,
                            description = if (expanded) tr("Свернуть") else tr("Больше значков"),
                            picked = false,
                            accent = accent,
                            // Кнопка, а не значок: обводка вместо заливки.
                            outlined = true,
                            onClick = { expanded = !expanded }
                        )
                    } else {
                        IconCell(
                            modifier = Modifier.weight(1f),
                            icon = vectorFor(key),
                            description = null,
                            picked = key == selected,
                            accent = accent,
                            onClick = { onPick(key) }
                        )
                    }
                }
                // Хвост последней строки, чтобы значки не растягивались.
                repeat(columns - rowCells.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun IconCell(
    modifier: Modifier,
    icon: ImageVector,
    description: String?,
    picked: Boolean,
    accent: Color,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                when {
                    outlined -> accent.copy(alpha = 0.07f)
                    picked -> accent.copy(alpha = 0.22f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .then(
                if (outlined) Modifier.border(1.5.dp, accent.copy(alpha = 0.45f), shape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (picked || outlined) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (outlined) 22.dp else 20.dp)
        )
    }
}

/** Читаемый цвет текста поверх произвольного акцента. */
fun contrastOn(background: Color): Color {
    val luminance = background.red * 0.299f + background.green * 0.587f + background.blue * 0.114f
    return if (luminance > 0.6f) Color(0xFF12121A) else Color.White
}

@Composable
fun LabeledRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

/**
 * Переименование на месте: одно поле и «Готово». Живёт рядом со списком,
 * чтобы менять название не приходилось через полноэкранный редактор.
 */
@Composable
fun RenameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    confirmLabel: String = tr("Готово"),
    /** Удаление прямо из диалога переименования: незачем искать отдельное меню. */
    onDelete: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        modifier = Modifier.dialogEnter(),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isNotBlank()) onConfirm(text.trim())
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            if (onDelete == null) TextButton(onClick = onDismiss) { Text(tr("Отмена")) }
            else TextButton(onClick = onDelete) {
                Text(tr("Удалить"), color = MaterialTheme.colorScheme.error)
            }
        },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

/**
 * Появление диалога: системный AlertDialog возникает одним кадром, что и
 * читалось как «резкие переходы». Отдаём его поверхности лёгкий наплыв —
 * уход оставляем мгновенным, окно диалога всё равно снимается сразу.
 */
@Composable
fun Modifier.dialogEnter(): Modifier {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        if (shown) 1f else 0.9f,
        animationSpec = Motion.settle(),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        if (shown) 1f else 0f,
        animationSpec = Motion.enter(Motion.QUICK),
        label = "dialogAlpha"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

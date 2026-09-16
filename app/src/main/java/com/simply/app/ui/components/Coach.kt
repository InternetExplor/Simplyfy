package com.simply.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.simply.app.ui.i18n.tr

/**
 * Куда светит гид. Экраны сами отмечают свои места `Modifier.coachTarget("ключ")`,
 * а обзорный слой берёт координаты по ключу — так подсветка попадает точно
 * в живой элемент и не разъезжается на других экранах и размерах шрифта.
 */
class CoachTargets {
    private val rects = mutableStateMapOf<String, Pair<Any, Rect>>()

    operator fun get(key: String?): Rect? {
        if (key == null) return null
        return rects[key]?.second
    }

    fun put(key: String, owner: Any, rect: Rect) {
        rects[key] = owner to rect
    }

    /**
     * Элемент ушёл с экрана — забываем координаты, чтобы не светить в пустоту.
     * Чужую запись не трогаем: при переключении вкладок новый экран успевает
     * занять тот же ключ раньше, чем старый уходит из композиции.
     */
    fun forget(key: String, owner: Any) {
        if (rects[key]?.first === owner) rects.remove(key)
    }
}

val LocalCoachTargets = staticCompositionLocalOf { CoachTargets() }

/** Ключи мест, на которые светит гид. */
object CoachKeys {
    const val QUICK_ADD = "quick_add"
    const val HEADER_ACTIONS = "header_actions"

    /** Кнопка вкладки в нижней панели. */
    fun tab(name: String) = "tab_$name"
}

/** Отметить элемент как остановку гида. */
@Composable
fun Modifier.coachTarget(key: String): Modifier {
    val targets = LocalCoachTargets.current
    val owner = remember { Any() }
    DisposableEffect(key) { onDispose { targets.forget(key, owner) } }
    return onGloballyPositioned { targets.put(key, owner, it.boundsInRoot()) }
}

/**
 * Слой гида: всё темнеет, кроме одного места, и рядом всплывает объяснение.
 * Лежит поверх всего приложения, поэтому касания под ним не проходят —
 * пока идёт рассказ, случайно нажать нечего. Касание в любом месте —
 * следующий шаг. Без цели (`target == null`) экран просто затемнён,
 * а карточка встаёт по центру: так начинается и заканчивается тур.
 */
@Composable
fun CoachOverlay(
    title: String,
    text: String,
    target: Rect?,
    index: Int,
    total: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val scrim = Color.Black.copy(alpha = 0.76f)
    val ring = MaterialTheme.colorScheme.primary
    val inset = with(density) { 10.dp.toPx() }
    val maxRadius = with(density) { 28.dp.toPx() }
    val strokeWidth = with(density) { 2.dp.toPx() }
    val hole = target?.let {
        Rect(it.left - inset, it.top - inset, it.right + inset, it.bottom + inset)
    }

    // Мягкая пульсация рамки: подсказывает, что подсвеченное место живое.
    val pulse by rememberInfiniteTransition(label = "coachPulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "coachRing"
    )

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(index) { detectTapGestures { onNext() } }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (hole == null) {
                drawRect(scrim)
                return@Canvas
            }
            val radius = minOf(hole.height / 2f, hole.width / 2f, maxRadius)
            val path = Path().apply {
                addRoundRect(RoundRect(hole, CornerRadius(radius, radius)))
            }
            clipPath(path, ClipOp.Difference) { drawRect(scrim) }
            drawPath(path, color = ring.copy(alpha = pulse), style = Stroke(strokeWidth))
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenHeight = constraints.maxHeight.toFloat()
            // Карточка встаёт с той стороны, где больше места: подсветку
            // внизу экрана она не должна закрывать собой.
            val below = hole != null && hole.center.y < screenHeight * 0.45f
            when {
                hole == null -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CoachCard(title, text, index, total, onNext, onSkip) }

                below -> Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(with(density) { hole.bottom.toDp() } + 16.dp))
                    CoachCard(title, text, index, total, onNext, onSkip)
                }

                else -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height((with(density) { hole.top.toDp() } - 16.dp).coerceAtLeast(0.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.BottomStart
                ) { CoachCard(title, text, index, total, onNext, onSkip) }
            }
        }
    }
}

@Composable
private fun CoachCard(
    title: String,
    text: String,
    index: Int,
    total: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 10.dp
    ) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1} / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (index + 1 < total) {
                    TextButton(onClick = onSkip) { Text(tr("Пропустить")) }
                    Spacer(Modifier.width(4.dp))
                }
                Button(
                    onClick = onNext,
                    shape = MaterialTheme.shapes.large
                ) { Text(if (index + 1 < total) tr("Дальше") else tr("Готово")) }
            }
        }
    }
}

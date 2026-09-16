package com.simply.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize

/**
 * Общий словарь движения. Раньше каждая анимация задавала свою длительность
 * и свою пружину, и от этого интерфейс дёргался: где-то отклик был мгновенный,
 * где-то пружина отскакивала. Здесь три правила на всё приложение:
 *
 * - приход длиннее ухода: то, что появляется, читают, а то, что уходит, — уже нет;
 * - разгон мягкий, торможение долгое — движение выкатывается, а не щёлкает;
 * - пружины без отскока: доезжают и останавливаются.
 */
object Motion {

    /** Мелкий отклик: галочка, плашка, счётчик. */
    const val QUICK = 220

    /** Основной переход: экран, день, вкладка. */
    const val CALM = 320

    /** Уход — всегда короче прихода. */
    const val SETTLE = 240

    /**
     * Отметка выполнения — единственное место, где движение намеренно длиннее
     * обычного: это ответ на действие, и его хочется досмотреть. Кружок
     * наливается CHECK, держится HOLD, и только потом строка уходит.
     */
    const val CHECK = 420

    /** Сколько галочка живёт одна, прежде чем строка начнёт уходить. */
    const val HOLD = 300

    /** Мягкий выкат: быстро тронулся, долго тормозит. */
    val EaseEnter: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Уход: трогается плавно, дальше не мешает. */
    val EaseExit: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /**
     * Лёгкий перелёт с возвратом — только для подтверждения действия.
     * Перебирает цель примерно на 12%, поэтому значение может выйти за 1f:
     * там, где это альфа, его надо зажимать.
     */
    val EaseBack: Easing = CubicBezierEasing(0.34f, 1.5f, 0.5f, 1f)

    fun <T> enter(duration: Int = CALM): FiniteAnimationSpec<T> =
        tween(duration, easing = EaseEnter)

    fun <T> exit(duration: Int = SETTLE): FiniteAnimationSpec<T> =
        tween(duration, easing = EaseExit)

    /** Подтверждение: наливается с перелётом и садится обратно. */
    fun <T> pop(duration: Int = CHECK): FiniteAnimationSpec<T> =
        tween(duration, easing = EaseBack)

    /** Пружина без дребезга — для того, что едет к своему месту. */
    fun <T> settle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

    /** Смена высоты блока: список закрывает просвет, а не схлопывает его. */
    val size: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntSize.VisibilityThreshold
    )
}

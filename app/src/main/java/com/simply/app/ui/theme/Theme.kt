package com.simply.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.simply.app.data.ThemeMode

/** Акцентная тема приложения — выбирается в настройках. */
enum class AccentTheme(
    val title: String,
    val lightPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
) {
    INDIGO("Индиго", Color(0xFF5B5BD6), Color(0xFFE6E6FF), Color(0xFF23238A),
        Color(0xFFAEB6FF), Color(0xFF3B44B8), Color(0xFFE7E9FF)),
    OCEAN("Океан", Color(0xFF0A7AEA), Color(0xFFDCEBFF), Color(0xFF00375F),
        Color(0xFF8CC8FF), Color(0xFF10537F), Color(0xFFDCEBFF)),
    MINT("Мята", Color(0xFF13A17C), Color(0xFFCDF3E6), Color(0xFF00382A),
        Color(0xFF74E0C0), Color(0xFF045B48), Color(0xFFCDF3E6)),
    SUNSET("Закат", Color(0xFFDD6B20), Color(0xFFFFE3CE), Color(0xFF4A1D00),
        Color(0xFFFFBA8A), Color(0xFF7E3A0A), Color(0xFFFFE3CE)),
    PLUM("Слива", Color(0xFF8E4EC6), Color(0xFFF0E3FF), Color(0xFF3A0B63),
        Color(0xFFD9B6FF), Color(0xFF5E3391), Color(0xFFF0E3FF)),
    ROSE("Роза", Color(0xFFD6336C), Color(0xFFFFE1EA), Color(0xFF5A0026),
        Color(0xFFFFA6C2), Color(0xFF8E2050), Color(0xFFFFE1EA));

    companion object {
        fun at(index: Int): AccentTheme = entries[((index % entries.size) + entries.size) % entries.size]
    }
}

/** Мягкие «бумажные» поверхности в духе iOS: светло-серый фон и белые карточки. */
private fun lightScheme(a: AccentTheme) = lightColorScheme(
    primary = a.lightPrimary,
    onPrimary = Color.White,
    primaryContainer = a.lightContainer,
    onPrimaryContainer = a.lightOnContainer,
    secondary = Color(0xFF6C6C74),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E9EF),
    onSecondaryContainer = Color(0xFF2A2A30),
    tertiary = Color(0xFF13A17C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDF3E6),
    onTertiaryContainer = Color(0xFF00382A),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFFE3E3),
    onErrorContainer = Color(0xFF5C0B0B),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE9E9EF),
    onSurfaceVariant = Color(0xFF83838C),
    outline = Color(0xFFC4C4CC),
    outlineVariant = Color(0xFFE4E4EA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF7F7FA),
    surfaceContainerHigh = Color(0xFFEBEBF0),
    surfaceContainerHighest = Color(0xFFE4E4EA),
    inverseSurface = Color(0xFF2E2E33),
    inverseOnSurface = Color(0xFFF5F5F8),
    scrim = Color(0xFF000000)
)

/** Тёмная тема — глубокий сине-графитовый «midnight»: мягче чистого чёрного и не давит контрастом. */
private fun darkScheme(a: AccentTheme) = darkColorScheme(
    primary = a.darkPrimary,
    onPrimary = Color(0xFF141826),
    primaryContainer = a.darkContainer,
    onPrimaryContainer = a.darkOnContainer,
    secondary = Color(0xFFB4BDD0),
    onSecondary = Color(0xFF232A38),
    secondaryContainer = Color(0xFF333C4D),
    onSecondaryContainer = Color(0xFFE1E7F3),
    tertiary = Color(0xFF74E0C0),
    onTertiary = Color(0xFF00352A),
    tertiaryContainer = Color(0xFF015140),
    onTertiaryContainer = Color(0xFFC6F5E7),
    error = Color(0xFFFF8A93),
    onError = Color(0xFF3F0710),
    errorContainer = Color(0xFF7A2230),
    onErrorContainer = Color(0xFFFFDDE1),
    background = Color(0xFF12151E),
    onBackground = Color(0xFFE6EAF3),
    surface = Color(0xFF12151E),
    onSurface = Color(0xFFE6EAF3),
    surfaceVariant = Color(0xFF2A3140),
    onSurfaceVariant = Color(0xFF98A1B5),
    outline = Color(0xFF4E5768),
    outlineVariant = Color(0xFF2E3644),
    surfaceContainerLowest = Color(0xFF0D1018),
    surfaceContainerLow = Color(0xFF1B202C),
    surfaceContainer = Color(0xFF202634),
    surfaceContainerHigh = Color(0xFF28303F),
    surfaceContainerHighest = Color(0xFF313A4B),
    inverseSurface = Color(0xFFE6EAF3),
    inverseOnSurface = Color(0xFF232A38),
    scrim = Color(0xFF000000)
)

/** Акценты для проектов и привычек — мягкие, читаемые на обоих фонах. */
object Accents {
    private val light = listOf(
        Color(0xFF5B5BD6),
        Color(0xFF13A17C),
        Color(0xFFDD6B20),
        Color(0xFFD6336C),
        Color(0xFF0A7AEA),
        Color(0xFF8E4EC6)
    )
    private val dark = listOf(
        Color(0xFFAEB6FF),
        Color(0xFF74E0C0),
        Color(0xFFFFBA8A),
        Color(0xFFFFA6C2),
        Color(0xFF8CC8FF),
        Color(0xFFD9B6FF)
    )

    val size: Int get() = light.size

    @Composable
    fun color(index: Int): Color {
        val palette = if (LocalIsDark.current) dark else light
        return palette[((index % palette.size) + palette.size) % palette.size]
    }
}

val LocalIsDark = staticCompositionLocalOf { false }

private val SimplyTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Normal),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(letterSpacing = (-0.1).sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
    )
}

private val SimplyShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun SimplyTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accentIndex: Int = 0,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = AccentTheme.at(accentIndex)
    val context = LocalContext.current
    // Material You: цвета берутся из обоев, наши остаются запасным вариантом.
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        dynamic && darkTheme -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        darkTheme -> darkScheme(accent)
        else -> lightScheme(accent)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colors,
            typography = SimplyTypography,
            shapes = SimplyShapes,
            content = content
        )
    }
}

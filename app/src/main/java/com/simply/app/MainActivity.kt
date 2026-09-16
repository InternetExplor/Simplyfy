package com.simply.app

import android.content.res.Configuration
import androidx.core.graphics.drawable.toDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.simply.app.widget.Shortcuts
import com.simply.app.ui.SimplyApp

class MainActivity : ComponentActivity() {

    /** Действие, с которым приложение открыли: ярлык или виджет. */
    private val launchAction = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applySavedWindowBackground()
        // Полям приложения автозаполнение не нужно, а системная служба
        // иногда отвечает по несколько секунд и подвешивает ввод.
        window.decorView.importantForAutofill =
            View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        launchAction.value = intent?.action
        Shortcuts.install(this)
        setContent {
            SimplyApp(
                launchAction = launchAction.value,
                onLaunchActionHandled = { launchAction.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchAction.value = intent.action
    }

    companion object {
        const val ACTION_NEW_TASK = "com.simply.app.NEW_TASK"
        const val ACTION_START_FOCUS = "com.simply.app.START_FOCUS"
    }

    /**
     * Тема приложения может отличаться от системной, а фон окна выбирается
     * системой до старта Compose. Подкрашиваем его сами по сохранённому выбору,
     * иначе на кадр мелькает светлый фон в тёмной теме.
     */
    private fun applySavedWindowBackground() {
        val prefs = getSharedPreferences(ThemePrefs.NAME, MODE_PRIVATE)
        val mode = prefs.getString(ThemePrefs.KEY_MODE, null)
        val dark = when (mode) {
            "LIGHT" -> false
            "DARK" -> true
            else -> resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        window.setBackgroundDrawable(
            (if (dark) 0xFF12151E.toInt() else 0xFFF2F2F7.toInt()).toDrawable()
        )
    }
}

/** Зеркало выбранной темы в SharedPreferences — читается до старта Compose. */
object ThemePrefs {
    const val NAME = "simply_theme"
    const val KEY_MODE = "mode"
}

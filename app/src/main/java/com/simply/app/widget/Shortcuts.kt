package com.simply.app.widget

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.simply.app.MainActivity
import com.simply.app.R
import com.simply.app.ui.i18n.tr

/**
 * Быстрые действия по долгому нажатию на иконку приложения.
 * Ставятся из кода, а не из XML: так они работают и в debug-сборке,
 * у которой другой applicationId.
 */
object Shortcuts {

    fun install(context: Context) {
        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(
                context,
                listOf(
                    shortcut(
                        context,
                        id = "new_task",
                        label = tr("Новая задача"),
                        icon = R.drawable.ic_shortcut_task,
                        action = MainActivity.ACTION_NEW_TASK
                    ),
                    shortcut(
                        context,
                        id = "focus",
                        label = tr("Фокус"),
                        icon = R.drawable.ic_shortcut_focus,
                        action = MainActivity.ACTION_START_FOCUS
                    )
                )
            )
        }
    }

    private fun shortcut(
        context: Context,
        id: String,
        label: String,
        icon: Int,
        action: String
    ): ShortcutInfoCompat = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(label)
        .setLongLabel(label)
        .setIcon(IconCompat.createWithResource(context, icon))
        .setIntent(
            Intent(context, MainActivity::class.java)
                .setAction(action)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        .build()
}

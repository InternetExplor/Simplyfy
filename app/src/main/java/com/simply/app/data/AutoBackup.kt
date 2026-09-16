package com.simply.app.data

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

/**
 * Автокопия: раз в неделю кладём тот же JSON, что и ручной экспорт,
 * в выбранную пользователем папку. Работает при запуске приложения —
 * так не нужны ни фоновые сервисы, ни особые разрешения.
 */
object AutoBackup {

    private const val WEEK_MILLIS = 7L * 24 * 3600_000L

    fun isDue(settings: UiSettings, now: Long = System.currentTimeMillis()): Boolean =
        settings.autoBackup &&
            settings.backupFolder != null &&
            now - settings.lastBackupAt >= WEEK_MILLIS

    /** Возвращает время сохранения или null, если записать не удалось. */
    fun write(context: Context, folderUri: String, json: String): Long? = runCatching {
        val tree = DocumentFile.fromTreeUri(context, folderUri.toUri()) ?: return null
        if (!tree.canWrite()) return null
        val name = "simply-backup-${today().iso()}.json"
        tree.findFile(name)?.delete()
        val file = tree.createFile("application/json", name) ?: return null
        context.contentResolver.openOutputStream(file.uri)?.use { out ->
            out.write(json.toByteArray())
        } ?: return null
        System.currentTimeMillis()
    }.getOrNull()
}

package com.simply.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Всё состояние приложения — один JSON-файл в filesDir.
 * Для личного трекера этого более чем достаточно: никакой БД, миграций и фоновых сервисов.
 */
class Repository private constructor(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val writeLock = Mutex()

    private val file = File(context.filesDir, FILE_NAME)
    private val tmpFile = File(context.filesDir, "$FILE_NAME.tmp")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    /** Последний снимок, который реально лежит в файле. */
    @Volatile
    private var lastPersisted: AppData = _data.value

    init {
        // Автосохранение с задержкой, чтобы не писать файл на каждое нажатие.
        // Сравниваем со снимком, а не пропускаем первое значение: подписчик
        // стартует не мгновенно, и правки на старте (миграция данных, демо-контент)
        // иначе не попадали бы в файл до следующего изменения.
        _data
            .debounce(300)
            .onEach { if (it != lastPersisted) persist(it) }
            .launchIn(scope)
    }

    fun update(transform: (AppData) -> AppData) {
        _data.value = transform(_data.value)
    }

    private fun load(): AppData {
        val raw = runCatching { if (file.exists()) file.readText() else null }.getOrNull()
            ?: return AppData()
        return runCatching { json.decodeFromString<AppData>(raw) }.getOrElse { AppData() }
    }

    private suspend fun persist(data: AppData) = withContext(Dispatchers.IO) {
        // Замок нужен, потому что автосохранение и принудительный flush()
        // могут прийти одновременно — иначе они пишут в один временный файл.
        writeLock.withLock {
            runCatching {
                // Пишем во временный файл и переименовываем — так данные не потеряются при сбое.
                val text = json.encodeToString(data)
                tmpFile.writeText(text)
                lastPersisted = data
                if (!tmpFile.renameTo(file)) {
                    // Переименование не прошло — пишем напрямую, чтобы не остаться
                    // вообще без файла: удалять старый до успешной записи нельзя.
                    file.writeText(text)
                    tmpFile.delete()
                }
            }
        }
    }

    /** Принудительное сохранение — вызывается, когда приложение уходит в фон. */
    suspend fun flush() = persist(_data.value)

    /** Резервная копия: тот же JSON, что лежит в файле. */
    fun exportJson(): String = json.encodeToString(_data.value)

    /** Восстановление из копии. Возвращает false, если файл не разобрался. */
    fun importJson(raw: String): Boolean {
        val parsed = runCatching { json.decodeFromString<AppData>(raw) }.getOrNull() ?: return false
        _data.value = parsed
        return true
    }

    companion object {
        private const val FILE_NAME = "simply_data.json"

        @Volatile
        private var instance: Repository? = null

        /** Один экземпляр на процесс: им пользуются и экран, и сервис таймера. */
        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context.applicationContext).also { instance = it }
            }
    }
}

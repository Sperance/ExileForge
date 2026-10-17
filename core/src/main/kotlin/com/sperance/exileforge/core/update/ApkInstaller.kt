package com.sperance.exileforge.core.update

import kotlinx.coroutines.flow.Flow
import java.io.File

/** Ответ системного установщика на сборку. */
sealed interface InstallResult {
    /** Установлена: процесс сейчас сменится новым. */
    data object Done : InstallResult

    /** Игрок закрыл подтверждение или система отказала; [message] - слова самой системы. */
    data class Failed(val message: String) : InstallResult
}

/** Сборка на устройстве глазами обновлений: её номер [code] и ищет ли она обновления сама ([selfUpdating], релизная). */
data class UpdateBuild(val code: Int, val selfUpdating: Boolean)

/** Установка скачанной сборки глазами ядра: системный установщик `:app`. */
interface ApkInstaller {
    /** Ответы установщика. */
    val results: Flow<InstallResult>

    /** Папка скачанных сборок. */
    val downloads: File

    /** Разрешено ли игре ставить сборки («Установка неизвестных приложений»). */
    fun allowed(): Boolean

    /** Отдаёт [apk] системному установщику; ответ придёт в [results]. */
    suspend fun install(apk: File)
}

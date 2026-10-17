package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.command.BugReportRequest
import java.io.File
import java.util.UUID

/**
 * Очередь отчётов на диске: отчёт, собранный к отправке (со сжатым снимком), сразу ложится файлом в [dir] и лежит
 * там, пока сервер его не примет или не откажет окончательно - краш и перезапуск приложения его не теряют. Ключ отчёта
 * ([BugReportRequest.key]) - имя файла: повтор уходит тем же ключом, и сервер второй раз отчёт не запишет. Не потокобезопасна:
 * зовёт один владелец по очереди.
 */
class BugOutbox(private val dir: File) {
    /** Отчёт [report] в очередь под новым ключом; отчёт с ключом уже записанный - тот же файл. Вернёт отчёт с ключом. */
    fun put(report: BugReportRequest): BugReportRequest {
        val keyed = if (report.key.isBlank()) report.copy(key = newKey()) else report
        dir.mkdirs()
        val target = file(keyed.key)
        val draft = File(dir, keyed.key + DRAFT)
        draft.writeText(WireJson.encodeToString(BugReportRequest.serializer(), keyed))
        if (!draft.renameTo(target)) {
            target.delete()
            draft.renameTo(target)
        }
        trim()
        return keyed
    }

    /** Отчёты в очереди, старые первыми; нечитаемый файл (оборванная запись) удаляется. */
    fun pending(): List<BugReportRequest> = files().mapNotNull { file ->
        runCatching { WireJson.decodeFromString(BugReportRequest.serializer(), file.readText()) }
            .getOrNull()?.takeIf { it.key.isNotBlank() }
            ?: run {
                file.delete()
                null
            }
    }

    /** Отчёт [key] принят сервером или отклонён им окончательно - из очереди вон. */
    fun remove(key: String) {
        file(key).delete()
    }

    private fun file(key: String) = File(dir, key + SUFFIX)

    private fun files(): List<File> = dir.listFiles { f -> f.isFile && f.name.endsWith(SUFFIX) }.orEmpty().sortedBy(File::lastModified)

    /** Очередь не растёт без конца, пока связи нет: сверх [LIMIT] уходят самые старые. */
    private fun trim() {
        files().dropLast(LIMIT).forEach(File::delete)
    }

    companion object {
        /** Сколько отчётов ждут в очереди самое большее. */
        const val LIMIT = 20

        private const val SUFFIX = ".json"
        private const val DRAFT = ".part"

        /** Ключ отчёта: 32 шестнадцатеричных знака - в формате ключей идемпотентности сервера. */
        fun newKey(): String = UUID.randomUUID().toString().replace("-", "")
    }
}

package com.sperance.exileforge.platform

import android.content.Context
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.app.LeftReport
import com.sperance.exileforge.core.app.LeftReports
import com.sperance.exileforge.core.feedback.CrashTraces
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.command.CrashTrace
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Время падения (мс) - приставка имени его файла. */
private val File.stamp: Long get() = name.substringBefore('-').toLongOrNull() ?: 0

/**
 * Падения клиента: необработанное исключение пишет стек на диск ([CRASH_DIR]) и отдаёт падение прежнему обработчику -
 * процесс умирает как обычно. Следующий запуск шлёт каждое неотправленное падение отчётом [FeedbackKind.CRASH] ([pending] /
 * [sent]) - сервер сам заведёт Issue или посчитает повтор. Отправленные стеки живут [FRESH_MS] и только в своей версии сборки
 * ([sweep]): их прикладывает отчёт об ошибке игрока ([recent]); неотправленные срок и смена версии не трогают.
 */
class CrashKeeper(context: Context) :
    LeftReports,
    CrashTraces {
    private val dir = File(context.filesDir, CRASH_DIR)

    /** Встаёт перед прежним обработчиком; повторный вызов ничего не меняет. */
    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is Keeper) return
        Thread.setDefaultUncaughtExceptionHandler(Keeper(previous))
    }

    private inner class Keeper(private val previous: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, error: Throwable) {
            runCatching { write(thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun write(thread: Thread, error: Throwable) {
        dir.mkdirs()
        val at = System.currentTimeMillis()
        File(dir, "$at-${UUID.randomUUID().toString().replace("-", "")}$PENDING").writeText("${BuildConfig.VERSION_NAME}\n${thread.name}\n---\n${error.stackTraceToString()}")
        trim()
    }

    /** На диске не больше [KEEP] стеков: старые уходят, даже неотправленные - падение в цикле не копит гору. */
    private fun trim() {
        files().drop(KEEP).forEach { it.delete() }
    }

    /** Стеки новыми первыми. */
    private fun files(): List<File> = dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.stamp }.orEmpty()

    /** Прочитанные записи падений, новые первыми; нечитаемая и без стека - не падение. */
    private fun kept(): List<KeptCrash> = files().mapNotNull { file -> runCatching { file.readText() }.getOrNull()?.let { KeptCrash.of(file, it) } }

    /**
     * Стирает отправленные стеки старше [FRESH_MS] от [now] и прежних версий сборки - к отчёту они больше не идут; неотправленные
     * ждут своей отправки.
     */
    private fun sweep(now: Long) {
        kept().filter { it.sent && (it.at < now - FRESH_MS || it.trace.buildVersion != BuildConfig.VERSION_NAME) }.forEach { it.file.delete() }
    }

    /** Неотправленные падения: готовый отчёт и стирание его записи, старые первыми. */
    override suspend fun pending(): List<LeftReport> = withContext(Dispatchers.IO) {
        sweep(System.currentTimeMillis())
        kept().filterNot { it.sent }.reversed().map { crash ->
            val report = BugReportRequest(
                text = crash.trace.stack.lineSequence().first().take(TITLE),
                screen = SCREEN,
                context = mapOf("crashThread" to crash.thread, "auto" to "true"),
                kind = FeedbackKind.CRASH,
                clientVersion = crash.trace.buildVersion,
                crashes = listOf(crash.trace),
                key = "crash-${crash.file.name.substringAfter('-').substringBefore('.')}",
            )
            LeftReport(report) { sent(crash.file) }
        }
    }

    /** Падение [file] ушло: стек остаётся среди последних, но больше не шлётся. */
    private suspend fun sent(file: File) = withContext(Dispatchers.IO) {
        file.renameTo(File(dir, file.name.removeSuffix(PENDING) + SENT))
    }

    /**
     * Падения к отчёту игрока об ошибке: только этой версии сборки и не старше [FRESH_MS], одинаковые стеки - одним с числом повторов
     * ([CrashTrace.count]), новые первыми; стек с версией своей сборки, без потока.
     */
    override suspend fun recent(): List<CrashTrace> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        sweep(now)
        kept().filter { it.at >= now - FRESH_MS && it.trace.buildVersion == BuildConfig.VERSION_NAME }
            .groupBy { it.trace.stack }.values.map { same -> same.first().trace.copy(count = same.size) }
    }

    /**
     * Запись падения на диске: [file], время [at] из имени, отправлено ли ([sent] - `.sent`), поток [thread] и стек с версией
     * сборки [trace].
     */
    private data class KeptCrash(val file: File, val at: Long, val sent: Boolean, val thread: String, val trace: CrashTrace) {
        companion object {
            /** Запись [text] файла [file]: первая строка шапки - версия сборки, вторая - поток, после [SPLIT] - стек; пустой стек - не падение. */
            fun of(file: File, text: String): KeptCrash? {
                val stack = text.substringAfter(SPLIT, "").takeIf { it.isNotBlank() } ?: return null
                val head = text.substringBefore(SPLIT).lines()
                return KeptCrash(file, file.stamp, file.name.endsWith(SENT), head.getOrNull(1).orEmpty(), CrashTrace(stack, head.first()))
            }
        }
    }

    private companion object {
        const val CRASH_DIR = "crashes"
        const val PENDING = ".pending"
        const val SENT = ".sent"
        const val SPLIT = "\n---\n"
        const val KEEP = 3
        const val TITLE = 200
        const val SCREEN = "CRASH"

        /** Сколько живёт отправленный стек и сколько падение идёт к отчёту об ошибке: сутки. */
        const val FRESH_MS = 24 * 60 * 60 * 1000L
    }
}

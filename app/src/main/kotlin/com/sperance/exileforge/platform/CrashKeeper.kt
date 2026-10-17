package com.sperance.exileforge.platform

import android.content.Context
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.app.LeftReport
import com.sperance.exileforge.core.app.LeftReports
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Падения клиента: необработанное исключение пишет стек на диск ([CRASH_DIR]) и отдаёт падение прежнему обработчику -
 * процесс умирает как обычно. Следующий запуск шлёт каждое неотправленное падение отчётом [FeedbackKind.CRASH] ([pending] /
 * [sent]) - сервер сам заведёт Issue или посчитает повтор. Последние [KEEP] стеков остаются и после отправки: их прикладывает
 * отчёт об ошибке игрока ([recent]).
 */
class CrashKeeper(context: Context) : LeftReports {
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
    private fun files(): List<File> = dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.name.substringBefore('-').toLongOrNull() ?: 0 }.orEmpty()

    /** Неотправленные падения: готовый отчёт и стирание его записи, старые первыми. */
    override suspend fun pending(): List<LeftReport> = withContext(Dispatchers.IO) {
        files().filter { it.name.endsWith(PENDING) }.reversed().mapNotNull { file ->
            val text = runCatching { file.readText() }.getOrNull() ?: return@mapNotNull null
            val head = text.substringBefore(SPLIT).lines()
            val stack = text.substringAfter(SPLIT, "")
            if (stack.isBlank()) return@mapNotNull null
            val report = BugReportRequest(
                text = stack.lineSequence().first().take(TITLE),
                screen = SCREEN,
                context = mapOf("crashThread" to head.getOrNull(1).orEmpty(), "auto" to "true"),
                kind = FeedbackKind.CRASH,
                clientVersion = head.getOrNull(0).orEmpty(),
                traces = listOf(stack),
                key = "crash-${file.name.substringAfter('-').substringBefore('.')}",
            )
            LeftReport(report) { sent(file) }
        }
    }

    /** Падение [file] ушло: стек остаётся среди последних, но больше не шлётся. */
    private suspend fun sent(file: File) = withContext(Dispatchers.IO) {
        file.renameTo(File(dir, file.name.removeSuffix(PENDING) + SENT))
    }

    /** Последние стеки (новые первыми) - к отчёту игрока об ошибке; без версии и потока, только стек. */
    suspend fun recent(): List<String> = withContext(Dispatchers.IO) {
        files().mapNotNull { runCatching { it.readText().substringAfter(SPLIT, "") }.getOrNull()?.takeIf { s -> s.isNotBlank() } }
    }

    private companion object {
        const val CRASH_DIR = "crashes"
        const val PENDING = ".pending"
        const val SENT = ".sent"
        const val SPLIT = "\n---\n"
        const val KEEP = 3
        const val TITLE = 200
        const val SCREEN = "CRASH"
    }
}

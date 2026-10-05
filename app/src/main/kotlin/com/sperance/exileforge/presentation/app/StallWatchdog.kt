package com.sperance.exileforge.presentation.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.session.CommandRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Сторож главного потока (3.82.0). Фоновый поток раз в секунду ставит в очередь главного потока отметку; не выполнилась
 * за [STALL_MS] - поток завис: его стек, шаги запуска ([StartupTrace]) и версия пишутся в файл. Завис намертво - отчёт
 * всё равно на диске, и следующий запуск сам отправляет его баг-репортом ([pending]/[sent]). Одно зависание - один отчёт.
 * Отправленный отчёт остаётся последним ([diagnostics]): игрок копирует его из окна запуска вместе со стеками всех потоков.
 */
class StallWatchdog(context: Context, private val trace: StartupTrace, private val commands: CommandRunner) {
    private val file = File(context.filesDir, FILE)
    private val last = File(context.filesDir, LAST_FILE)
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var started = false

    fun start() {
        if (started) return
        started = true
        Thread(::watch, "stall-watchdog").apply { isDaemon = true }.start()
    }

    private fun watch() {
        while (true) {
            val posted = SystemClock.uptimeMillis()
            val answered = AtomicBoolean(false)
            main.post { answered.set(true) }
            var reported = false
            var since = posted
            var tick = posted
            while (!answered.get()) {
                Thread.sleep(TICK_MS)
                val now = SystemClock.uptimeMillis()
                // Сторож проспал больше пары тиков - процесс стоял целиком (заморозка свёрнутого приложения, 3.84.0):
                // главный поток не виноват, отсчёт идёт заново, иначе разморозка пишет «зависание» на всю паузу.
                if (now - tick > PAUSE_MS) since = now
                tick = now
                val stalled = now - since
                if (!reported && stalled >= STALL_MS) {
                    reported = true
                    write(stalled)
                }
            }
            Thread.sleep(TICK_MS)
        }
    }

    private fun write(stalled: Long) {
        runCatching {
            val stack = threads()
            file.writeText("${BuildConfig.VERSION_NAME}\n$stalled\n${System.currentTimeMillis()}\n---\n$stack\n---\n${trace.journal()}")
        }
    }

    /** Отчёт о прошлом зависании, ещё не отправленный; null - его нет. */
    suspend fun pending(): BugReportRequest? = withContext(Dispatchers.IO) {
        val text = file.takeIf { it.isFile }?.readText() ?: return@withContext null
        val parts = text.split("\n---\n")
        val head = parts.getOrNull(0).orEmpty().lines()
        val stack = parts.getOrNull(1).orEmpty().lines()
        val journal = parts.getOrNull(2).orEmpty().lines()
        BugReportRequest(
            text = ui("stall.title") + " ${head.getOrNull(1).orEmpty()} ms",
            screen = SCREEN,
            context = mapOf("version" to head.getOrNull(0).orEmpty(), "stalledMs" to head.getOrNull(1).orEmpty(), "at" to head.getOrNull(2).orEmpty()),
            // Сервер берёт до 20 строк по 400 знаков: стек - сверху, журнал запуска - хвостом.
            requests = pack(stack, STACK_ENTRIES) + pack(journal.takeLast(JOURNAL_LINES), JOURNAL_ENTRIES),
        )
    }

    suspend fun sent() = withContext(Dispatchers.IO) {
        last.delete()
        if (!file.renameTo(last)) file.delete()
    }

    /**
     * Журнал для разработчика: версия, шаги запуска, стеки всех потоков сейчас и отчёт о прошлом зависании, если он был.
     * Стеки снимаются в момент вызова - зависший фон виден, пока окно запуска ещё отвечает.
     */
    fun diagnostics(): String = buildString {
        appendLine("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) uptime=${SystemClock.uptimeMillis()}ms at=${System.currentTimeMillis()}")
        appendLine("--- start")
        appendLine(trace.journal())
        appendLine("--- commands")
        appendLine(runCatching { commands.describe() }.getOrElse { it.toString() })
        appendLine("--- threads")
        appendLine(threads())
        (file.takeIf { it.isFile } ?: last.takeIf { it.isFile })?.let { stall ->
            appendLine("--- previous stall")
            append(runCatching { stall.readText() }.getOrDefault(""))
        }
    }

    /**
     * Стеки потоков: главный первым, затем занятые. Простаивающие (ждут очередь, сообщений или сокета пула) сведены в одну
     * строку именами (3.87.0): журнал, вставленный в чат, не обрезается на них.
     */
    private fun threads(): String {
        val main = Looper.getMainLooper().thread
        val (busy, idle) = Thread.getAllStackTraces().entries.partition { (thread, frames) -> thread === main || !resting(thread, frames) }
        return busy.sortedWith(compareBy({ it.key !== main }, { it.key.name }))
            .joinToString("\n") { (thread, frames) ->
                "\"${thread.name}\" ${thread.state}" + frames.take(THREAD_FRAMES).joinToString("") { "\n  at $it" }
            } + "\nidle: " + idle.joinToString(", ") { it.key.name }
    }

    private fun resting(thread: Thread, frames: Array<StackTraceElement>): Boolean = frames.isEmpty() ||
        thread.state == Thread.State.WAITING || thread.state == Thread.State.TIMED_WAITING ||
        frames.first().methodName == "nativePollOnce"

    private fun pack(lines: List<String>, entries: Int): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        for (line in lines) {
            if (current.isNotEmpty() && current.length + line.length + 1 > ENTRY) {
                out += current.toString()
                current.clear()
                if (out.size == entries) return out
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(line.take(ENTRY))
        }
        if (current.isNotEmpty() && out.size < entries) out += current.toString()
        return out
    }

    private companion object {
        const val FILE = "stall.txt"
        const val LAST_FILE = "stall-last.txt"
        const val THREAD_FRAMES = 25
        const val SCREEN = "stall"
        const val STALL_MS = 5_000L
        const val TICK_MS = 1_000L
        const val PAUSE_MS = 3 * TICK_MS
        const val ENTRY = 400
        const val STACK_ENTRIES = 12
        const val JOURNAL_ENTRIES = 8
        const val JOURNAL_LINES = 60
    }
}

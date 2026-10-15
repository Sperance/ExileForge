package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.network.RequestLog
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Последние действия игрока для отчёта (0.14.2): переходы экранов и дыры словаря (0.14.5, #48) копятся здесь, запросы - в журнале
 * запросов; [lines] сводит их по времени. Опрос сервера (`/system/health`, `/static/`) - шум, его нет. Только вид действия: без тел
 * запросов и данных игрока.
 */
class ReportTrail(private val clock: () -> Long = System::currentTimeMillis) {
    private val steps = ArrayDeque<Step>()

    private data class Step(val at: Long, val line: String)

    /** Игрок открыл экран [place] (его имя для человека). */
    @Synchronized fun screen(place: String) {
        if (steps.lastOrNull()?.line == "▸ $place") return
        add("▸ $place")
    }

    /** На экране не нашлось строки словаря сервера [key] (0.14.5, #48): игрок видел заглушку. */
    @Synchronized fun hole(key: String) = add("⚠ locale $key")

    private fun add(line: String) {
        steps.addLast(Step(clock(), line))
        while (steps.size > LIMIT) steps.removeFirst()
    }

    /** Последние [LIMIT] действий старыми первыми: экраны «▸ место», дыры словаря «⚠ locale ключ», запросы «метод путь → статус». */
    @Synchronized fun lines(requests: List<RequestLog>): List<String> {
        val calls = requests.filterNot { log -> NOISE.any { log.path.startsWith(it) } }
            .map { Step(it.at, "${it.method} ${it.path.substringBefore('?')} → ${it.status ?: 0}${it.code?.let { code -> " $code" }.orEmpty()}") }
        return (steps + calls).sortedBy { it.at }.takeLast(LIMIT).map { "${TIME.format(Instant.ofEpochMilli(it.at))} ${it.line}" }
    }

    private companion object {
        const val LIMIT = 15

        /** Запросы, что идут сами по таймеру: в действиях игрока их нет. */
        val NOISE = listOf("/system/health", "system/health", "/static/", "static/")
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC)
    }
}

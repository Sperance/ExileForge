package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.stampText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.core.model.command.RequestFailure
import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.FullReport
import com.sperance.exileforge.core.model.feedback.ReportDetail

/** Чтение технического раскрытого отчёта модерации: идёт, не удалось (экран предлагает «Повторить») или готово. */
sealed interface DetailLoad {
    data object Loading : DetailLoad

    data object Failed : DetailLoad

    data class Ready(val detail: ReportDetail) : DetailLoad
}

/**
 * Свёрнутая секция технического отчёта модерации - по важности для решения: падения, ошибки запросов, последние действия, устройство
 * и среда, снимок героя. [title] - ключ подписи; [alarming] - итог красный, когда данные есть.
 */
enum class ReportSection(val title: String, val alarming: Boolean = false) {
    CRASHES("feedback.section.crashes", alarming = true),
    FAILURES("feedback.section.failures", alarming = true),
    TRAIL("feedback.section.trail"),
    ENVIRONMENT("feedback.section.environment"),
    HERO("feedback.section.hero"),
    ;

    /**
     * Итог секции справа: падения - «число · версии» (у падения - его повторы, у ошибки - падения перед ней), ошибки - «число · последняя
     * статус», действия - их число, среда - «устройство · Android», герой - класс и уровень; null - данных нет, секция не раскрывается.
     */
    fun summary(report: FullReport, detail: ReportDetail): String? = when (this) {
        CRASHES -> detail.crashes.takeIf { it.isNotEmpty() }?.let { crashes ->
            val repeats = report.kind == FeedbackKind.CRASH
            val count = if (repeats) report.occurrences else crashes.size
            val versions = (if (repeats) report.versions else crashes.map { it.buildVersion }).filter { it.isNotBlank() }.distinct()
            (listOf(count.toString()) + listOfNotNull(versions.joinToString(", ").takeIf { it.isNotEmpty() })).joinToString(" · ")
        }

        FAILURES -> detail.failures.firstOrNull()?.let { last -> ui("feedback.section.failures_sum", detail.failures.size, last.answer) }

        TRAIL -> detail.trail.size.takeIf { it > 0 }?.toString()

        ENVIRONMENT -> listOfNotNull(detail.context[MODEL], detail.context[ANDROID]?.let { "Android " + it.substringBefore(' ') }).joinToString(" · ").ifEmpty { report.clientVersion.ifEmpty { DASH } }

        HERO -> detail.heroSheet.takeIf { it.isNotEmpty() }?.let { sheet ->
            if (report.heroClass.isBlank()) sheet.size.toString() else ui("feedback.admin.hero_line", classTitle(report.heroClass), report.heroLevel)
        }
    }

    private companion object {
        const val MODEL = "model"
        const val ANDROID = "android"
        const val DASH = "—"
    }
}

/** Ответ сервера на неудачный запрос: статус с кодом отказа или «нет ответа». */
val RequestFailure.answer: String get() = if (status == 0) ui("feedback.no_answer") else listOf(status.toString(), code).filter { it.isNotBlank() }.joinToString(" ")

/** Неудачный запрос строкой журнала: «время метод путь → ответ». */
val RequestFailure.line: String get() = listOf(at.substringAfter('T').take(TIME), method, path, "→", answer).filter { it.isNotBlank() }.joinToString(" ")

/** Строка последних действий - запрос без ответа или с отказом (`→ 0`, `→ 4xx`, `→ 5xx`): журнал красит её красным. */
fun trailFault(line: String): Boolean = FAULT.find(line)?.groupValues?.get(1)?.toIntOrNull()?.let { it == 0 || it >= FIRST_FAULT } == true

/** Первая строка стека - класс и текст исключения - без пакета: заголовок падения в секции. */
fun crashHead(stack: String): String = stack.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().trim().replace(PACKAGE, "")

/**
 * Секция «Устройство и среда» строками «подпись - значение»: версия клиента, экран, раздел, открытый контекст по порядку сервера
 * (подпись ключа - словарь, неизвестный ключ - как есть), время отправки и номер отчёта; пустое не пишется.
 */
fun ReportDetail.environment(report: FullReport): List<Pair<String, String>> = buildList {
    add(ui("feedback.env.version") to report.clientVersion.ifBlank { "—" })
    if (report.screen.isNotBlank()) add(ui("feedback.env.screen") to report.screen)
    if (area.isNotBlank()) add(ui("feedback.env.area") to area)
    context.forEach { (key, value) -> add(uiOr(uiLanguage, "feedback.env.$key", key) to value) }
    if (sentAt > 0) add(ui("feedback.env.sent") to stampText(sentAt))
    add(ui("feedback.env.id") to id)
}

/** Версии клиента отчётов [reports] для выбора версии в списке модерации: без пустых и повторов, новые первыми (по числам семвера). */
fun clientVersions(reports: List<AdminReport>): List<String> = reports.map { it.report.clientVersion }.filter { it.isNotBlank() }.distinct()
    .sortedWith(compareByDescending<String> { it.semver() }.thenByDescending { it })

/** Три числа семвера одним ключом сравнения: нечисловая и недостающая часть - 0. */
private fun String.semver(): Long = split('.').take(SEMVER_PARTS).fold(0L) { acc, part -> acc * SEMVER_BASE + (part.takeWhile(Char::isDigit).toLongOrNull() ?: 0L) }.let { key ->
    (split('.').size until SEMVER_PARTS).fold(key) { acc, _ -> acc * SEMVER_BASE }
}

private const val SEMVER_PARTS = 3
private const val SEMVER_BASE = 10_000L
private val FAULT = Regex("→ (\\d+)")
private val PACKAGE = Regex("^(?:[a-z_][\\w$]*\\.)+(?=[A-Z])")
private const val FIRST_FAULT = 400
private const val TIME = 8

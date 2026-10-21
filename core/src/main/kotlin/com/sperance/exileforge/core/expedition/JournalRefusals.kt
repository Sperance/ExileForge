package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.rules.content.TrialEventKind
import com.sperance.exileforge.rules.run.Refusal
import com.sperance.exileforge.rules.run.RunEventKind

/**
 * Отказ сервера по журналу захода или испытания: отклонённые события [rejected] - строкой на каждую причину из [refusals] (без
 * причины - отказ правила), в строке - виды отклонённых событий со счётом повторов; событие, чей вид неизвестен, - числом.
 * [kindKeys] - по номеру события ключ локали его вида из [refusedKey]. Красным «!» ([NoticeKind.FAILED]); ничего не отклонено - тоста нет.
 */
internal fun Notices.refused(rejected: Collection<Int>, refusals: Map<Int, Refusal>, kindKeys: Map<Int, String>) {
    if (rejected.isEmpty()) return
    val byCause = rejected.groupBy { refusals[it] ?: Refusal.RULE }
    toast(Refusal.entries.mapNotNull { cause -> byCause[cause]?.let { ui("expedition.refused.${cause.name}", refusedKinds(it, kindKeys)) } }.joinToString("\n"), NoticeKind.FAILED)
}

/** Виды отклонённых событий [numbers] через запятую по ключам [kindKeys]: «бой со стаей ×2, сундук»; без вида - «событий: N». */
private fun refusedKinds(numbers: List<Int>, kindKeys: Map<Int, String>): String {
    val named = numbers.mapNotNull { kindKeys[it] }.groupingBy { it }.eachCount().map { (key, count) -> if (count > 1) ui("expedition.refused.times", ui(key), count) else ui(key) }
    val nameless = numbers.count { it !in kindKeys }
    return (named + listOfNotNull(ui("expedition.refused.events", nameless).takeIf { nameless > 0 })).joinToString(", ")
}

/** Ключ локали имени вида события захода в тосте отказа. */
internal fun RunEventKind.refusedKey(): String = "expedition.refused.kind.$name"

/** Ключ локали имени вида события испытания в тосте отказа. */
internal fun TrialEventKind.refusedKey(): String = "trial.refused.kind.$name"

package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.rules.content.TrialEventKind

/**
 * Отказ правила сервера по журналу испытания: отклонённые события [rejected] одной строкой - вид события со счётом повторов; событие,
 * чей вид неизвестен, - числом. [kindKeys] - по номеру события ключ локали его вида из [refusedKey]. Красным «!» ([NoticeKind.FAILED]);
 * ничего не отклонено - тоста нет.
 */
internal fun Notices.refused(rejected: Collection<Int>, kindKeys: Map<Int, String>) {
    if (rejected.isEmpty()) return
    val parts = rejected.groupingBy { kindKeys[it] }.eachCount().map { (kindKey, count) -> refusalText(kindKey, count) }
    toast(ui("expedition.refused.rule", parts.joinToString(", ")), NoticeKind.FAILED)
}

/** Один вид отказа в тосте: «босс испытания ×2»; без вида ([kindKey] null) - «событий: N». */
private fun refusalText(kindKey: String?, count: Int): String = when {
    kindKey == null -> ui("expedition.refused.events", count)
    count > 1 -> ui("expedition.refused.times", ui(kindKey), count)
    else -> ui(kindKey)
}

/** Ключ локали имени вида события испытания в тосте отказа. */
internal fun TrialEventKind.refusedKey(): String = "trial.refused.kind.$name"

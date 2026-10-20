package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.rules.run.Refusal

/**
 * Отказ сервера по журналу захода или испытания: отклонённые события [rejected] - строкой на каждую причину из [refusals] (без причины -
 * отказ правила), красным «!» ([NoticeKind.FAILED]); ничего не отклонено - тоста нет.
 */
internal fun Notices.refused(rejected: Collection<Int>, refusals: Map<Int, Refusal>) {
    if (rejected.isEmpty()) return
    val causes = rejected.groupingBy { refusals[it] ?: Refusal.RULE }.eachCount()
    toast(Refusal.entries.mapNotNull { cause -> causes[cause]?.let { ui("expedition.refused.${cause.name}", it) } }.joinToString("\n"), NoticeKind.FAILED)
}

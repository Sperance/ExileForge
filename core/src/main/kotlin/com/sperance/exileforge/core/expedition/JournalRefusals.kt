package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.rules.content.TrialEventKind
import com.sperance.exileforge.rules.run.RefusalReason
import com.sperance.exileforge.rules.run.RunEventKind

/**
 * Отказ правила сервера по журналу захода или испытания: отклонённые события [rejected] одной строкой - вид события с причиной по коду
 * правила [reasons] и счётом повторов; событие, чей вид неизвестен, - числом, без кода - «не по правилам» ([RefusalReason.RULE]).
 * [kindKeys] - по номеру события ключ локали его вида из [refusedKey]. Красным «!» ([NoticeKind.FAILED]); ничего не отклонено - тоста нет.
 */
internal fun Notices.refused(rejected: Collection<Int>, kindKeys: Map<Int, String>, reasons: Map<Int, RefusalReason>) {
    if (rejected.isEmpty()) return
    val parts = rejected.groupingBy { JournalRefusal(kindKeys[it], reasons[it] ?: RefusalReason.RULE) }.eachCount().map { (refusal, count) -> refusal.text(count) }
    toast(ui("expedition.refused.rule", parts.joinToString(", ")), NoticeKind.FAILED)
}

/** Один вид отказа в тосте: вид события по ключу [kindKey] (null - вид неизвестен) и причина [reason]. */
private data class JournalRefusal(val kindKey: String?, val reason: RefusalReason) {
    /** «бой со стаей ×2 - этого уже нет»; без вида - «событий: N - …». */
    fun text(count: Int): String {
        val what = when {
            kindKey == null -> ui("expedition.refused.events", count)
            count > 1 -> ui("expedition.refused.times", ui(kindKey), count)
            else -> ui(kindKey)
        }
        return ui("expedition.refused.because", what, ui(reason.refusedKey()))
    }
}

/** Ключ локали имени вида события захода в тосте отказа. */
internal fun RunEventKind.refusedKey(): String = "expedition.refused.kind.$name"

/** Ключ локали имени вида события испытания в тосте отказа. */
internal fun TrialEventKind.refusedKey(): String = "trial.refused.kind.$name"

/** Ключ локали причины отказа правила сервера. */
internal fun RefusalReason.refusedKey(): String = "expedition.refused.reason.$name"

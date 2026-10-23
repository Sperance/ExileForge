package com.sperance.exileforge.core.admin

import com.sperance.exileforge.core.display.dayText
import com.sperance.exileforge.core.display.dayTimeText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.epochOfStamp
import java.time.ZoneId

/**
 * Срок корзины удаления на миг «сейчас» по часам сервера: [until] - когда очистка (мс эпохи), [left] - сколько до неё осталось, [share] -
 * доля срока, что ещё впереди (полоса «осталось»: полная у свежего удаления, пустая к очистке), [urgent] - последние [URGENT_DAYS] дня.
 */
data class TrashTerm(val until: Long, val left: Long, val share: Float) {
    val urgent: Boolean get() = left < URGENT_DAYS * DAY_MS

    /** Сколько осталось: «через 29 дн. 22 ч», в последние сутки - «через 4 ч 5 мин», в последний час - «через 5 мин»; срок вышел - «вот-вот». */
    fun leftText(): String = when {
        left >= DAY_MS -> ui("moderation.purge.left_days", left / DAY_MS, left % DAY_MS / HOUR_MS)
        left >= HOUR_MS -> ui("moderation.purge.left_hours", left / HOUR_MS, left % HOUR_MS / MINUTE_MS)
        left >= MINUTE_MS -> ui("moderation.purge.left_minutes", left / MINUTE_MS)
        else -> ui("moderation.purge.due")
    }

    /** Когда очистка в поясе [zone]: «24 нояб.», в последние сутки - со временем «25 окт. в 20:05». */
    fun dateText(zone: ZoneId = ZoneId.systemDefault()): String = if (left < DAY_MS) dayTimeText(until, zone) else dayText(until, zone)

    companion object {
        /** Сколько последних дней срока полоса - красная. */
        const val URGENT_DAYS = 3

        private const val MINUTE_MS = 60_000L
        private const val HOUR_MS = 60 * MINUTE_MS
        private const val DAY_MS = 24 * HOUR_MS

        /** Срок удаления, наложенного в [at], с очисткой в [until] (метки сервера) на миг [now]; срока нет или не прочитать - null. */
        fun of(at: String, until: String?, now: Long): TrashTerm? {
            val end = epochOfStamp(until) ?: return null
            val start = epochOfStamp(at) ?: end
            val left = (end - now).coerceAtLeast(0)
            val share = if (end <= start) 0f else (left.toFloat() / (end - start)).coerceIn(0f, 1f)
            return TrashTerm(end, left, share)
        }
    }
}

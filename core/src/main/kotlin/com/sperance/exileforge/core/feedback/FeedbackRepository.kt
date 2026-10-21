package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.FeedbackQueue
import com.sperance.exileforge.core.model.feedback.FeedbackQueuePage
import com.sperance.exileforge.core.model.feedback.FeedbackSort
import com.sperance.exileforge.core.model.feedback.IgnoredHero
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailAddressee
import com.sperance.exileforge.core.model.feedback.MailQuota
import com.sperance.exileforge.core.model.feedback.OwnReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Отчёты и почта аккаунта: свои отчёты, очередь модерации, ящик. [unread] - непрочитанные письма: приходит со снимком героя, а
 * прочитанный ящик пересчитывает его сам ([withMail]).
 */
data class Feedback(
    val mine: List<OwnReport> = emptyList(),
    /** Очередь модерации: отбор и прочитанные страницы. */
    val queue: ReportQueue = ReportQueue(),
    val mail: List<Mail> = emptyList(),
    val unread: Int = 0,
    /** Кому администратор пишет письмо: аккаунт из карточки игрока, его берёт форма письма; null - не задан. */
    val mailTo: MailAddressee? = null,
    /** Письмо, что пишет герой: открыто окно письма; null - закрыто. */
    val letter: LetterDraft? = null,
    /** Почта героя сейчас: квота окна письма; null - ещё не прочитана. */
    val quota: MailQuota? = null,
    /** Список игнора героя в игре. */
    val ignored: List<IgnoredHero> = emptyList(),
) {
    /** Ящик, каким его теперь знает клиент, и счёт непрочитанного по нему. */
    fun withMail(mail: List<Mail>): Feedback = copy(mail = mail, unread = mail.count { !it.read })
}

/**
 * Очередь модерации, как её показывает окно: отбор сервера - вид [kind], очередь [queue], поиск [query], порядок [sort] - и прочитанные
 * страницы [page] (строки всех страниц подряд, счёт очередей - последней прочитанной). Смена отбора читает её с первой страницы.
 */
data class ReportQueue(
    val kind: FeedbackKind = FeedbackKind.BUG,
    val queue: FeedbackQueue = FeedbackQueue.NEW,
    val query: String = "",
    val sort: FeedbackSort = FeedbackSort.NEWEST,
    val page: FeedbackQueuePage = FeedbackQueuePage(),
) {
    /** Тот же ли отбор у очереди [other]: ответ на прежний отбор в новую очередь не ложится. */
    fun sameFilter(other: ReportQueue): Boolean = kind == other.kind && queue == other.queue && query == other.query && sort == other.sort

    /** Отчёт [report] встал на место своей строки: ответ на действие модерации. */
    fun with(report: AdminReport): ReportQueue = copy(page = page.copy(items = page.items.map { if (it.report.id == report.report.id) report else it }))
}

/** Единственный источник правды об отзывах и почте. */
class FeedbackRepository {
    private val mutable = MutableStateFlow(Feedback())
    val state: StateFlow<Feedback> = mutable

    fun update(transform: (Feedback) -> Feedback) = mutable.update(transform)

    fun clear() = update { Feedback() }
}

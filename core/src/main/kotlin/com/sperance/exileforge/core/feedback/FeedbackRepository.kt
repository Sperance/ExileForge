package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackMove
import com.sperance.exileforge.core.model.feedback.FeedbackQueue
import com.sperance.exileforge.core.model.feedback.FeedbackSort
import com.sperance.exileforge.core.model.feedback.IgnoredHero
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailAddressee
import com.sperance.exileforge.core.model.feedback.MailQuota
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.model.feedback.QueueCounts
import com.sperance.exileforge.core.model.feedback.QueueSight
import com.sperance.exileforge.core.model.paging.Page
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
 * Очередь модерации, как её показывает окно: отбор сервера - очередь [queue], поиск [query], порядок [sort] - прочитанные страницы [page]
 * (строки всех страниц подряд, все виды отчётов вместе) и счёт очередей [counts] - первой страницы или последнего действия модерации.
 * Смена отбора читает её с первой страницы.
 */
data class ReportQueue(
    val queue: FeedbackQueue = FeedbackQueue.NEW,
    val query: String = "",
    val sort: FeedbackSort = FeedbackSort.NEWEST,
    val page: Page<AdminReport> = Page(),
    val counts: QueueCounts = QueueCounts(),
) {
    /** Поиск очереди для сервера: под ним он считает счёт очередей. */
    val sight: QueueSight get() = QueueSight(query)

    /** Тот же ли отбор у очереди [other]: ответ на прежний отбор в новую очередь не ложится. */
    fun sameFilter(other: ReportQueue): Boolean = queue == other.queue && query == other.query && sort == other.sort

    /**
     * Ответ [move] на действие модерации с отчётом [id]: свежий счёт очередей; отчёт, что остался в выбранной очереди, встаёт на место
     * своей строки, удалённый или ушедший в другую очередь - уходит из списка. Страница не перечитывается.
     */
    fun after(id: String, move: FeedbackMove): ReportQueue {
        val report = move.report?.takeIf { it.queue == queue }
        val rows = if (report == null) page.without { it.report.id != id } else page.copy(items = page.items.map { if (it.report.id == id) report else it })
        return copy(page = rows, counts = move.counts)
    }
}

/** Единственный источник правды об отзывах и почте. */
class FeedbackRepository {
    private val mutable = MutableStateFlow(Feedback())
    val state: StateFlow<Feedback> = mutable

    fun update(transform: (Feedback) -> Feedback) = mutable.update(transform)

    fun clear() = update { Feedback() }
}

package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackMove
import com.sperance.exileforge.core.model.feedback.IssueMove
import com.sperance.exileforge.core.model.feedback.MailAddressee
import com.sperance.exileforge.core.model.feedback.MailRequest
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.model.feedback.QueueSight
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.rules.feedback.BugStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Виденные ответы на свои отчёты на устройстве: по id отчёта - отпечаток последнего виденного ответа. */
interface SeenReplies {
    /** Отпечатки виденных ответов по id отчёта. */
    val seen: Flow<Map<String, String>>

    /** Ответы [replies] (id отчёта → отпечаток) видены. */
    fun mark(replies: Map<String, String>)
}

/**
 * Отчёты и почта аккаунта: свои отчёты, очередь модерации страницами, ящик с вложениями, письма администратора. Первая модель экрана на
 * репозиториях :core. Ящик - глазами героя в игре, и почта между героями ([LetterActions]).
 */
class FeedbackModel(
    private val repository: FeedbackRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val sessions: SessionRepository,
    private val notices: Notices,
    private val heroes: HeroRepository,
    /** Почта между героями: окно письма, ответ, игнор, жалоба - общие с карточкой игрока и гильдией. */
    val letters: LetterActions,
    private val replies: SeenReplies,
    /** Область экрана: виденные ответы читаются, пока он жив. */
    scope: CoroutineScope,
) {
    val feedback: StateFlow<Feedback> = repository.state
    val activity = commands.state

    /** Виденные ответы на свои отчёты: отпечаток по id отчёта. */
    val seen: StateFlow<Map<String, String>> = replies.seen.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    private val api: GameApi get() = connection.api

    private fun feedback(transform: (Feedback) -> Feedback) = repository.update(transform)

    /** Адресат из карточки игрока для формы письма: отдаётся один раз и забывается. */
    fun takeMailTo(): MailAddressee? = feedback.value.mailTo.also { if (it != null) feedback { f -> f.copy(mailTo = null) } }

    /** Свои отчёты - окно своих отчётов; новый вызов сменяет идущий. */
    fun loadReports() = commands.read(Reads.FEEDBACK, restart = true) {
        val mine = api.feedback.mine()
        feedback { it.copy(mine = mine, mineRead = true) }
    }

    /** Ответы на свои отчёты [reports] видены (вкладка «Мои» открыта). */
    fun markSeen(reports: List<OwnReport>) = replies.mark(reports.mapNotNull { own -> own.replyPrint?.let { own.id to it } }.filter { (id, print) -> seen.value[id] != print }.toMap())

    /** Новый отбор очереди модерации [change] - вид, очередь, поиск или порядок: очередь читается с первой страницы. */
    fun filterQueue(change: (ReportQueue) -> ReportQueue) {
        feedback { it.copy(queue = change(it.queue)) }
        loadQueue()
    }

    /**
     * Первая страница очереди модерации под её отбором: своим ключом чтения, новый вызов сменяет идущий - последний выбранный отбор не
     * теряется за прежним чтением, ответ на прежний отбор в очередь не ложится.
     */
    fun loadQueue() = commands.read(Reads.FEEDBACK_ADMIN, restart = true) {
        check(sessions.state.value.isModerator) { ui("moderation.staff_only") }
        val asked = feedback.value.queue
        val read = api.feedback.queue(asked.sight, asked.queue, asked.sort, 0)
        feedback { f -> if (f.queue.sameFilter(asked)) f.copy(queue = f.queue.copy(page = read.page, counts = read.counts ?: f.queue.counts)) else f }
    }

    /**
     * Следующая страница очереди модерации под показанными (листание до края списка): чтение не перезапускается - это уронило бы свежую
     * первую страницу, - а страница ложится, только пока отбор и прочитанные страницы те же.
     */
    fun moreQueue() = commands.read(Reads.FEEDBACK_ADMIN) {
        val asked = feedback.value.queue
        if (!asked.page.more) return@read
        val next = api.feedback.queue(asked.sight, asked.queue, asked.sort, asked.page.page + 1).page
        feedback { f ->
            val shown = f.queue
            if (!shown.sameFilter(asked) || shown.page.page != asked.page.page) f else f.copy(queue = shown.copy(page = shown.page.followedBy(next) { it.report.id }))
        }
    }

    fun setReportStatus(id: String, status: BugStatus, reason: String) = moderate(id, ui("feedback.status_saved")) { api.feedback.setStatus(id, status, reason, it) }

    /** Ответ автора к своему отчёту: с Issue уходит комментарием, до Issue копится в отчёте; отчёт возвращается с перепиской. */
    fun answerReport(id: String, text: String) = commands.task(writing = true) {
        val answered = api.feedback.answer(id, text)
        feedback { f -> f.copy(mine = f.mine.map { if (it.id == id) answered else it }) }
        notices.toast(ui("feedback.answer_sent"))
    }

    /** Модерация ведёт Issue отчёта из игры: ответ или закрытие с причиной; судьба приходит из GitHub сразу. */
    fun steerIssue(id: String, move: IssueMove, text: String, duplicate: Int = 0) = moderate(id, ui("feedback.issue_moved")) { api.feedback.steer(id, move, text, duplicate, it) }

    /** Дополнение модерации к отчёту: сервер кладёт его разделом в тело Issue и отвечает отчётом как он теперь стоит. */
    fun noteReport(id: String, text: String) = moderate(id, ui("feedback.note.saved")) { api.feedback.note(id, text, it) }

    /** Отчёт в GitHub Issues: сервер заводит Issue (или повторяет неудачную выгрузку) и отвечает отчётом как он теперь стоит. */
    fun reportToIssue(id: String) = moderate(id, ui("feedback.issue_done")) { api.feedback.toIssue(id, it) }

    /** Автор закрывает свой отчёт («Закрыт автором»): без возврата, писать в него больше нельзя; отчёт встаёт на место закрытым. */
    fun closeReport(id: String) = commands.task(writing = true) {
        val closed = api.feedback.close(id)
        feedback { f -> f.copy(mine = f.mine.map { if (it.id == id) closed else it }) }
        notices.toast(ui("feedback.closed"))
    }

    /** Комментарий модерации автору: отчёт - в «Обсуждении», автору уходит письмо. */
    fun commentReport(id: String, text: String) = moderate(id, ui("feedback.comment_sent")) { api.feedback.comment(id, text, it) }

    /** Удаление отчёта модерацией: уходит из очереди, счёт очередей - свежий. */
    fun deleteReport(id: String) = moderate(id, ui("feedback.deleted")) { api.feedback.delete(id, it) }

    /**
     * Действие модерации [call] с отчётом [id] под видом и поиском очереди: ответ ложится в очередь ([ReportQueue.after]) - строка на месте
     * или уходит, чипы - со свежим счётом; страница не перечитывается.
     */
    private fun moderate(id: String, done: String, call: suspend (QueueSight) -> FeedbackMove) = commands.task(writing = true) {
        val move = call(feedback.value.queue.sight)
        feedback { f -> f.copy(queue = f.queue.after(id, move)) }
        notices.toast(done)
    }

    /** Ящик глазами героя в игре, тихо - при открытии почты; счёт конверта в шапке едет в снимке героя. */
    fun loadMail() {
        if (!sessions.state.value.signedIn) return
        commands.read(Reads.MAIL, silent = true) {
            val mail = api.mail.inbox(heroes.heroId)
            feedback { it.withMail(mail) }
        }
    }

    fun readMail(id: String) = commands.read(Reads.MAIL) {
        val letter = api.mail.read(id)
        feedback { f -> f.withMail(f.mail.map { if (it.id == id) letter else it }) }
    }

    /** Вложение уходит герою в игре [heroId]; герой приходит снимком в том же ответе. */
    fun claimMail(id: String, heroId: String) = commands.task(writing = true, touches = setOf(Reads.HERO)) {
        check(heroId.isNotBlank()) { ui("auction.choose_character") }
        val letter = api.mail.claim(id, heroId)
        feedback { f -> f.withMail(f.mail.map { if (it.id == id) letter else it }) }
        notices.toast(ui("mail.claimed"))
    }

    /** «Прочитать все». */
    fun readAllMail() = commands.task(writing = true) {
        api.mail.readAll(heroes.heroId)
        feedback { f -> f.withMail(f.mail.map { it.copy(read = true) }) }
    }

    /** «Удалить прочитанные»: письма с незабранным вложением остаются. */
    fun deleteReadMail() = commands.task(writing = true) {
        val gone = api.mail.deleteRead(heroes.heroId)
        feedback { f -> f.withMail(f.mail.filterNot { it.read && !it.claimable }) }
        notices.toast(ui("mail.deleted_n", gone))
    }

    fun deleteMail(id: String) = commands.task(writing = true) {
        api.mail.delete(id)
        feedback { f -> f.withMail(f.mail.filterNot { it.id == id }) }
    }

    /**
     * Письмо от Администрации: одному аккаунту по id (адресат из карточки или досье) - `admin/moderation/mail` модератору и администратору
     * (права и вложение решает сервер); по логину или всем - `admin/mail`, только администратору.
     */
    fun sendMail(request: MailRequest) = commands.task(writing = true) {
        val session = sessions.state.value
        check(session.isModerator && (session.isAdmin || request.userId.isNotBlank())) { ui("moderation.staff_only") }
        val sent = if (request.userId.isNotBlank()) api.moderation.mail(request) else api.mail.send(request)
        notices.toast(ui("mail.sent", sent))
    }
}

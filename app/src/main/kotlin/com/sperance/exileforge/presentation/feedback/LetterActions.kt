package com.sperance.exileforge.presentation.feedback

import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.feedback.LetterDraft
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailKind
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection

/**
 * Почта между героями (4.6.3, сервер `mail/letter`): одно место для ящика, карточки игрока и гильдии. Окно письма открывается
 * черновиком [LetterDraft] в [FeedbackRepository] - его рисует хозяин окна поверх всего; адресата вводом не задают. Пределы,
 * немоту и право писать решает сервер: квота окна - его же [com.sperance.exileforge.rules.content.MailRules.gate].
 * Письмо - команда транспорта с `Idempotency-Key`, как прочая почта (героя письмо не меняет - мимо `HeroActions`), но в очереди
 * команд не ждёт: окно ждёт ответа и показывает отказ сразу.
 */
class LetterActions(
    private val repository: FeedbackRepository,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
) {
    private val api: GameApi get() = connection.api

    /** Герой в игре - автор письма; без героя писать некому. */
    private val author: String? get() = heroes.heroId.takeIf { it.isNotBlank() }

    /** Открывает окно письма [draft] и читает квоту героя к его адресату. */
    fun compose(draft: LetterDraft) {
        repository.update { it.copy(letter = draft, quota = null) }
        loadQuota()
    }

    fun close() = repository.update { it.copy(letter = null) }

    /** «Ответить» на письмо героя [mail]: тема с пометкой ответа (в пределах [subjectLimit]) и цитата; письму без автора - ничего. */
    fun reply(mail: Mail, subjectLimit: Int) {
        val mark = ui("mail.reply_subject", "").trim()
        val subject = mail.subject.takeIf { it.startsWith(mark) } ?: ui("mail.reply_subject", mail.subject)
        LetterDraft.reply(mail, subject.take(subjectLimit))?.let(::compose)
    }

    /** Квота окна письма: можно ли писать адресату, сколько осталось, немота. */
    fun loadQuota() {
        val draft = repository.state.value.letter ?: return
        val hero = author ?: return
        commands.read(Reads.MAIL_QUOTA, restart = true, silent = true) {
            val quota = api.mail.quota(hero, draft.to)
            repository.update { if (it.letter == draft) it.copy(quota = quota) else it }
        }
    }

    /** Отправка окна письма: оно закрывается, квота после письма остаётся для следующего. */
    fun send(subject: String, body: String) = commands.task(writing = true) {
        val draft = repository.state.value.letter ?: return@task
        val hero = checkNotNull(author) { ui("auction.choose_character") }
        val quota = api.mail.letter(hero, draft.request(subject, body))
        repository.update { it.copy(letter = null, quota = quota) }
        notices.toast(ui(if (draft.kind == MailKind.GUILD) "mail.letter.broadcast_sent" else "mail.letter.sent", draft.toName))
    }

    /** Список игнора героя в игре. */
    fun loadIgnored() {
        val hero = author ?: return
        commands.read(Reads.MAIL_IGNORES, silent = true) {
            val ignored = api.mail.ignores(hero)
            repository.update { it.copy(ignored = ignored) }
        }
    }

    /** «Не принимать письма от героя» [heroId] ([on]) или снова принимать; [then] - после ответа сервера (карточка игрока). */
    fun ignore(heroId: String, name: String, on: Boolean, then: () -> Unit = {}) = commands.task(writing = true) {
        val hero = author ?: return@task
        val ignored = if (on) api.mail.ignore(hero, heroId) else api.mail.unignore(hero, heroId)
        repository.update { it.copy(ignored = ignored) }
        then()
        notices.toast(ui(if (on) "mail.ignore.added" else "mail.ignore.removed", name))
    }

    /** Жалоба на письмо [id]: уходит модерации с текстом письма; письмо помечается жалобой. */
    fun report(id: String) = commands.task(writing = true) {
        val hero = author ?: return@task
        val letter = api.mail.report(hero, id)
        repository.update { f -> f.withMail(f.mail.map { if (it.id == id) letter else it }) }
        notices.toast(ui("mail.report.sent"))
    }
}

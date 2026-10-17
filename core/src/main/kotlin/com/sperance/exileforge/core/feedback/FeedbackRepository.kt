package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.IgnoredHero
import com.sperance.exileforge.core.model.feedback.KnownReport
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailQuota
import com.sperance.exileforge.core.model.feedback.OwnReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Голоса игроков и почта аккаунта: предложения, свои отчёты, чтение администратора, ящик. [unread] -
 * непрочитанные письма: приходит со снимком героя, а прочитанный ящик пересчитывает его сам ([withMail]).
 */
data class Feedback(
    /** Известные проблемы: ошибки с открытым Issue (без предложений). */
    val known: List<KnownReport> = emptyList(),
    val mine: List<OwnReport> = emptyList(),
    val reports: List<AdminReport> = emptyList(),
    val mail: List<Mail> = emptyList(),
    val unread: Int = 0,
    /** Кому администратор пишет письмо: логин из карточки игрока, его берёт форма письма; пусто - не задан. */
    val mailTo: String = "",
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

/** Единственный источник правды об отзывах и почте. */
class FeedbackRepository {
    private val mutable = MutableStateFlow(Feedback())
    val state: StateFlow<Feedback> = mutable

    fun update(transform: (Feedback) -> Feedback) = mutable.update(transform)

    fun clear() = update { Feedback() }
}

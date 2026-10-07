package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.model.feedback.Suggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Голоса игроков и почта аккаунта (3.73.0): предложения, свои отчёты, чтение администратора, ящик. [unread] (3.94.1) -
 * непрочитанные письма: приходит со снимком героя, а прочитанный ящик пересчитывает его сам ([withMail]).
 */
data class Feedback(
    val suggestions: List<Suggestion> = emptyList(),
    val mine: List<OwnReport> = emptyList(),
    val reports: List<AdminReport> = emptyList(),
    val mail: List<Mail> = emptyList(),
    val unread: Int = 0,
) {
    /** Ящик, каким его теперь знает клиент, и счёт непрочитанного по нему. */
    fun withMail(mail: List<Mail>): Feedback = copy(mail = mail, unread = mail.count { !it.read })
}

/** Единственный источник правды об отзывах и почте (3.80.9). */
class FeedbackRepository {
    private val mutable = MutableStateFlow(Feedback())
    val state: StateFlow<Feedback> = mutable

    fun update(transform: (Feedback) -> Feedback) = mutable.update(transform)

    fun clear() = update { Feedback() }
}

package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.model.feedback.LetterRequest
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailKind

/**
 * Письмо, что пишет герой (4.6.3): кому - герой [to] (имя, класс, уровень - для плашки «Кому», не редактируется) или вся
 * гильдия ([MailKind.GUILD]); ответ - [replyTo] с цитатой [quote] автора [quoteBy]. Адресата вводом не задают: только карточка
 * игрока, «Ответить» и рассылка гильдии. [early] - ответ на письмо, на которое отвечают и до уровня права писать
 * ([MailKind.answerable]).
 */
data class LetterDraft(
    val kind: MailKind = MailKind.PLAYER,
    val to: String = "",
    val toName: String = "",
    val toClass: String = "",
    val toLevel: Int = 0,
    val replyTo: String = "",
    val subject: String = "",
    val quote: String = "",
    val quoteBy: String = "",
    val early: Boolean = false,
) {
    /** Запрос письма с темой [subject] и текстом [body]. */
    fun request(subject: String, body: String): LetterRequest = LetterRequest(kind, to, replyTo, subject.trim(), body.trim())

    companion object {
        /** Письмо герою [heroId] из карточки игрока. */
        fun to(heroId: String, name: String, heroClass: String, level: Int) = LetterDraft(MailKind.PLAYER, heroId, name, heroClass, level)

        /** Рассылка гильдии. */
        fun guild() = LetterDraft(MailKind.GUILD)

        /**
         * Ответ на письмо героя [mail]: лично его автору (и на рассылку гильдии тоже), тема [subject] - уже с пометкой ответа,
         * цитата - начало письма.
         */
        fun reply(mail: Mail, subject: String): LetterDraft? {
            val from = mail.from?.takeIf { mail.answerable } ?: return null
            return LetterDraft(MailKind.PLAYER, from.heroId, from.name, from.heroClass, from.level, mail.id, subject, mail.body, from.name, mail.kind.answerable)
        }
    }
}

package com.sperance.exileforge.core.feedback

import com.sperance.exileforge.core.app.Landing
import com.sperance.exileforge.core.app.Screens
import com.sperance.exileforge.core.model.feedback.MailAddressee

/**
 * Письмо от Администрации одному аккаунту - из карточки игрока и из досье модерации: форма письма ([Landing.MAIL]) открывается с адресатом
 * плашкой (по id аккаунта, [MailAddressee]); модератору - без вложения.
 */
class AdminLetters(private val feedback: FeedbackRepository, private val screens: Screens) {
    /** Письмо аккаунту [userId] с логином [login] (без логина адресат зовётся героем [heroName]). */
    fun compose(userId: String, login: String, heroName: String) {
        if (userId.isBlank()) return
        feedback.update { it.copy(mailTo = MailAddressee.of(userId, login, heroName)) }
        screens.open(Landing.MAIL)
    }
}

package com.sperance.exileforge.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.feedback.FeedbackModel
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.feedback.LetterActions
import com.sperance.exileforge.core.feedback.SeenReplies
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository

/** Голоса, отчёты и почта: модель ядра [model] в области экрана. */
class FeedbackViewModel(
    repository: FeedbackRepository,
    connection: ServerConnection,
    commands: CommandRunner,
    sessions: SessionRepository,
    notices: Notices,
    heroes: HeroRepository,
    letters: LetterActions,
    replies: SeenReplies,
) : ViewModel() {
    val model = FeedbackModel(repository, connection, commands, sessions, notices, heroes, letters, replies, viewModelScope)
}

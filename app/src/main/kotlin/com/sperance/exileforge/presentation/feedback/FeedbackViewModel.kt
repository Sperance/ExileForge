package com.sperance.exileforge.presentation.feedback

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.feedback.Feedback
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.MailRequest
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.model.feedback.Vote
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Голоса игроков и почта аккаунта (3.73.0): предложения с голосами, свои отчёты, чтение администратора,
 * ящик с вложениями, письма администратора. Первая модель экрана на репозиториях :core (3.80.9).
 */
class FeedbackViewModel(
    private val repository: FeedbackRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val sessions: SessionRepository,
    private val notices: Notices,
    private val events: GameEvents,
) : ViewModel() {
    val feedback: StateFlow<Feedback> = repository.state
    val activity = commands.state

    private val api: GameApi get() = connection.api

    private fun feedback(transform: (Feedback) -> Feedback) = repository.update(transform)

    fun loadSuggestions() = commands.read(Reads.FEEDBACK) {
        val suggestions = api.feedback.suggestions()
        val mine = api.feedback.mine()
        feedback { it.copy(suggestions = suggestions, mine = mine) }
    }

    /** Тот же голос снова снимает его; голос за другую сторону переключает. */
    fun vote(id: String, vote: Vote) = commands.task(writing = true) {
        val current = feedback.value.suggestions.firstOrNull { it.id == id } ?: return@task
        val answered = api.feedback.vote(id, if (current.vote == vote) Vote.NONE else vote)
        feedback { f -> f.copy(suggestions = f.suggestions.map { if (it.id == id) answered else it }) }
    }

    fun loadReports(kind: FeedbackKind?, status: ReportStatus?) = commands.read(Reads.FEEDBACK) {
        check(sessions.state.value.isModerator) { ui("moderation.staff_only") }
        val reports = api.feedback.all(kind, status)
        feedback { it.copy(reports = reports) }
    }

    fun setReportStatus(id: String, status: ReportStatus, reason: String) = answer(ui("feedback.status_saved")) { api.feedback.setStatus(id, status, reason) }

    /** Отчёт в Asana (3.75.0): сервер заводит задачу и отвечает отчётом как он теперь стоит. */
    fun reportToAsana(id: String) = answer(ui("feedback.asana_done")) {
        // 3.75.1: сервер старше 1.70.0 такого маршрута не знает и отвечал «обнови приложение» - обновлять надо сервер.
        val live = api.manifest(fresh = true)
        check(live.capabilities.has("POST", ASANA_ROUTE)) { ui("feedback.asana_stale_server", live.version.ifBlank { "?" }) }
        api.feedback.toAsana(id)
    }

    private fun answer(done: String, call: suspend () -> AdminReport) = commands.task(writing = true) {
        val answered = call()
        feedback { f -> f.copy(reports = f.reports.map { if (it.report.id == answered.report.id) answered else it }) }
        notices.toast(done)
    }

    /** Ящик, тихо: конверт в шапке считает непрочитанное. */
    fun loadMail() {
        if (!sessions.state.value.signedIn) return
        commands.read(Reads.MAIL, silent = true) {
            val mail = api.mail.inbox()
            feedback { it.copy(mail = mail) }
        }
    }

    fun readMail(id: String) = commands.read(Reads.MAIL) {
        val letter = api.mail.read(id)
        feedback { f -> f.copy(mail = f.mail.map { if (it.id == id) letter else it }) }
    }

    /** Вложение уходит герою в игре [heroId], и герой перечитывается с ним. */
    fun claimMail(id: String, heroId: String) = commands.task(writing = true, touches = setOf(Reads.HERO)) {
        check(heroId.isNotBlank()) { ui("auction.choose_character") }
        val letter = api.mail.claim(id, heroId)
        feedback { f -> f.copy(mail = f.mail.map { if (it.id == id) letter else it }) }
        events.heroChanged()
        notices.toast(ui("mail.claimed"))
    }

    fun deleteMail(id: String) = commands.task(writing = true) {
        api.mail.delete(id)
        feedback { f -> f.copy(mail = f.mail.filterNot { it.id == id }) }
    }

    fun sendMail(request: MailRequest) = commands.task(writing = true) {
        check(sessions.state.value.isAdmin) { ui("hero.grant_admin_only") }
        val sent = api.mail.send(request)
        notices.toast(ui("mail.sent", sent))
    }

    private companion object {
        const val ASANA_ROUTE = "/api/v1/admin/feedback/asana"
    }
}

package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.MailRequest
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.model.feedback.Vote
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.FeedbackState
import com.sperance.exileforge.presentation.state.Reads

/**
 * Players' voices and the account's mail (3.73.0): the suggestions with their likes and dislikes, one's own reports and how
 * they stand, the administrator's reading of them all, the inbox with its attachments, the administrator's letters.
 */
class FeedbackViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {
    private fun feedback(transform: (FeedbackState) -> FeedbackState) = update { it.copy(feedback = transform(it.feedback)) }

    fun loadSuggestions() {
        with(runtime) {
            read(Reads.FEEDBACK) {
                val suggestions = api.feedback.suggestions()
                val mine = api.feedback.mine()
                feedback { it.copy(suggestions = suggestions, mine = mine) }
            }
        }
    }

    /** The same vote again takes it back; a vote on another side switches it. */
    fun vote(id: String, vote: Vote) {
        with(runtime) {
            task(writing = true) {
                val current = state.value.feedback.suggestions.firstOrNull { it.id == id } ?: return@task
                val answered = api.feedback.vote(id, if (current.vote == vote) Vote.NONE else vote)
                feedback { f -> f.copy(suggestions = f.suggestions.map { if (it.id == id) answered else it }) }
            }
        }
    }

    fun loadReports(kind: FeedbackKind?, status: ReportStatus?) {
        with(runtime) {
            read(Reads.FEEDBACK) {
                check(state.value.isAdmin) { ui("hero.grant_admin_only") }
                val reports = api.feedback.all(kind, status)
                feedback { it.copy(reports = reports) }
            }
        }
    }

    fun setReportStatus(id: String, status: ReportStatus, reason: String) = answer(ui("feedback.status_saved")) { api.feedback.setStatus(id, status, reason) }

    /** The report to Asana (3.75.0): the server makes the task and answers with the report as it now stands. */
    fun reportToAsana(id: String) = answer(ui("feedback.asana_done")) {
        // 3.75.1: a server older than 1.70.0 has no such route and answered «update the app» — the server is the one to update.
        val live = api.manifest(fresh = true)
        check(live.capabilities.has("POST", ASANA_ROUTE)) { ui("feedback.asana_stale_server", live.version.ifBlank { "?" }) }
        api.feedback.toAsana(id)
    }

    private companion object {
        const val ASANA_ROUTE = "/api/v1/admin/feedback/asana"
    }

    private fun answer(done: String, call: suspend ForgeRuntime.() -> AdminReport) {
        with(runtime) {
            task(writing = true) {
                val answered = call()
                feedback { f -> f.copy(reports = f.reports.map { if (it.report.id == answered.report.id) answered else it }) }
                toast(done)
            }
        }
    }

    /** The inbox, quietly: the envelope in the banner counts what is unread. */
    fun loadMail() {
        with(runtime) {
            if (state.value.account.signedIn) {
                read(Reads.MAIL, silent = true) {
                    val mail = api.mail.inbox()
                    feedback { it.copy(mail = mail) }
                }
            }
        }
    }

    fun readMail(id: String) {
        with(runtime) {
            read(Reads.MAIL) {
                val letter = api.mail.read(id)
                feedback { f -> f.copy(mail = f.mail.map { if (it.id == id) letter else it }) }
            }
        }
    }

    /** The attachment goes to the hero in play, and the hero is read again with it. */
    fun claimMail(id: String) {
        with(runtime) {
            task(writing = true, touches = setOf(Reads.HERO)) {
                check(heroId.isNotBlank()) { ui("auction.choose_character") }
                val letter = api.mail.claim(id, heroId)
                feedback { f -> f.copy(mail = f.mail.map { if (it.id == id) letter else it }) }
                heroViewModel.readHero()
                toast(ui("mail.claimed"))
            }
        }
    }

    fun deleteMail(id: String) {
        with(runtime) {
            task(writing = true) {
                api.mail.delete(id)
                feedback { f -> f.copy(mail = f.mail.filterNot { it.id == id }) }
            }
        }
    }

    fun sendMail(request: MailRequest) {
        with(runtime) {
            task(writing = true) {
                check(state.value.isAdmin) { ui("hero.grant_admin_only") }
                val sent = api.mail.send(request)
                toast(ui("mail.sent", sent))
            }
        }
    }
}

package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.IgnoredHero
import com.sperance.exileforge.core.model.feedback.LetterRequest
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailQuota
import com.sperance.exileforge.core.model.feedback.MailRequest
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.model.feedback.Suggestion
import com.sperance.exileforge.core.model.feedback.Vote
import kotlinx.serialization.json.encodeToJsonElement

private const val REPORTS = "api/v1/bugreport"
private const val MAIL = "api/v1/mail"
private const val ADMIN = "api/v1/admin"

/** Players' suggestions and one's own reports (3.73.0, server 1.69.0); the administrator's view of them all. */
class FeedbackClient internal constructor(private val http: Transport) {
    suspend fun suggestions(): List<Suggestion> = http.get("$REPORTS/suggestions")
    suspend fun mine(): List<OwnReport> = http.get("$REPORTS/mine")
    suspend fun vote(id: String, vote: Vote): Suggestion = http.post("$REPORTS/vote", mapOf("id" to id, "vote" to vote.name))

    /** Удаление отчёта (4.3.0, сервер API 63): автор - своего, модерация - любого; апелляцию - никто. */
    suspend fun delete(id: String) {
        http.request("POST", "$REPORTS/delete", mapOf("id" to id), authenticated = true)
    }

    suspend fun all(kind: FeedbackKind?, status: ReportStatus?): List<AdminReport> = http.get(
        "$ADMIN/feedback",
        buildMap {
            kind?.let { put("kind", it.name) }
            status?.let { put("status", it.name) }
        },
    )

    suspend fun setStatus(id: String, status: ReportStatus, reason: String): AdminReport = http.post("$ADMIN/feedback/status", mapOf("id" to id, "status" to status.name, "reason" to reason))

    /** To Asana (3.75.0, server 1.70.0): a task of it, its link kept on the report, the status «in progress». */
    suspend fun toAsana(id: String): AdminReport = http.post("$ADMIN/feedback/asana", mapOf("id" to id))
}

/**
 * The account's mail (3.73.0, server 1.69.0): the inbox, reading, taking an attachment with a hero, deleting; the administrator's
 * sending. С 4.6.3 ящик - глазами героя [heroId] (письма героев видны лишь адресату), и почта между героями: письмо, квота,
 * игнор, жалоба.
 */
class MailClient internal constructor(private val http: Transport) {
    suspend fun inbox(heroId: String): List<Mail> = http.get("$MAIL/inbox", heroOf(heroId))
    suspend fun read(id: String): Mail = http.post("$MAIL/read", mapOf("id" to id))

    /** Все письма прочитаны (3.94.0, server 1.81.14); сколько стало прочитанными. Письма героев - только героя [heroId]. */
    suspend fun readAll(heroId: String): Long = http.post("$MAIL/read/all", heroOf(heroId))

    /** Прочитанные удалены, кроме писем с незабранным вложением (3.94.0); сколько удалено. */
    suspend fun deleteRead(heroId: String): Long = http.post("$MAIL/delete/read", heroOf(heroId))

    /** Письмо героя [heroId] (4.6.3): личное, ответ или рассылка гильдии; ответ - почта героя после письма. */
    suspend fun letter(heroId: String, request: LetterRequest): MailQuota = http.post("$MAIL/letter", heroQuery(heroId), WireJson.encodeToJsonElement(request))

    /** Почта героя [heroId] сейчас (4.6.3): можно ли писать адресату [to] (пусто - без адресата) и сколько осталось. */
    suspend fun quota(heroId: String, to: String): MailQuota = http.get("$MAIL/quota", heroQuery(heroId, "to" to to.takeIf { it.isNotBlank() }))

    /** Список игнора героя [heroId] (4.6.3), новые сверху. */
    suspend fun ignores(heroId: String): List<IgnoredHero> = http.get("$MAIL/ignores", heroQuery(heroId))

    /** Не принимать письма героя [target]; ответ - список игнора. */
    suspend fun ignore(heroId: String, target: String): List<IgnoredHero> = http.post("$MAIL/ignore", heroQuery(heroId, "target" to target))

    /** Снова принимать письма героя [target]; ответ - список игнора. */
    suspend fun unignore(heroId: String, target: String): List<IgnoredHero> = http.post("$MAIL/unignore", heroQuery(heroId, "target" to target))

    /** Жалоба на письмо [id] (4.6.3): уходит модерации с текстом письма; ответ - письмо с отметкой жалобы. */
    suspend fun report(heroId: String, id: String): Mail = http.post("$MAIL/report", heroQuery(heroId, "id" to id))
    suspend fun claim(id: String, heroId: String): Mail = http.post("$MAIL/claim", heroQuery(heroId, "id" to id))
    suspend fun delete(id: String) {
        http.request("POST", "$MAIL/delete", mapOf("id" to id), authenticated = true)
    }

    /** How many letters went out. */
    suspend fun send(request: MailRequest): Int = http.post("$ADMIN/mail", body = WireJson.encodeToJsonElement(request))

    /** Герой ящика: без героя (меню героев) - письма аккаунта без писем героев. */
    private fun heroOf(heroId: String): Map<String, String> = if (heroId.isBlank()) emptyMap() else heroQuery(heroId)
}

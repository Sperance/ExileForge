package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.feedback.FeedbackMove
import com.sperance.exileforge.core.model.feedback.FeedbackQueue
import com.sperance.exileforge.core.model.feedback.FeedbackQueuePage
import com.sperance.exileforge.core.model.feedback.FeedbackSort
import com.sperance.exileforge.core.model.feedback.IgnoredHero
import com.sperance.exileforge.core.model.feedback.IssueMove
import com.sperance.exileforge.core.model.feedback.LetterRequest
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.MailQuota
import com.sperance.exileforge.core.model.feedback.MailRequest
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.model.feedback.QueueSight
import com.sperance.exileforge.core.model.feedback.ReportAnswer
import com.sperance.exileforge.core.model.feedback.ReportStatus
import kotlinx.serialization.json.encodeToJsonElement

private const val REPORTS = "api/v1/bugreport"
private const val MAIL = "api/v1/mail"
private const val ADMIN = "api/v1/admin"

/** Свои отчёты игрока и очередь модерации страницами. */
class FeedbackClient internal constructor(private val http: Transport) {
    suspend fun mine(): List<OwnReport> = http.get("$REPORTS/mine")

    /** Ответ автора в Issue своего отчёта (сервер API 86): пока Issue открыт; уходит комментарием, сразу в переписке. */
    suspend fun answer(id: String, text: String): OwnReport = http.post("$REPORTS/answer", mapOf("id" to id), WireJson.encodeToJsonElement(ReportAnswer(text)))

    /** Автор закрывает свой отчёт - новый или в обсуждении: «Закрыт автором», без возврата; ответ - отчёт как он теперь стоит. */
    suspend fun close(id: String): OwnReport = http.post("$REPORTS/close", mapOf("id" to id))

    /**
     * Страница [page] очереди модерации [queue] под видом и поиском [sight] (текст, переписка, номер Issue, логин автора) в порядке [sort] -
     * отбор и порядок сервера, размер страницы - его `rules.pageSize`; счёт очередей - только у первой страницы.
     */
    suspend fun queue(sight: QueueSight, queue: FeedbackQueue, sort: FeedbackSort, page: Int): FeedbackQueuePage = http.get("$ADMIN/feedback", sight.params + mapOf("queue" to queue.name, "sort" to sort.name, "page" to page.coerceAtLeast(0).toString()))

    /**
     * Действия модерации с отчётом [id]: ответ - отчёт, как он теперь стоит, с его очередью и свежий счёт очередей под видом и поиском
     * [sight] модерации ([FeedbackMove]).
     */
    suspend fun setStatus(id: String, status: ReportStatus, reason: String, sight: QueueSight): FeedbackMove = http.post("$ADMIN/feedback/status", sight.params + mapOf("id" to id, "status" to status.name, "reason" to reason))

    /** Комментарий модерации автору: отчёт - в «Обсуждении», автору письмо. */
    suspend fun comment(id: String, text: String, sight: QueueSight): FeedbackMove = http.post("$ADMIN/feedback/comment", sight.params + ("id" to id), WireJson.encodeToJsonElement(ReportAnswer(text)))

    /** Удаление отчёта модерацией: ошибку и жалобу - да, апелляцию - никто; свой отчёт автор не удаляет, а закрывает ([close]). */
    suspend fun delete(id: String, sight: QueueSight): FeedbackMove = http.post("$ADMIN/feedback/delete", sight.params + ("id" to id))

    /** Issue в GitHub: из неотправленного отчёта - отправленный с Issue; неудачная выгрузка - ещё раз. */
    suspend fun toIssue(id: String, sight: QueueSight): FeedbackMove = http.post("$ADMIN/feedback/issue", sight.params + ("id" to id))

    /** Дополнение модерации к отчёту: раздел тела Issue, автору не видно, письма нет; пустой [text] стирает его. */
    suspend fun note(id: String, text: String, sight: QueueSight): FeedbackMove = http.post("$ADMIN/feedback/note", sight.params + ("id" to id), WireJson.encodeToJsonElement(ReportAnswer(text)))

    /** Issue из игры: ответ модерации, закрытие с причиной; [duplicate] - номер оригинала у дубля. */
    suspend fun steer(id: String, move: IssueMove, text: String, duplicate: Int, sight: QueueSight): FeedbackMove = http.post(
        "$ADMIN/feedback/issue/move",
        sight.params + mapOf("id" to id, "move" to move.name, "duplicate" to duplicate.toString()),
        WireJson.encodeToJsonElement(ReportAnswer(text)),
    )
}

/**
 * The account's mail: the inbox, reading, taking an attachment with a hero, deleting; the administrator's
 * sending. Ящик - глазами героя [heroId] (письма героев видны лишь адресату), и почта между героями: письмо, квота,
 * игнор, жалоба.
 */
class MailClient internal constructor(private val http: Transport) {
    suspend fun inbox(heroId: String): List<Mail> = http.get("$MAIL/inbox", heroOf(heroId))
    suspend fun read(id: String): Mail = http.post("$MAIL/read", mapOf("id" to id))

    /** Все письма прочитаны; сколько стало прочитанными. Письма героев - только героя [heroId]. */
    suspend fun readAll(heroId: String): Long = http.post("$MAIL/read/all", heroOf(heroId))

    /** Прочитанные удалены, кроме писем с незабранным вложением; сколько удалено. */
    suspend fun deleteRead(heroId: String): Long = http.post("$MAIL/delete/read", heroOf(heroId))

    /** Письмо героя [heroId]: личное, ответ или рассылка гильдии; ответ - почта героя после письма. */
    suspend fun letter(heroId: String, request: LetterRequest): MailQuota = http.post("$MAIL/letter", heroQuery(heroId), WireJson.encodeToJsonElement(request))

    /** Почта героя [heroId] сейчас: можно ли писать, немота, рассылка гильдии. */
    suspend fun quota(heroId: String): MailQuota = http.get("$MAIL/quota", heroQuery(heroId))

    /** Список игнора героя [heroId], новые сверху. */
    suspend fun ignores(heroId: String): List<IgnoredHero> = http.get("$MAIL/ignores", heroQuery(heroId))

    /** Не принимать письма героя [target]; ответ - список игнора. */
    suspend fun ignore(heroId: String, target: String): List<IgnoredHero> = http.post("$MAIL/ignore", heroQuery(heroId, "target" to target))

    /** Снова принимать письма героя [target]; ответ - список игнора. */
    suspend fun unignore(heroId: String, target: String): List<IgnoredHero> = http.post("$MAIL/unignore", heroQuery(heroId, "target" to target))

    /** Жалоба на письмо [id]: уходит модерации с текстом письма; ответ - письмо с отметкой жалобы. */
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

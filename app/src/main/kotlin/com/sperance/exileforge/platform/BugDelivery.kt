package com.sperance.exileforge.platform

import android.content.Context
import com.sperance.exileforge.core.feedback.BugCourier
import com.sperance.exileforge.core.feedback.BugNews
import com.sperance.exileforge.core.feedback.BugOutbox
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.ServerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Доставка отчётов об ошибке - «очередь на диске» [BugOutbox]: отчёт со сжатым снимком сразу ложится файлом и уходит
 * повторами под своим ключом, пока сервер не ответит. Не зависит от занятости [com.sperance.exileforge.core.session.CommandRunner]
 * и переживает краш и перезапуск: оставшееся в очереди уходит, как только есть связь. Везёт один долгоживущий курьер
 * ([BugCourier]): новый отчёт и возврат связи его будят, запрос в полёте не рвут. Вести - тостами: ушедшие за проход -
 * один тост («Отчёт отправлен» или «Отправлено отчётов: N»), отклонённый окончательно - с причиной, нет связи - один тост «уйдёт,
 * когда будет связь». Один на приложение.
 *
 * Курьер выходит в путь, как только встанет связь с сервером: оставшееся с прошлого запуска уходит сразу.
 */
class BugDelivery(context: Context, private val connection: ServerConnection, private val notices: Notices, private val scope: CoroutineScope) {
    private val courier = BugCourier(BugOutbox(File(context.filesDir, OUTBOX_DIR)), { connection.api.sendBug(it) }, ::tell)

    init {
        scope.launch {
            connection.current.filterNotNull().first()
            courier.run()
        }
    }

    /**
     * Отчёт, что собирает [build] (сжатие снимка, подпись - [com.sperance.exileforge.core.network.GameApi.bugReport]), - в очередь
     * и в путь; [onQueued] - отчёт на диске. Идёт в области приложения: закрытый экран сборку не обрывает.
     */
    fun file(build: suspend () -> BugReportRequest, onQueued: suspend () -> Unit = {}) {
        scope.launch {
            courier.put(build())
            onQueued()
        }
    }

    /** Связь вернулась - ждущие отчёты уходят сразу, без досиживания отступа. */
    fun kick() = courier.wake()

    private fun tell(news: BugNews) = when (news) {
        is BugNews.Sent -> notices.toast(if (news.count == 1) ui("bug.sent") else ui("bug.sent_many", news.count))
        is BugNews.Refused -> notices.toast(ui("bug.refused", news.reason), NoticeKind.FAILED)
        BugNews.Queued -> notices.toast(ui("bug.queued"))
    }

    private companion object {
        /** Папка очереди в личных файлах приложения. */
        const val OUTBOX_DIR = "bug_outbox"
    }
}

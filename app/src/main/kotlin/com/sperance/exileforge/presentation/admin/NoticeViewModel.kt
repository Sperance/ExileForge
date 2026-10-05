package com.sperance.exileforge.presentation.admin

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.SanctionView
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Экран санкции (3.88.5, вариант B): забаненный или удалённый видит, кто, когда, за что и до когда закрыл ему доступ, и может
 * один раз обжаловать. Санкция читается по id из отказа без входа - сессии у него уже нет. Апелляция с карточки удалённого
 * или забаненного героя идёт сюда же.
 */
class NoticeViewModel(
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val sessions: SessionRepository,
    private val notices: Notices,
) : ViewModel() {
    private val mutable = MutableStateFlow<SanctionView?>(null)
    val sanction: StateFlow<SanctionView?> = mutable.asStateFlow()
    val activity = commands.state

    fun load(id: String) {
        if (id.isBlank()) return
        commands.read(Reads.NOTICE, restart = true) { mutable.value = connection.api.moderation.notice(id) }
    }

    /** Апелляция на санкцию [id]: одна на санкцию, ответ - санкция с отметкой. */
    fun appeal(id: String, text: String) {
        val started = commands.task(writing = true) {
            require(text.isNotBlank()) { ui("notice.appeal_empty") }
            mutable.value = connection.api.moderation.appeal(id, text)
            notices.toast(ui("notice.appeal_sent"))
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Экран санкции закрыт: игрок идёт ко входу - другим аккаунтом или гостем. */
    fun dismiss() {
        mutable.value = null
        sessions.update { it.copy(notice = null) }
    }
}

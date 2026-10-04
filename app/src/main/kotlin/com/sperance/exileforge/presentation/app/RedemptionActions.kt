package com.sperance.exileforge.presentation.app

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.Actions
import com.sperance.exileforge.presentation.Repositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update

/**
 * Promo codes, which only an administrator ever sees as a list.
 *
 * A player types a code in on the Account tab and gets a reward; everything here is the other
 * side of that — what the codes are and what they pay out. The server decides whether a code is
 * acceptable, so nothing is validated twice: a blank or duplicate code, an empty reward and a
 * non-positive amount are all refusals, and a refusal is shown rather than pre-empted.
 */
class RedemptionActions(
    repositories: Repositories,
    actions: Actions,
    commands: CommandRunner,
    connection: ServerConnection,
    store: ServerStore,
    scope: CoroutineScope,
) : AppService(repositories, actions, commands, connection, store, scope) {

    fun load() {
        run {
            read(Reads.REDEMPTIONS) {
                check(sessions.state.value.isAdmin) { ui("redemption.admin_only") }
                val codes = api.promo.codes()
                admins.update { it.copy(redemptions = codes) }
            }
        }
    }

    fun create(code: RedemptionCode) {
        run {
            task(writing = true, touches = setOf(Reads.REDEMPTIONS)) {
                check(sessions.state.value.isAdmin) { ui("redemption.admin_only") }
                val created = api.promo.create(code)
                admins.update { it.copy(redemptions = it.redemptions + created) }
            }
        }
    }

    fun delete(id: String) {
        run {
            task(writing = true, touches = setOf(Reads.REDEMPTIONS)) {
                check(sessions.state.value.isAdmin) { ui("redemption.admin_only") }
                api.promo.delete(id)
                admins.update { it.copy(redemptions = it.redemptions.filterNot { code -> code.id == id }) }
            }
        }
    }
}

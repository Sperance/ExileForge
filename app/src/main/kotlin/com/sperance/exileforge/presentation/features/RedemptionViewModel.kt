package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.flow.update

/**
 * Promo codes, which only an administrator ever sees as a list.
 *
 * A player types a code in on the Account tab and gets a reward; everything here is the other
 * side of that — what the codes are and what they pay out. The server decides whether a code is
 * acceptable, so nothing is validated twice: a blank or duplicate code, an empty reward and a
 * non-positive amount are all refusals, and a refusal is shown rather than pre-empted.
 */
class RedemptionViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun load() { with(runtime) { task {
        check(state.value.isAdmin) { ui("redemption.admin_only") }
        val codes = api.redemptionCodes()
        mutable.update { it.copy(redemptions = codes) }
    } } }

    fun create(code: RedemptionCode) { with(runtime) { task(writing = true) {
        check(state.value.isAdmin) { ui("redemption.admin_only") }
        val created = api.createRedemption(code)
        mutable.update { it.copy(redemptions = it.redemptions + created, message = ui("redemption.created", created.code)) }
    } } }

    fun delete(id: String) { with(runtime) { task(writing = true) {
        check(state.value.isAdmin) { ui("redemption.admin_only") }
        api.deleteRedemption(id)
        mutable.update { it.copy(redemptions = it.redemptions.filterNot { code -> code.id == id }, message = ui("redemption.deleted")) }
    } } }
}

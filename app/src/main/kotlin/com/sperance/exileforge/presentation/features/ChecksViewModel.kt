package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.modifier.PoolKind
import com.sperance.exileforge.core.verification.CrudScenario
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.flow.update

class ChecksViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun runChecks() { with(runtime) { task(writing = true) {
        check(state.value.isAdmin) { ui("checks.admin_only") }
        require(state.value.admin.catalog != Catalog.CHARACTERS) { ui("checks.items_only") }
        mutable.update { it.copy(admin = it.admin.copy(checks = emptyList())) }
        ensureWorld()
        // Equipment names the modifier pools it rolls from, so the scenario needs one real modifier pool tag.
        val pool = if (state.value.admin.catalog == Catalog.EQUIPMENT) state.value.world.pools.firstOrNull { it.kind == PoolKind.MODIFIER }?.code.orEmpty() else ""
        CrudScenario(api.catalog, pool).run(state.value.admin.catalog) { result -> mutable.update { it.copy(admin = it.admin.copy(checks = it.admin.checks + result)) } }
    } } }

    fun clearLogs() { with(runtime) { journal.clear() } }
}

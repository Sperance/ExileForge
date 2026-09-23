package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.verification.CrudScenario
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.flow.update

class ChecksViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun runChecks() { with(runtime) { task(writing = true) {
        check(state.value.isAdmin) { ui("checks.admin_only") }
        require(state.value.admin.catalog != Catalog.CHARACTERS) { ui("checks.items_only") }
        mutable.update { it.copy(admin = it.admin.copy(checks = emptyList())) }
        ensureDefinitions()
        // Equipment keeps a pool of modifier references, so the scenario needs one real definition id.
        val modifierId = if (state.value.admin.catalog == Catalog.EQUIPMENT) state.value.world.definitions.firstOrNull()?.id.orEmpty() else ""
        CrudScenario(api.catalog, modifierId).run(state.value.admin.catalog) { result -> mutable.update { it.copy(admin = it.admin.copy(checks = it.admin.checks + result)) } }
    } } }

    fun clearLogs() { with(runtime) { journal.clear() } }
}

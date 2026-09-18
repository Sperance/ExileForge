package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.verification.CrudScenario
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.flow.update

class ChecksViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun runChecks() { with(runtime) { task(writing = true) {
        check(state.value.isAdmin) { tr("Проверки записи доступны администратору", "Write checks are available to administrators") }
        require(state.value.catalog != Catalog.CHARACTERS) { tr("CRUD-сценарий предназначен для предметов", "The CRUD scenario is meant for items") }
        mutable.update { it.copy(checks = emptyList()) }
        ensureDefinitions()
        // Equipment keeps a pool of modifier references, so the scenario needs one real definition id.
        val modifierId = if (state.value.catalog == Catalog.EQUIPMENT) state.value.definitions.firstOrNull()?.id.orEmpty() else ""
        CrudScenario(api, modifierId).run(state.value.catalog) { result -> mutable.update { it.copy(checks = it.checks + result) } }
    } } }

    fun clearLogs() { with(runtime) { journal.clear() } }
}

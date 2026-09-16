package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.contract.starterModifier
import com.sperance.exileforge.core.generation.modifierFromDefinition
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.verification.CrudScenario
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.core.i18n.tr

class ChecksViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state
    fun runChecks() { with(runtime) {
task {
        check(state.value.isAdmin) { tr("Проверки записи доступны администратору", "Write checks are available to administrators") }
        require(state.value.catalog != Catalog.CHARACTERS) { tr("CRUD-сценарий предназначен для предметов", "The CRUD scenario is meant for items") }
        mutable.update { it.copy(checks = emptyList()) }
        val modifier = if(state.value.catalog == Catalog.EQUIPMENT) {
            val definitions = api.definitions("", 0).getValue("items").jsonArray
            modifierFromDefinition(definitions.firstOrNull()?.jsonObject ?: error(tr("Каталог модификаторов пуст. Запустите Seeder сервера", "The modifier catalogue is empty. Run the server seeder")))
        } else starterModifier()
        CrudScenario(api, modifier).run(state.value.catalog) { result -> mutable.update { it.copy(checks = it.checks + result) } }
    }
    } }
    fun clearLogs() { with(runtime) {
journal.clear()
    } }
}

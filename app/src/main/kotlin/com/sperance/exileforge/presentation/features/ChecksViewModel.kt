package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.contract.starterModifier
import com.sperance.exileforge.core.generation.modifierFromDefinition
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.verification.CrudScenario
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

import com.sperance.exileforge.presentation.ForgeRuntime

class ChecksViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state
    fun runChecks() { with(runtime) {
task {
        check(state.value.isAdmin) { "Проверки записи доступны администратору" }
        require(state.value.catalog != Catalog.CHARACTERS) { "CRUD-сценарий предназначен для предметов" }
        mutable.update { it.copy(checks = emptyList()) }
        val modifier = if(state.value.catalog == Catalog.EQUIPMENT) {
            val definitions = api.definitions("", 0).getValue("items").jsonArray
            modifierFromDefinition(definitions.firstOrNull()?.jsonObject ?: error("Каталог модификаторов пуст. Запустите Seeder сервера"))
        } else starterModifier()
        CrudScenario(api, modifier).run(state.value.catalog) { result -> mutable.update { it.copy(checks = it.checks + result) } }
    }
    } }
    fun clearLogs() { with(runtime) {
journal.clear()
    } }
}

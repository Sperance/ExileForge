package com.sperance.exileforge.presentation.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.ForgeApplication
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.editor.conflict.ThreeWayMerge
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.contract.entityVersion
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.definitionKey
import com.sperance.exileforge.core.contract.diff
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.referenceKey
import com.sperance.exileforge.core.contract.starterModifier
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.generation.modifierFromDefinition
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.core.verification.CrudScenario
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.PendingInventoryAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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

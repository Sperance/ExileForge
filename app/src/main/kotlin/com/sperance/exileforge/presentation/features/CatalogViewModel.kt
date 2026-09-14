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

class CatalogViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state
    fun query(value: String) { with(runtime) {
 mutable.update { it.copy(query = value) } 
    } }
    fun catalog(value: Catalog) { with(runtime) {

        if (state.value.busy || state.value.editorOpen || !state.value.adminTools && value != Catalog.CHARACTERS) return
        mutable.update { it.copy(catalog = value, items = emptyList(), page = 0, total = 0, totalPages = 0, query = "") }
        task {
            val saved = store.filters(state.value.server, value.path)?.let { WireJson.decodeFromString(CatalogFilter.serializer(), it) } ?: CatalogFilter()
            mutable.update { it.copy(filter = saved, query = saved.query) }; loadPage(0)
        }
    
    } }
    fun filter(value: CatalogFilter) { with(runtime) {
 if(!state.value.busy) mutable.update { it.copy(filter = value, query = value.query) } 
    } }
    fun applyFilters() { with(runtime) {
task {
        store.saveFilters(state.value.server, state.value.catalog.path, WireJson.encodeToString(CatalogFilter.serializer(), state.value.filter.copy(query = state.value.query)))
        loadPage(0)
    }
    } }
    fun refresh(page: Int = state.value.page) { with(runtime) {
task { if(state.value.signedIn) loadPage(page) else mutable.update { it.copy(tab = 3, message = "Войдите в аккаунт") } }
    } }
    fun count() { with(runtime) {
task {
        val result = api.count(state.value.catalog)
        mutable.update { it.copy(message = "Количество: $result") }
    }
    } }
    fun open(id: String) { with(runtime) {
task {
        val doc = api.get(state.value.catalog, id) ?: error("Предмет не найден")
        val pinned = pinnedDefinitions(doc)
        setEditor(doc, doc)
        mutable.update { it.copy(definitions = (pinned + it.definitions).distinctBy(::definitionKey)) }
    }
    } }
}

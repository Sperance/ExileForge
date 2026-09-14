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

class EditorViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) { with(runtime) {

        if (state.value.busy || !state.value.canEdit) return
        setEditor(template(state.value.catalog, kind), null)
    
    } }
    fun closeEditor() { with(runtime) {
 if (!state.value.busy) mutable.update { it.copy(editorOpen = false, original = null, draft = JsonObject(emptyMap())) } 
    } }
    fun edit(document: JsonObject) { with(runtime) {
 if (!state.value.busy) mutable.update { it.copy(draft = document) } 
    } }
    fun publishDefinition(document: JsonObject, expectedRevision: Int) { with(runtime) {
task {
        validateForm("definition", document)
        val saved = api.publishDefinition(document, expectedRevision)
        mutable.update { it.copy(definitions = (listOf(saved) + it.definitions).distinctBy(::definitionKey), message = "Опубликовано ${saved.text("id")} v${saved.text("revision")}") }
    }
    } }
    fun definitionQuery(value: String) { with(runtime) {
 mutable.update { it.copy(definitionQuery = value) } 
    } }
    fun loadDefinitions(page: Int = 0) { with(runtime) {
task {
        val result = api.definitions(state.value.definitionQuery.trim(), page)
        val definitions = (pinnedDefinitions(state.value.draft) + result.getValue("items").jsonArray.map { it.jsonObject }).distinctBy(::definitionKey)
        mutable.update { it.copy(definitions = definitions, definitionPage = page, definitionTotal = result.getValue("total").jsonPrimitive.int) }
    }
    } }
    fun reviewConflict() { with(runtime) {
task {
        val base = requireNotNull(state.value.original)
        val remote = api.get(state.value.catalog, base.entityId) ?: error("Запись удалена")
        val review = ThreeWayMerge.review(base, state.value.draft, remote)
        mutable.update { it.copy(mergeReview = review, mergeRemote = remote) }
    }
    } }
    fun resolveConflict(choices: Map<String, Boolean>) { with(runtime) {
task {
        val review = requireNotNull(state.value.mergeReview)
        val remote = requireNotNull(state.value.mergeRemote)
        val merged = ThreeWayMerge.resolve(review, choices)
        setEditor(merged, remote)
        mutable.update { it.copy(message = "Черновик объединён. Проверьте его и нажмите «Сохранить».") }
    }
    } }
    fun reloadEditor() { with(runtime) {
task {
        val original = requireNotNull(state.value.original)
        val latest = api.get(state.value.catalog, original.entityId) ?: error("Запись удалена")
        setEditor(latest, latest)
    }
    } }
    fun save() { with(runtime) {
task(writing = true) {
        check(state.value.canEdit) { "Недостаточно прав" }
        check(!state.value.conflict) { "Сначала обновите запись после конфликта" }
        val document = state.value.draft
        val catalog = state.value.catalog
        validate(document, catalog)
        val original = state.value.original
        val saved = if (original == null) api.create(catalog, JsonObject(document.filterKeys { it in editableFields(catalog) || it == "type" })) else {
            val changes = diff(original, document)
            require(catalog != Catalog.CHARACTERS || "userId" !in changes) { "Владельца существующего персонажа менять нельзя" }
            require(changes.isNotEmpty()) { "Нет изменений для сохранения" }
            api.update(catalog, original.entityId, changes, original.entityVersion)
        }
        if(catalog == Catalog.EQUIPMENT) mutable.update { it.copy(inventoryBases = it.inventoryBases + (saved.entityId to saved)) }
        setEditor(saved, saved)
        mutable.update { it.copy(message = "Сохранено: ${saved.entityId}") }
        // List refresh failure must not imply that the successful mutation failed.
        try { loadPage(state.value.page) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = "Сохранено. Обновите список вручную.") } }
    }
    } }
    fun delete() { with(runtime) {
task(writing = true) {
        val original = state.value.original ?: error("Сначала сохраните предмет")
        check(state.value.canEdit && !state.value.conflict)
        api.delete(state.value.catalog, original.entityId, original.entityVersion)
        mutable.update { it.copy(editorOpen = false, original = null, tab = 0, items = it.items.filterNot { item -> item.entityId == original.entityId }, message = "Предмет удалён") }
        try { loadPage(0) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = "Удалено. Обновите список вручную.") } }
    }
    } }
    fun editInventoryBase(id: String) { with(runtime) {
task {
        check(!state.value.editorOpen || state.value.original?.let { diff(it, state.value.draft).isEmpty() } == true) { "Сохраните или закройте текущий черновик" }
        val doc = api.get(Catalog.EQUIPMENT, id) ?: error("База предмета не найдена")
        val definitions = pinnedDefinitions(doc)
        mutable.update { it.copy(catalog = Catalog.EQUIPMENT, items = emptyList(), total = 0, page = 0, totalPages = 0, definitions = definitions) }
        setEditor(doc, doc)
    }
    } }
}
